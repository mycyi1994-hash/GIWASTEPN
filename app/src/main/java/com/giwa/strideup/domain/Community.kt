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

enum class RankBoard {
    /** 이번 주 걸음 수 */
    WEEKLY_STEPS,

    /** 누적 SUP */
    TOTAL_SUP,
}

data class RankEntry(
    val rank: Int,
    val name: String,
    val monogram: String,
    val steps: Long,
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
     * 내 실적을 끼워 넣은 순위표.
     *
     * @param myName "나"에 해당하는 표시 이름
     * @param myWeeklySteps 이번 주 실제 걸음 수
     * @param myTotalSup 실제 누적 SUP
     */
    fun build(
        board: RankBoard,
        myName: String,
        myWeeklySteps: Long,
        myTotalSup: Double,
    ): List<RankEntry> {
        val rows = rivals.map { rival ->
            RankEntry(
                rank = 0,
                name = rival.name,
                monogram = rival.monogram,
                steps = rival.seed * 1_130L + 4_800L,
                sup = rival.seed * 74.5 + 260.0,
                isMe = false,
            )
        } + RankEntry(
            rank = 0,
            name = myName,
            monogram = "ME",
            steps = myWeeklySteps,
            sup = myTotalSup,
            isMe = true,
        )

        val sorted = when (board) {
            RankBoard.WEEKLY_STEPS -> rows.sortedWith(
                compareByDescending<RankEntry> { it.steps }.thenBy { it.name }
            )
            RankBoard.TOTAL_SUP -> rows.sortedWith(
                compareByDescending<RankEntry> { it.sup }.thenBy { it.name }
            )
        }
        return sorted.mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }
}
