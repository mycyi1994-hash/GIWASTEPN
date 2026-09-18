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

/**
 * 순위를 매길 기간.
 *
 * 전체기간만 있으면 순위표는 일찍 시작한 사람의 명단이 된다. 어제 가입한
 * 사람이 아무리 달려도 3년 치 누적을 따라잡을 수 없고, 따라잡을 수 없는
 * 순위표는 두 번 보지 않는다. 오늘·이번 주·이번 달은 누구에게나 0부터다.
 */
enum class RankPeriod {
    /** 최근 24시간 */
    DAY,

    /** 최근 7일 */
    WEEK,

    /** 최근 30일 */
    MONTH,

    /** 처음부터 지금까지 */
    ALL;

    /**
     * 이 기간이 시작되는 시각(epoch ms). 전체기간은 0 — 시간의 시작이다.
     *
     * "이번 주"를 월요일 0시로 끊지 않고 최근 7일로 잡았다. 월요일 0시로
     * 끊으면 일요일 밤에 올린 기록이 몇 시간 만에 사라지고, 사용자는
     * 기록이 지워졌다고 생각한다.
     */
    fun sinceMillis(now: Long = System.currentTimeMillis()): Long = when (this) {
        DAY -> now - DAY_MS
        WEEK -> now - 7 * DAY_MS
        MONTH -> now - 30 * DAY_MS
        ALL -> 0L
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
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

// ─────────────────────────────────────────────────────────────
// 크루 랭킹
// ─────────────────────────────────────────────────────────────

/**
 * 크루 순위 한 줄.
 *
 * 개인 순위가 "나 vs 남"이고 종족 순위가 "우리 편 vs 저쪽"이라면, 이건
 * "우리 모임 vs 저 모임"이다. 세는 것은 **크루 러닝으로 달린 거리**뿐이다 —
 * 크루원이 혼자 달린 거리까지 얹으면 사람 많은 크루가 자동으로 1등이 되고,
 * 그러면 크루 러닝을 여는 이유가 사라진다.
 *
 * @param km 크루 러닝으로 달린 거리의 합
 * @param runs 그 거리를 만든 크루 러닝 횟수
 * @param joined 내가 가입한 크루인지
 */
data class CrewRank(
    val rank: Int,
    val crewId: String,
    val name: String,
    val monogram: String,
    val km: Double,
    val runs: Int,
    val joined: Boolean,
)
