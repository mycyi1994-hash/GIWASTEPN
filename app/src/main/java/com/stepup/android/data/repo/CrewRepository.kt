package com.stepup.android.data.repo

import android.content.Context
import com.stepup.android.R
import com.stepup.android.data.local.CrewDao
import com.stepup.android.data.local.CrewEntity
import com.stepup.android.data.local.CrewInfoDao
import com.stepup.android.data.local.CrewMembershipEntity
import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.local.WalkSessionDao
import com.stepup.android.domain.CrewRank
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    /** 러너 고유 ID — "SU-XXXXXX" */
    val uid: String,
    val level: Int,
    val ready: Boolean,
    val isMe: Boolean,
    /** 파티장(나)로부터의 거리 (m, 시뮬레이션) */
    val distanceM: Int,
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
    /** 번개러닝에서 연 로비면 그 글의 id. 크루 로비면 null. */
    val flashPostId: Long? = null,
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

    /**
     * 파티장이 지금 출발할 수 있는가.
     *
     * 전원이 준비하기를 기다리지 않는다. 한 사람이 신발 끈을 못 찾으면
     * 나머지 넷이 길에 서 있게 되고, 그 다섯 명은 다음부터 파티런을 안 쓴다.
     * 파티장 본인만 준비돼 있으면 출발할 수 있고, 준비를 마친 사람들끼리 뛴다.
     */
    val canStart: Boolean get() = myReady && readyCount >= 1
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
    private val walkSessionDao: WalkSessionDao,
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
        crewOf(crewId)?.let {
            // 크루 id 를 함께 담는다. 알림을 눌렀을 때 그 크루로 갈 수 있어야 한다.
            rewardRepository.notify(NotificationType.CREW_JOINED, it.name, argExtra = it.id)
        }
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
    //
    // 로비를 연 사람(나)이 파티장이다. 파티장은 크루원을 초대·강퇴할 수 있고,
    // 전원 준비되면 "시작"을 눌러 러닝을 개시한다.
    // 러닝 중 파티장에게서 MAX_PARTY_DISTANCE_M 이상 떨어진 크루원은
    // 자동으로 파티에서 빠진다(백엔드가 없으므로 거리는 시뮬레이션).

    private val _party = MutableStateFlow(PartyState())
    val party: StateFlow<PartyState> = _party

    private var driftJob: Job? = null

    /** 시뮬레이션 러너의 고유 ID — 이름에서 결정적으로 만든다 */
    private fun uidOf(name: String): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        var h = name.hashCode()
        val body = buildString {
            repeat(6) {
                append(alphabet[((h % alphabet.length) + alphabet.length) % alphabet.length])
                h = h shr 3
            }
        }
        return "SU-$body"
    }

    private fun member(index: Int, name: String, random: Random, ready: Boolean = false) =
        PartyMember(
            id = "m$index",
            name = name,
            uid = uidOf(name),
            level = 8 + random.nextInt(14),
            ready = ready,
            isMe = false,
            distanceM = 4 + random.nextInt(38),
        )

    /** 로비 입장. 크루원 일부는 이미 준비를 마친 상태로 시작한다. */
    fun openLobby(crewId: String) {
        val crew = crewOf(crewId) ?: return
        if (_party.value.crewId == crewId &&
            _party.value.phase !in listOf(PartyPhase.IDLE, PartyPhase.FINISHED)
        ) {
            return // 이미 이 크루 로비에 있음
        }
        simulationJob?.cancel()
        driftJob?.cancel()
        val random = Random(System.nanoTime())
        val squad = crew.roster.take(3 + random.nextInt(2))
        val members = buildList {
            add(PartyMember("me", "", "", 0, ready = false, isMe = true, distanceM = 0))
            squad.forEachIndexed { index, name ->
                add(member(index, name, random, ready = index == 0 && random.nextBoolean()))
            }
        }
        _party.value = PartyState(
            phase = PartyPhase.LOBBY,
            crewId = crew.id,
            crewName = crew.name,
            members = members,
        )
    }

    /**
     * 번개러닝 로비.
     *
     * 크루 로비와 같은 판을 쓴다 — 준비를 누르고, 파티장이 시작하고, 인원수만큼
     * 적립 부스트가 붙는다. 번개러닝만 이 흐름이 없으면 "모집은 되는데 같이
     * 뛸 수는 없는 글"이 되고, 그건 게시판이지 러닝 기능이 아니다.
     *
     * 크루 로비와 마찬가지로 **로비를 연 사람이 파티장**이다. 주최자만 시작할
     * 수 있게 하면, 주최자가 늦는 날 모인 사람들이 아무것도 못 한다.
     *
     * @param others 나를 뺀 참가자 수. 글의 참가 인원에서 온다.
     */
    fun openFlashLobby(postId: Long, title: String, others: Int) {
        if (_party.value.flashPostId == postId &&
            _party.value.phase !in listOf(PartyPhase.IDLE, PartyPhase.FINISHED)
        ) {
            return // 이미 이 번개 로비에 있음
        }
        simulationJob?.cancel()
        driftJob?.cancel()
        val random = Random(System.nanoTime())
        val squad = EXTRA_RUNNERS.shuffled(random).take(others.coerceIn(0, 9))
        val members = buildList {
            add(PartyMember("me", "", "", 0, ready = false, isMe = true, distanceM = 0))
            squad.forEachIndexed { index, name ->
                add(member(index, name, random, ready = index == 0 && random.nextBoolean()))
            }
        }
        _party.value = PartyState(
            phase = PartyPhase.LOBBY,
            flashPostId = postId,
            crewName = title,
            members = members,
        )
    }

    fun leaveLobby() {
        simulationJob?.cancel()
        driftJob?.cancel()
        _party.value = PartyState()
    }

    /** 파티에 없는 크루원 — 초대 후보 */
    fun inviteCandidates(): List<String> {
        val state = _party.value
        val inParty = state.members.map { it.name }.toSet()
        // 번개러닝 로비에는 크루 명단이 없다. 그때는 일반 러너 중에서 부른다.
        val roster = state.crewId?.let { crewOf(it) }?.roster.orEmpty()
        return (roster + EXTRA_RUNNERS).distinct().filterNot { it in inParty }
    }

    /** 파티장 권한 — 크루원 초대. 초대된 크루원은 잠시 후 준비를 누른다. */
    fun invite(name: String) {
        val state = _party.value
        if (state.phase != PartyPhase.LOBBY) return
        if (state.members.any { it.name == name }) return
        val random = Random(System.nanoTime())
        val newMember = member(state.members.size + 100, name, random)
        _party.value = state.copy(members = state.members + newMember)
        scope.launch {
            delay(900L + random.nextLong(1800))
            val snapshot = _party.value
            if (snapshot.phase != PartyPhase.LOBBY) return@launch
            _party.value = snapshot.copy(
                members = snapshot.members.map {
                    if (it.id == newMember.id) it.copy(ready = true) else it
                },
            )
        }
    }

    /** 파티장 권한 — 크루원 강퇴 */
    fun kick(memberId: String) {
        val state = _party.value
        if (state.phase != PartyPhase.LOBBY) return
        if (memberId == "me") return
        _party.value = state.copy(members = state.members.filterNot { it.id == memberId })
    }

    /** 내 준비 상태 토글. 준비를 누르면 남은 크루원들이 차례로 따라온다. */
    fun setMyReady(ready: Boolean) {
        val state = _party.value
        if (state.phase != PartyPhase.LOBBY) return
        _party.value = state.copy(
            members = state.members.map { if (it.isMe) it.copy(ready = ready) else it },
        )
        if (ready) startSimulation() else simulationJob?.cancel()
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            val random = Random(System.nanoTime())
            while (true) {
                val current = _party.value
                if (current.phase != PartyPhase.LOBBY) return@launch
                val pending = current.members.filter { !it.ready && !it.isMe }
                if (pending.isEmpty()) return@launch
                delay(700L + random.nextLong(1600))
                val snapshot = _party.value
                if (snapshot.phase != PartyPhase.LOBBY || !snapshot.myReady) return@launch
                val next = snapshot.members.firstOrNull { !it.ready && !it.isMe } ?: return@launch
                _party.value = snapshot.copy(
                    members = snapshot.members.map {
                        if (it.id == next.id) it.copy(ready = true) else it
                    },
                )
            }
        }
    }

    /**
     * 파티장 권한 — 준비된 사람들끼리 시작. 카운트다운을 거쳐 동시 측정에 들어간다.
     *
     * 아직 준비하지 않은 사람은 여기서 파티에서 빠진다. 데리고 들어가면
     * 적립 부스트(인원수 배율)에는 들어가면서 실제로는 안 뛰는 사람이 생긴다 —
     * 같이 뛴 사람들 몫을 안 뛴 사람이 나눠 갖는 셈이다.
     */
    fun startParty() {
        val state = _party.value
        if (state.phase != PartyPhase.LOBBY || !state.canStart) return

        val ready = state.members.filter { it.ready }
        val leftBehind = state.members.filterNot { it.ready }
        if (leftBehind.isNotEmpty()) _party.value = state.copy(members = ready)

        simulationJob?.cancel()
        simulationJob = scope.launch {
            // 두고 간 사람은 알림으로 남긴다. 로비에 있다가 조용히 사라지면
            // 본인은 앱이 고장 난 줄 안다.
            //
            // 알림 쓰기는 suspend 라 여기 코루틴 안에서 한다. 화면이 보는
            // 파티 상태는 위에서 이미 바꿔 뒀으므로, 카운트다운은 알림을
            // 기다리지 않고 바로 시작한다.
            leftBehind.forEach {
                rewardRepository.notify(NotificationType.PARTY_MEMBER_LEFT, it.name)
            }
            for (n in 3 downTo 1) {
                val snapshot = _party.value
                if (snapshot.phase !in listOf(PartyPhase.LOBBY, PartyPhase.COUNTDOWN)) return@launch
                _party.value = snapshot.copy(phase = PartyPhase.COUNTDOWN, countdown = n)
                delay(1000)
            }
            val snapshot = _party.value
            if (snapshot.phase != PartyPhase.COUNTDOWN) return@launch
            _party.value = snapshot.copy(phase = PartyPhase.RUNNING, countdown = 0)
            startDistanceDrift()
        }
    }

    /**
     * 러닝 중 거리 시뮬레이션. 크루원 거리가 조금씩 출렁이다
     * [MAX_PARTY_DISTANCE_M]을 넘으면 자동으로 파티에서 빠지고 알림을 남긴다.
     */
    private fun startDistanceDrift() {
        driftJob?.cancel()
        driftJob = scope.launch {
            val random = Random(System.nanoTime())
            while (true) {
                delay(2500)
                val state = _party.value
                if (state.phase != PartyPhase.RUNNING) return@launch
                var dropped: PartyMember? = null
                val updated = state.members.map { m ->
                    if (m.isMe) return@map m
                    val next = (m.distanceM + random.nextInt(-9, 15)).coerceAtLeast(2)
                    m.copy(distanceM = next)
                }
                val survivors = updated.filter { m ->
                    val ok = m.isMe || m.distanceM <= MAX_PARTY_DISTANCE_M
                    if (!ok && dropped == null) dropped = m
                    ok
                }
                _party.value = state.copy(members = survivors)
                dropped?.let {
                    rewardRepository.notify(NotificationType.PARTY_MEMBER_LEFT, it.name)
                }
            }
        }
    }

    /** 세션 정산 후 결과 화면으로 전환 */
    fun finishParty(points: Double, steps: Int) {
        val state = _party.value
        if (state.phase != PartyPhase.RUNNING) return
        simulationJob?.cancel()
        driftJob?.cancel()
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

    /**
     * 지금 달리고 있는 크루 러닝의 크루 id. 크루 러닝이 아니면 빈 문자열이다.
     *
     * 번개러닝 로비도 파티지만 크루가 없다(crewId 가 null 이다). 그 거리를
     * 아무 크루에나 얹을 수는 없으므로 여기서도 빈 문자열이 나간다.
     */
    fun currentPartyCrewId(): String =
        if (_party.value.isActive) _party.value.crewId.orEmpty() else ""

    /**
     * 크루 순위 — 크루 러닝으로 많이 달린 순.
     *
     * 아직 한 번도 같이 달리지 않은 크루는 0 km 로 명단에 남는다. 목록에서
     * 빼 버리면 "우리 크루가 순위에 없다"가 되고, 사용자는 기능이 고장 난
     * 줄 안다. 0 km 는 고장이 아니라 아직 안 달렸다는 뜻이다.
     *
     * @param since 이 시각 이후에 시작한 러닝만 센다. 전체기간이면 0.
     */
    suspend fun ranking(since: Long): List<CrewRank> {
        val totals = walkSessionDao.crewDistances(since).associateBy { it.crewId }
        val joined = joinedCrewIds.first()
        // crews 대신 표를 직접 읽는다. 화면이 열리자마자 순위를 물으면
        // StateFlow 가 아직 첫 값을 못 받아 빈 목록일 수 있다.
        return crewInfoDao.observeAll().first()
            .map { it.toDomain() }
            .map { crew ->
                val total = totals[crew.id]
                CrewRank(
                    // 번호는 정렬한 뒤에 붙인다
                    rank = 0,
                    crewId = crew.id,
                    name = crew.name,
                    monogram = crew.monogram,
                    km = (total?.meters ?: 0.0) / 1000.0,
                    runs = total?.runs ?: 0,
                    joined = crew.id in joined,
                )
            }
            // 같은 거리면 이름순. 순서가 매번 흔들리면 순위표를 믿지 않게 된다.
            .sortedWith(compareByDescending<CrewRank> { it.km }.thenBy { it.name })
            .mapIndexed { index, row -> row.copy(rank = index + 1) }
    }

    companion object {
        /** 파티장에게서 이만큼 떨어지면 자동으로 파티에서 빠진다 */
        const val MAX_PARTY_DISTANCE_M = 100

        /** 초대 후보를 채우는 여분 러너들 */
        private val EXTRA_RUNNERS = listOf("Riley P.", "Sena K.", "Théo M.", "Ines V.", "Milo J.")
    }
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
