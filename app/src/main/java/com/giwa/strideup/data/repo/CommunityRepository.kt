package com.giwa.strideup.data.repo

import android.content.Context
import com.giwa.strideup.R
import com.giwa.strideup.data.local.CommentDao
import com.giwa.strideup.data.local.CommentEntity
import com.giwa.strideup.data.local.NotificationType
import com.giwa.strideup.data.local.PostDao
import com.giwa.strideup.data.local.PostEntity
import com.giwa.strideup.domain.Comment
import com.giwa.strideup.domain.CommentThread
import com.giwa.strideup.domain.Post
import com.giwa.strideup.domain.PostCategory
import com.giwa.strideup.domain.toThreads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 커뮤니티 게시판.
 *
 * 전체 게시판(crewId = "")과 크루 게시판(crewId = 크루 ID)이 같은 표를 쓴다.
 * 백엔드가 없으므로 첫 실행 시 데모 글을 시드하고, 이후 사용자가 쓴 글이 위에 쌓인다.
 * 시드 문구는 문자열 리소스에서 읽어 설치 언어를 따른다.
 */
class CommunityRepository(
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val rewardRepository: RewardRepository,
    private val appContext: Context,
) {

    /** 데모용 답글을 잠시 뒤에 붙이는 용도 — 화면이 사라져도 살아 있어야 한다 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val posts: Flow<List<Post>> = postDao.observeAll().map { list -> list.map { it.toDomain() } }

    /** 전체 게시판 글만 */
    val boardPosts: Flow<List<Post>> = posts.map { list -> list.filter { it.crewId.isEmpty() } }

    fun crewPosts(crewId: String): Flow<List<Post>> =
        posts.map { list -> list.filter { it.crewId == crewId } }

    suspend fun ensureSeeded() {
        if (postDao.count() > 0) return
        val ids = postDao.insertAll(seed())
        seedComments(ids)
    }

    /**
     * 데모 댓글. 글 카드에 보이는 댓글 수와 실제 목록이 어긋나지 않도록
     * 시드 댓글을 넣고 그 개수로 카운트를 다시 맞춘다.
     * 두 번째 글에는 답글까지 달아 스레드 모양을 바로 볼 수 있게 한다.
     */
    private suspend fun seedComments(postIds: List<Long>) {
        val now = System.currentTimeMillis()
        postIds.forEachIndexed { index, postId ->
            val howMany = SEED_COMMENT_COUNTS[index % SEED_COMMENT_COUNTS.size]
            var previous = 0L
            repeat(howMany) { slot ->
                val pick = (index * 3 + slot) % SEED_COMMENTS.size
                val id = commentDao.insert(
                    CommentEntity(
                        postId = postId,
                        parentId = 0L,
                        author = DEMO_RESPONDERS[(index + slot) % DEMO_RESPONDERS.size],
                        body = s(SEED_COMMENTS[pick]),
                        createdAt = now - (howMany - slot) * 11 * 60_000L,
                        mine = false,
                    )
                )
                if (slot == 0) previous = id
            }
            if (howMany >= 2 && previous != 0L) {
                commentDao.insert(
                    CommentEntity(
                        postId = postId,
                        parentId = previous,
                        author = DEMO_RESPONDERS[(index + 2) % DEMO_RESPONDERS.size],
                        body = s(DEMO_REPLIES[index % DEMO_REPLIES.size]),
                        createdAt = now - 6 * 60_000L,
                        mine = false,
                    )
                )
            }
            syncCount(postId)
        }
    }

    suspend fun toggleLike(id: Long) {
        val entity = postDao.byId(id) ?: return
        postDao.update(
            entity.copy(
                liked = !entity.liked,
                likes = (entity.likes + if (entity.liked) -1 else 1).coerceAtLeast(0),
            )
        )
    }

    /** 번개러닝 참가/취소 */
    suspend fun toggleJoinFlash(id: Long) {
        val entity = postDao.byId(id) ?: return
        if (entity.category != PostCategory.FLASH.id) return
        val joining = !entity.joined
        if (joining && entity.capacity > 0 && entity.joinedCount >= entity.capacity) return
        postDao.update(
            entity.copy(
                joined = joining,
                joinedCount = (entity.joinedCount + if (joining) 1 else -1).coerceAtLeast(0),
            )
        )
    }

    suspend fun write(
        category: PostCategory,
        title: String,
        body: String,
        author: String,
        crewId: String = "",
        place: String = "",
        distanceKm: Double = 0.0,
        meetInMinutes: Int = 0,
        capacity: Int = 0,
    ): Long = postDao.insert(
        PostEntity(
            category = category.id,
            crewId = crewId,
            author = author,
            title = title.trim(),
            body = body.trim(),
            createdAt = System.currentTimeMillis(),
            likes = 0,
            liked = false,
            commentCount = 0,
            mine = true,
            place = place.trim(),
            distanceKm = distanceKm,
            meetAt = if (category == PostCategory.FLASH) {
                System.currentTimeMillis() + meetInMinutes * 60_000L
            } else {
                0L
            },
            capacity = if (category == PostCategory.FLASH) capacity.coerceAtLeast(2) else 0,
            joinedCount = if (category == PostCategory.FLASH) 1 else 0,
            joined = category == PostCategory.FLASH,
        )
    )

    suspend fun delete(id: Long) {
        val entity = postDao.byId(id) ?: return
        if (!entity.mine) return
        commentDao.deleteForPost(id)
        postDao.delete(id)
    }

    // ── 댓글 ─────────────────────────────────────────────────

    /** 한 글의 댓글을 부모–답글 묶음으로 */
    fun commentThreads(postId: Long): Flow<List<CommentThread>> =
        commentDao.observeForPost(postId).map { list -> list.map { it.toDomain() }.toThreads() }

    /**
     * 댓글/답글 작성.
     *
     * @param parentId 0이면 새 댓글, 그 외에는 그 댓글에 대한 답글
     */
    suspend fun addComment(postId: Long, body: String, author: String, parentId: Long = 0L) {
        val text = body.trim()
        if (text.isEmpty()) return
        if (postDao.byId(postId) == null) return
        commentDao.insert(
            CommentEntity(
                postId = postId,
                parentId = parentId,
                author = author,
                body = text,
                createdAt = System.currentTimeMillis(),
                mine = true,
            )
        )
        syncCount(postId)
        if (parentId == 0L) scheduleDemoReply(postId)
    }

    suspend fun deleteComment(id: Long) {
        val entity = commentDao.byId(id) ?: return
        if (!entity.mine) return
        commentDao.deleteWithReplies(id)
        syncCount(entity.postId)
    }

    private suspend fun syncCount(postId: Long) {
        val post = postDao.byId(postId) ?: return
        val count = commentDao.countForPost(postId)
        if (post.commentCount != count) postDao.update(post.copy(commentCount = count))
    }

    /**
     * 백엔드가 없으므로 답글은 시뮬레이션한다.
     * 내가 댓글을 남기면 잠시 뒤 다른 러너가 답글을 달고, 알림함에 알림이 뜬다.
     */
    private fun scheduleDemoReply(postId: Long) {
        scope.launch {
            delay(REPLY_DELAY_MS)
            // 가장 최근에 내가 남긴 최상위 댓글에 답글을 붙인다
            val target = commentDao.latestMineTopLevel(postId) ?: return@launch
            if (commentDao.replyCount(target.id) > 0) return@launch
            val responder = DEMO_RESPONDERS[(target.id % DEMO_RESPONDERS.size).toInt()]
            val bodyRes = DEMO_REPLIES[(target.id % DEMO_REPLIES.size).toInt()]
            commentDao.insert(
                CommentEntity(
                    postId = postId,
                    parentId = target.id,
                    author = responder,
                    body = s(bodyRes),
                    createdAt = System.currentTimeMillis(),
                    mine = false,
                )
            )
            syncCount(postId)
            rewardRepository.notify(
                type = NotificationType.COMMENT_REPLY,
                argText = responder,
                argExtra = postId.toString(),
            )
        }
    }

    // ── 데모 시드 ────────────────────────────────────────────

    private fun s(resId: Int): String = appContext.getString(resId)

    private fun seed(): List<PostEntity> {
        val now = System.currentTimeMillis()
        val minute = 60_000L
        val hour = 60 * minute

        fun flash(
            author: String,
            titleRes: Int,
            bodyRes: Int,
            place: String,
            km: Double,
            inMinutes: Int,
            capacity: Int,
            joinedCount: Int,
            crewId: String = "",
        ) = PostEntity(
            category = PostCategory.FLASH.id,
            crewId = crewId,
            author = author,
            title = s(titleRes),
            body = s(bodyRes),
            createdAt = now - (inMinutes / 2) * minute,
            likes = joinedCount * 2,
            liked = false,
            commentCount = 0,
            mine = false,
            place = place,
            distanceKm = km,
            meetAt = now + inMinutes * minute,
            capacity = capacity,
            joinedCount = joinedCount,
            joined = false,
        )

        fun text(
            category: PostCategory,
            author: String,
            titleRes: Int,
            bodyRes: Int,
            hoursAgo: Int,
            likes: Int,
            comments: Int,
            crewId: String = "",
        ) = PostEntity(
            category = category.id,
            crewId = crewId,
            author = author,
            title = s(titleRes),
            body = s(bodyRes),
            createdAt = now - hoursAgo * hour,
            likes = likes,
            liked = false,
            commentCount = 0,
            mine = false,
            place = "",
            distanceKm = 0.0,
            meetAt = 0L,
            capacity = 0,
            joinedCount = 0,
            joined = false,
        )

        return listOf(
            // ── 번개러닝 — 거리를 다양하게 두어 "가까운 순" 정렬이 드러나게 한다 ──
            flash(
                author = "Sora K.",
                titleRes = R.string.seed_flash1_title,
                bodyRes = R.string.seed_flash1_body,
                place = "Riverside Park Gate 3",
                km = 0.4,
                inMinutes = 90,
                capacity = 8,
                joinedCount = 5,
            ),
            flash(
                author = "Marco P.",
                titleRes = R.string.seed_flash2_title,
                bodyRes = R.string.seed_flash2_body,
                place = "Cedar Track",
                km = 1.2,
                inMinutes = 260,
                capacity = 6,
                joinedCount = 2,
            ),
            flash(
                author = "Elena R.",
                titleRes = R.string.seed_flash3_title,
                bodyRes = R.string.seed_flash3_body,
                place = "Summit Stadium",
                km = 2.6,
                inMinutes = 180,
                capacity = 4,
                joinedCount = 3,
            ),
            flash(
                author = "Kai W.",
                titleRes = R.string.seed_flash4_title,
                bodyRes = R.string.seed_flash4_body,
                place = "North Bridge",
                km = 4.1,
                inMinutes = 600,
                capacity = 10,
                joinedCount = 6,
            ),

            // ── 자유 게시판 ──
            text(
                PostCategory.FREE, "Alex R.",
                R.string.seed_free1_title, R.string.seed_free1_body,
                hoursAgo = 2, likes = 45, comments = 12,
            ),
            text(
                PostCategory.FREE, "Priya N.",
                R.string.seed_free2_title, R.string.seed_free2_body,
                hoursAgo = 5, likes = 18, comments = 24,
            ),
            text(
                PostCategory.FREE, "Owen D.",
                R.string.seed_free3_title, R.string.seed_free3_body,
                hoursAgo = 9, likes = 31, comments = 7,
            ),

            // ── 꿀팁 ──
            text(
                PostCategory.TIP, "Aiko T.",
                R.string.seed_tip1_title, R.string.seed_tip1_body,
                hoursAgo = 6, likes = 88, comments = 19,
            ),
            text(
                PostCategory.TIP, "Tomas L.",
                R.string.seed_tip2_title, R.string.seed_tip2_body,
                hoursAgo = 14, likes = 132, comments = 41,
            ),
            text(
                PostCategory.TIP, "Zara F.",
                R.string.seed_tip3_title, R.string.seed_tip3_body,
                hoursAgo = 27, likes = 76, comments = 15,
            ),

            // ── 크루 게시판 ──
            text(
                PostCategory.FREE, "Maya C.",
                R.string.seed_crew1_title, R.string.seed_crew1_body,
                hoursAgo = 3, likes = 12, comments = 8,
                crewId = "trailblazer",
            ),
            text(
                PostCategory.TIP, "Jun H.",
                R.string.seed_crew2_title, R.string.seed_crew2_body,
                hoursAgo = 20, likes = 9, comments = 3,
                crewId = "trailblazer",
            ),
            flash(
                author = "Diego M.",
                titleRes = R.string.seed_crew3_title,
                bodyRes = R.string.seed_crew3_body,
                place = "Riverside Park",
                km = 1.3,
                inMinutes = 120,
                capacity = 12,
                joinedCount = 7,
                crewId = "night_runners",
            ),
        )
    }
}

private const val REPLY_DELAY_MS = 9_000L

private val DEMO_RESPONDERS = listOf("Sora K.", "Marco P.", "Aiko T.", "Kai W.", "Elena R.")

/** 글마다 심을 데모 댓글 개수 */
private val SEED_COMMENT_COUNTS = listOf(2, 3, 1, 2, 3, 1, 2)

private val SEED_COMMENTS = listOf(
    R.string.seed_comment1,
    R.string.seed_comment2,
    R.string.seed_comment3,
    R.string.seed_comment4,
    R.string.seed_comment5,
    R.string.seed_comment6,
)

private val DEMO_REPLIES = listOf(
    R.string.seed_reply1,
    R.string.seed_reply2,
    R.string.seed_reply3,
    R.string.seed_reply4,
)

/** Room 엔티티 → 도메인 모델 */
fun CommentEntity.toDomain(): Comment = Comment(
    id = id,
    postId = postId,
    parentId = parentId,
    author = author,
    body = body,
    createdAt = createdAt,
    mine = mine,
)

/** Room 엔티티 → 도메인 모델 */
fun PostEntity.toDomain(): Post = Post(
    id = id,
    category = PostCategory.of(category),
    crewId = crewId,
    author = author,
    title = title,
    body = body,
    createdAt = createdAt,
    likes = likes,
    liked = liked,
    commentCount = commentCount,
    mine = mine,
    place = place,
    distanceKm = distanceKm,
    meetAt = meetAt,
    capacity = capacity,
    joinedCount = joinedCount,
    joined = joined,
)
