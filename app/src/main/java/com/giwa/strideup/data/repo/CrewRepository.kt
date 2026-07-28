package com.giwa.strideup.data.repo

import android.content.Context
import com.giwa.strideup.R
import com.giwa.strideup.data.local.CrewDao
import com.giwa.strideup.data.local.CrewEntity
import com.giwa.strideup.data.local.CrewInfoDao
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 러닝 크루 */
data class Crew(
    val id: String,
    val monogram: String,
    val name: String,
    val tagline: String,
    val area: String,
    val kmAway: Double,
    val memberCount: Int,
    val roster: List<String>,
    /** 내가 만든 모임 */
    val owned: Boolean,
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
 * 크루 목록·가입 상태와 파티런 로비를 관리한다.
 *
 * 크루는 Room에 저장되며 기본 4개는 첫 실행 시 시드된다. 사용자가 만든 모임도
 * 같은 표에 들어가 목록·게시판·파티런을 그대로 쓴다.
 *
 * 백엔드가 없으므로 크루원은 시뮬레이션한다. 내가 준비를 누르면 남은 크루원들이
 * 차례로 준비를 마치고, 전원 준비되면 카운트다운 후 다 같이 측정이 시작된다.
 * 인원수만큼 적립 부스트가 붙는다(RewardEconomy.partyMultiplier).
 */
class CrewRepository(
    private val crewDao: CrewDao,
    private val crewInfoDao: CrewInfoDao,
    private val rewardRepository: RewardRepository,
    private val appContext: Context,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var simulationJob: Job? = null

    val crews: StateFlow<List<Crew>> = crewInfoDao.observeAll()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val joinedCrewIds: Flow<Set<String>> =
        crewDao.observeAll().map { list -> list.map { it.crewId }.toSet() }

    fun crewOf(id: String): Crew? = crews.value.firstOrNull { it.id == id }

    /** 기본 크루 시드. 이미 있으면 아무것도 하지 않는다. */
    suspend fun ensureSeeded() {
        if (crewInfoDao.count() > 0) return
        val now = System.currentTimeMillis()
        crewInfoDao.upsertAll(
            listOf(
                CrewEntity(
                    id = "trailblazer",
                    name = "Trailblazer Crew",
                    monogram = "TB",
                    tagline = appContext.getString(R.string.seed_crew_trailblazer_tagline),
                    area = "Riverside",
                    kmAway = 0.8,
                    memberCount = 128,
                    roster = "Maya C.|Jun H.|Elena R.|Marco P.",
                    createdAt = now,
                    owned = false,
                ),
                CrewEntity(
                    id = "night_runners",
                    name = "Night Runners",
                    monogram = "NR",
                    tagline = appContext.getString(R.string.seed_crew_night_tagline),
                    area = "Downtown",
                    kmAway = 1.3,
                    memberCount = 86,
                    roster = "Sora K.|Diego M.|Lena B.",
                    createdAt = now,
                    owned = false,
                ),
                CrewEntity(
                    id = "summit",
                    name = "Summit Seekers",
                    monogram = "SS",
                    tagline = appContext.getString(R.string.seed_crew_summit_tagline),
                    area = "Highland",
                    kmAway = 2.1,
                    memberCount = 142,
                    roster = "Aiko T.|Tomas L.|Priya N.|Owen D.|Zara F.",
                    createdAt = now,
                    owned = false,
                ),
                CrewEntity(
                    id = "new_striders",
                    name = "New Striders",
                    monogram = "NS",
                    tagline = appContext.getString(R.string.seed_crew_striders_tagline),
                    area = "Cedar Park",
                    kmAway = 0.5,
                    memberCount = 42,
                    roster = "Kai W.|Nora S.",
                    createdAt = now,
                    owned = false,
                ),
            )
        )
    }

    suspend fun join(crewId: String) {
        crewDao.insert(CrewMembershipEntity(crewId, System.currentTimeMillis()))
        crewOf(crewId)?.let { rewardRepository.notify(NotificationType.CREW_JOINED, it.name) }
    }

    suspend fun leave(crewId: String) {
        crewDao.leave(crewId)
    }

    /**
     * 모임 만들기. 만든 사람은 곧바로 가입 상태가 된다.
     *
     * 새 모임에는 파티런을 바로 체험할 수 있도록 초대 멤버 몇 명을 넣어 둔다.
     */
    suspend fun create(name: String, tagline: String, area: String): String {
        val trimmed = name.trim().ifBlank { return "" }
        val id = "crew_${System.currentTimeMillis()}"
        val monogram = trimmed.split(" ", "-", "_")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { trimmed.take(2).uppercase() }
        crewInfoDao.upsert(
            CrewEntity(
                id = id,
                name = trimmed,
                monogram = monogram,
                tagline = tagline.trim(),
                area = area.trim(),
                kmAway = 0.0,
                memberCount = 4,
                roster = "Riley P.|Sena K.|Théo M.",
                createdAt = System.currentTimeMillis(),
                owned = true,
            )
        )
        join(id)
        return id
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

/** Room 엔티티 → 도메인 모델 */
fun CrewEntity.toDomain(): Crew = Crew(
    id = id,
    monogram = monogram,
    name = name,
    tagline = tagline,
    area = area,
    kmAway = kmAway,
    memberCount = memberCount,
    roster = roster.split("|").filter { it.isNotBlank() },
    owned = owned,
)
