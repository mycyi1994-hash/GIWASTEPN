package com.giwa.strideup.domain

/**
 * 커뮤니티 도메인 — 게시판 글, 크루, 랭킹.
 *
 * 게시판은 앱 사용자 전체가 쓰는 열린 공간(crewId 비어 있음)이고,
 * 크루 게시판은 가입한 크루에만 보이는 작은 커뮤니티다.
 */

// ─────────────────────────────────────────────────────────────
// 게시글
// ─────────────────────────────────────────────────────────────

enum class PostCategory(val id: String) {
    /** 번개러닝 — 지금 같이 뛸 사람 모집. 가까운 순으로 정렬한다. */
    FLASH("FLASH"),

    /** 자유 게시판 */
    FREE("FREE"),

    /** 꿀팁 · 정보 공유 */
    TIP("TIP");

    companion object {
        fun of(id: String): PostCategory = entries.firstOrNull { it.id == id } ?: FREE
    }
}

data class Post(
    val id: Long,
    val category: PostCategory,
    /** 빈 문자열이면 전체 게시판 */
    val crewId: String,
    val author: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val likes: Int,
    val liked: Boolean,
    val commentCount: Int,
    /** 내가 쓴 글 */
    val mine: Boolean,
    // ── 번개러닝 전용 ──
    val place: String,
    val distanceKm: Double,
    val meetAt: Long,
    val capacity: Int,
    val joinedCount: Int,
    val joined: Boolean,
) {
    val isFlash: Boolean get() = category == PostCategory.FLASH
    val isFull: Boolean get() = capacity > 0 && joinedCount >= capacity
    val isClosed: Boolean get() = isFlash && meetAt > 0L && meetAt < System.currentTimeMillis()
}

// ─────────────────────────────────────────────────────────────
// 댓글
// ─────────────────────────────────────────────────────────────

data class Comment(
    val id: Long,
    val postId: Long,
    /** 0이면 최상위 댓글, 그 외에는 부모 댓글 id */
    val parentId: Long,
    val author: String,
    val body: String,
    val createdAt: Long,
    val mine: Boolean,
) {
    val isReply: Boolean get() = parentId != 0L
}

/** 댓글 하나와 거기 달린 답글들 */
data class CommentThread(
    val comment: Comment,
    val replies: List<Comment>,
) {
    val size: Int get() = 1 + replies.size
}

/** 평평한 댓글 목록을 부모–답글 묶음으로 정리한다. 고아 답글은 최상위로 올린다. */
fun List<Comment>.toThreads(): List<CommentThread> {
    val byParent = filter { it.isReply }.groupBy { it.parentId }
    val ids = mapTo(mutableSetOf()) { it.id }
    val roots = filter { !it.isReply || it.parentId !in ids }
    return roots.map { root ->
        CommentThread(
            comment = root,
            replies = byParent[root.id].orEmpty().sortedBy { it.createdAt },
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 랭킹
// ─────────────────────────────────────────────────────────────

/**
 * 개인 랭킹 부문.
 *
 * 걸음 수 하나로 줄을 세우면 "많이 걷기"만 남는다. 러닝은 빠르기·지구력·꾸준함이
 * 서로 다른 능력이라, 잘하는 축이 다른 사람이 각자 오를 자리를 갖도록 셋으로 나눴다.
 */
enum class RankBoard {
    /** 쾌속 — 세션 최고 속도(km/h) */
    TOP_SPEED,

    /** 지구력 — 러닝에 쓴 누적 시간 */
    LONGEST_TIME,

    /** 적립 — 누적 SUP */
    TOTAL_SUP,
}

data class RankEntry(
    val rank: Int,
    val name: String,
    val monogram: String,
    /** 역대 최고 속도(km/h) */
    val topSpeedKmh: Double,
    /** 러닝에 쓴 누적 시간(초) */
    val activeSec: Long,
    val sup: Double,
    val isMe: Boolean,
)

/**
 * 백엔드가 없으므로 리더보드 상대는 고정 시드로 만든다.
 * 값이 매번 흔들리면 순위가 신뢰를 잃으므로, 이름마다 결정적인 수치를 부여한다.
 */
object Leaderboard {

    private data class Rival(val name: String, val monogram: String, val seed: Int)

    private val rivals = listOf(
        Rival("Maya C.", "MC", 71),
        Rival("Jun H.", "JH", 63),
        Rival("Elena R.", "ER", 58),
        Rival("Marco P.", "MP", 52),
        Rival("Sora K.", "SK", 47),
        Rival("Diego M.", "DM", 43),
        Rival("Aiko T.", "AT", 39),
        Rival("Tomas L.", "TL", 34),
        Rival("Priya N.", "PN", 30),
        Rival("Owen D.", "OD", 26),
        Rival("Zara F.", "ZF", 22),
        Rival("Kai W.", "KW", 18),
        Rival("Nora S.", "NS", 14),
        Rival("Lena B.", "LB", 11),
        Rival("Théo M.", "TM", 8),
    )

    /**
     * 상대의 최고 속도 — 9.0~19.5 km/h 사이에 흩뿌린다.
     * 시드 순서와 어긋나게 섞어, 적립 1등이 속도 1등도 하는 일이 없게 한다.
     */
    private fun rivalSpeed(seed: Int): Double = 9.0 + (seed * 7 % 43) * 0.25

    /** 상대의 누적 러닝 시간 — 시드에 비례하되 속도와는 다른 순서로 */
    private fun rivalActiveSec(seed: Int): Long = (seed * 13 % 97) * 1_450L + 3_600L

    private fun rivalSup(seed: Int): Double = seed * 74.5 + 260.0

    /**
     * 내 실적을 끼워 넣은 순위표.
     *
     * @param myName "나"에 해당하는 표시 이름
     * @param myTopSpeedKmh 판정을 통과한 구간에서 기록한 역대 최고 속도
     * @param myActiveSec 러닝에 쓴 누적 시간(초)
     * @param myTotalSup 실제 누적 SUP
     */
    fun build(
        board: RankBoard,
        myName: String,
        myTopSpeedKmh: Double,
        myActiveSec: Long,
        myTotalSup: Double,
    ): List<RankEntry> {
        val rows = rivals.map { rival ->
            RankEntry(
                rank = 0,
                name = rival.name,
                monogram = rival.monogram,
                topSpeedKmh = rivalSpeed(rival.seed),
                activeSec = rivalActiveSec(rival.seed),
                sup = rivalSup(rival.seed),
                isMe = false,
            )
        } + RankEntry(
            rank = 0,
            name = myName,
            monogram = "ME",
            topSpeedKmh = myTopSpeedKmh,
            activeSec = myActiveSec,
            sup = myTotalSup,
            isMe = true,
        )

        val sorted = when (board) {
            RankBoard.TOP_SPEED -> rows.sortedWith(
                compareByDescending<RankEntry> { it.topSpeedKmh }.thenBy { it.name }
            )
            RankBoard.LONGEST_TIME -> rows.sortedWith(
                compareByDescending<RankEntry> { it.activeSec }.thenBy { it.name }
            )
            RankBoard.TOTAL_SUP -> rows.sortedWith(
                compareByDescending<RankEntry> { it.sup }.thenBy { it.name }
            )
        }
        return sorted.mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }
}

// ─────────────────────────────────────────────────────────────
// 종족 랭킹
// ─────────────────────────────────────────────────────────────

/**
 * 종족 순위 한 줄.
 *
 * @param km 그 종족 신발을 신고 달린 거리의 합
 * @param myKm 그중 내가 기여한 거리
 */
data class FactionRank(
    val rank: Int,
    val faction: Faction,
    val km: Double,
    val myKm: Double,
    /** 내가 지금 이 종족 신발을 신고 있는지 */
    val isMine: Boolean,
) {
    /** 내 기여 비중(0..1) */
    val myShare: Float
        get() = if (km > 0.0) (myKm / km).coerceIn(0.0, 1.0).toFloat() else 0f
}

/**
 * 종족별 누적 거리 순위.
 *
 * 개인 랭킹이 "나 vs 남"이라면 이건 "우리 vs 저쪽"이다. 신발을 고르는 행위가
 * 내 부스트뿐 아니라 소속을 정하는 선택이 되고, 한 번 달릴 때마다 그 소속에
 * 거리가 쌓인다. 내가 1등을 못 해도 우리 종족은 1등일 수 있다.
 *
 * 백엔드가 없으므로 다른 러너들의 누적은 종족마다 고정된 기준값으로 둔다.
 * 값이 실행할 때마다 흔들리면 순위가 의미를 잃는다.
 */
object FactionLeaderboard {

    /** 종족별 다른 러너들의 누적 거리(km). 고정값이라 순위가 흔들리지 않는다. */
    private fun baseKm(faction: Faction): Double = when (faction) {
        Faction.FIRE -> 12_840.5
        Faction.WATER -> 11_930.2
        Faction.LIGHTNING -> 13_505.8
        Faction.WIND -> 10_460.4
    }

    fun build(myKm: Map<Faction, Double>, myFaction: Faction?): List<FactionRank> =
        Faction.entries
            .map { faction ->
                val mine = myKm[faction] ?: 0.0
                FactionRank(
                    rank = 0,
                    faction = faction,
                    km = baseKm(faction) + mine,
                    myKm = mine,
                    isMine = faction == myFaction,
                )
            }
            .sortedWith(compareByDescending<FactionRank> { it.km }.thenBy { it.faction.ordinal })
            .mapIndexed { index, row -> row.copy(rank = index + 1) }
}
