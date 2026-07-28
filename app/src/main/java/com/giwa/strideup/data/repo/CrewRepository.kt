package com.giwa.strideup.data.repo

import com.giwa.strideup.data.local.CrewDao
import com.giwa.strideup.data.local.CrewMembershipEntity
import com.giwa.strideup.data.local.NotificationType
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** 러닝 크루 (데모 데이터) */
data class Crew(
    val id: String,
    val monogram: String,
    val name: String,
    val kmAway: String,
    val memberCount: Int,
    val roster: List<String>,
)

data class PartyMember(
    val id: String,
    val name: String,
    val level: Int,
    val ready: Boolean,
    val isMe: Boolean,
)

enum class PartyPhase {
    /** 로비에 들어오지 않은 상태 */
    IDLE,

    /** 준비 대기 중 */
    LOBBY,

    /** 전원 준비 완료 → 카운트다운 */
    COUNTDOWN,

    /** 같이 측정 중 */
    RUNNING,

    /** 정산 결과 표시 */
    FINISHED,
}

data class PartyState(
    val phase: PartyPhase = PartyPhase.IDLE,
    val crewId: String? = null,
    val crewName: String = "",
    val members: List<PartyMember> = emptyList(),
    val countdown: Int = 0,
    val resultPoints: Double = 0.0,
    val resultSteps: Int = 0,
) {
    val readyCount: Int get() = members.count { it.ready }
    val partySize: Int get() = members.size
    val allReady: Boolean get() = members.isNotEmpty() && members.all { it.ready }
    val myReady: Boolean get() = members.firstOrNull { it.isMe }?.ready == true
    val isActive: Boolean get() = phase == PartyPhase.RUNNING
}

/**
 * 크루 가입 상태와 파티런 로비를 관리한다.
 *
 * 백엔드가 없으므로 크루원은 시뮬레이션한다. 내가 준비를 누르면 남은 크루원들이
 * 차례로 준비를 마치고, 전원 준비되면 카운트다운 후 다 같이 측정이 시작된다.
 * 인원수만큼 적립 부스트가 붙는다(RewardEconomy.partyMultiplier).
 */
class CrewRepository(
    private val crewDao: CrewDao,
    private val rewardRepository: RewardRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var simulationJob: Job? = null

    val crews: List<Crew> = listOf(
        Crew("trailblazer", "TB", "Trailblazer Crew", "0.8", 128, listOf("Maya C.", "Jun H.", "Elena R.", "Marco P.")),
        Crew("night_runners", "NR", "Night Runners", "1.3", 86, listOf("Sora K.", "Diego M.", "Lena B.")),
        Crew("summit", "SS", "Summit Seekers", "2.1", 142, listOf("Aiko T.", "Tomas L.", "Priya N.", "Owen D.", "Zara F.")),
        Crew("new_striders", "NS", "New Striders", "0.5", 42, listOf("Kai W.", "Nora S.")),
    )

    fun crewOf(id: String): Crew? = crews.firstOrNull { it.id == id }

    val joinedCrewIds: Flow<Set<String>> =
        crewDao.observeAll().map { list -> list.map { it.crewId }.toSet() }

    suspend fun join(crewId: String) {
        crewDao.insert(CrewMembershipEntity(crewId, System.currentTimeMillis()))
        crewOf(crewId)?.let { rewardRepository.notify(NotificationType.CREW_JOINED, it.name) }
    }

    suspend fun leave(crewId: String) {
        crewDao.leave(crewId)
    }

    // ── 파티런 로비 ──────────────────────────────────────────

    private val _party = MutableStateFlow(PartyState())
    val party: StateFlow<PartyState> = _party

    /** 로비 입장. 크루원 일부는 이미 준비를 마친 상태로 시작한다. */
    fun openLobby(crewId: String) {
        val crew = crewOf(crewId) ?: return
        if (_party.value.crewId == crewId &&
            _party.value.phase !in listOf(PartyPhase.IDLE, PartyPhase.FINISHED)
        ) {
            return // 이미 이 크루 로비에 있음
        }
        simulationJob?.cancel()
        val random = Random(System.nanoTime())
        val squad = crew.roster.take(3 + random.nextInt(2))
        val members = buildList {
            add(PartyMember("me", "", 0, ready = false, isMe = true))
            squad.forEachIndexed { index, name ->
                add(
                    PartyMember(
                        id = "m$index",
                        name = name,
                        level = 8 + random.nextInt(14),
                        // 한두 명은 이미 준비 완료
                        ready = index == 0 && random.nextBoolean(),
                        isMe = false,
                    )
                )
            }
        }
        _party.value = PartyState(
            phase = PartyPhase.LOBBY,
            crewId = crew.id,
            crewName = crew.name,
            members = members,
        )
    }

    fun leaveLobby() {
        simulationJob?.cancel()
        _party.value = PartyState()
    }

    /** 내 준비 상태 토글. 준비를 누르면 남은 크루원들이 차례로 따라온다. */
    fun setMyReady(ready: Boolean) {
        val state = _party.value
        if (state.phase != PartyPhase.LOBBY) return
        _party.value = state.copy(
            members = state.members.map { if (it.isMe) it.copy(ready = ready) else it },
        )
        if (ready) startSimulation() else simulationJob?.cancel()
        maybeStartCountdown()
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            val random = Random(System.nanoTime())
            while (true) {
                val current = _party.value
                if (current.phase != PartyPhase.LOBBY) return@launch
                val pending = current.members.filter { !it.ready && !it.isMe }
                if (pending.isEmpty()) break
                delay(700L + random.nextLong(1600))
                val snapshot = _party.value
                if (snapshot.phase != PartyPhase.LOBBY || !snapshot.myReady) return@launch
                val next = snapshot.members.firstOrNull { !it.ready && !it.isMe } ?: break
                _party.value = snapshot.copy(
                    members = snapshot.members.map {
                        if (it.id == next.id) it.copy(ready = true) else it
                    },
                )
            }
            maybeStartCountdown()
        }
    }

    private fun maybeStartCountdown() {
        val state = _party.value
        if (state.phase != PartyPhase.LOBBY || !state.allReady) return
        simulationJob?.cancel()
        simulationJob = scope.launch {
            for (n in 3 downTo 1) {
                val snapshot = _party.value
                if (snapshot.phase !in listOf(PartyPhase.LOBBY, PartyPhase.COUNTDOWN)) return@launch
                _party.value = snapshot.copy(phase = PartyPhase.COUNTDOWN, countdown = n)
                delay(1000)
            }
            val snapshot = _party.value
            if (snapshot.phase != PartyPhase.COUNTDOWN) return@launch
            _party.value = snapshot.copy(phase = PartyPhase.RUNNING, countdown = 0)
        }
    }

    /** 세션 정산 후 결과 화면으로 전환 */
    fun finishParty(points: Double, steps: Int) {
        val state = _party.value
        if (state.phase != PartyPhase.RUNNING) return
        simulationJob?.cancel()
        _party.value = state.copy(
            phase = PartyPhase.FINISHED,
            resultPoints = points,
            resultSteps = steps,
        )
    }

    fun dismissResult() {
        if (_party.value.phase == PartyPhase.FINISHED) _party.value = PartyState()
    }

    /** 현재 파티 인원 (파티런이 아니면 1) */
    fun currentPartySize(): Int =
        if (_party.value.isActive) _party.value.partySize.coerceAtLeast(1) else 1
}
