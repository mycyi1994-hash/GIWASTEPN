package com.stepup.android.domain

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
