package com.stepup.android.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.data.repo.SneakerRepository
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.CommentTarget
import com.stepup.android.data.repo.CommunityRepository
import com.stepup.android.data.repo.Crew
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.FactionRankingState
import com.stepup.android.data.repo.RankingRepository
import com.stepup.android.data.repo.RankingState
import com.stepup.android.data.repo.RewardRepository
import com.stepup.android.domain.CommentThread
import com.stepup.android.domain.CrewRank
import com.stepup.android.domain.Post
import com.stepup.android.domain.PostCategory
import com.stepup.android.domain.RankBoard
import com.stepup.android.domain.RankPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 커뮤니티 최상단 세그먼트 */
enum class CommunityTab { BOARD, CREW }

/**
 * 게시판 필터.
 *
 * 카테고리(번개·자유·꿀팁)와 핫글을 한 줄에 같이 둔다. 핫글은 카테고리가
 * 아니라 **뽑힌 목록**이라 PostCategory 에 넣을 수 없다 — 글은 자유이면서
 * 동시에 핫글일 수 있다.
 */
enum class BoardFilter(val category: PostCategory?) {
    ALL(null),
    FLASH(PostCategory.FLASH),
    HOT(null),
    FREE(PostCategory.FREE),
    TIP(PostCategory.TIP),
}

class CommunityViewModel(
    private val crewRepository: CrewRepository,
    private val communityRepository: CommunityRepository,
    private val rankingRepository: RankingRepository,
    rewardRepository: RewardRepository,
    private val sneakerRepository: SneakerRepository,
) : ViewModel() {

    init {
        // 갱신할 때가 지났으면 이번 주 핫글을 다시 뽑는다. 때가 아니면
        // 아무 일도 하지 않으므로 화면이 열릴 때마다 불러도 된다.
        viewModelScope.launch { communityRepository.refreshHotIfDue() }
    }

    val crews: StateFlow<List<Crew>> = crewRepository.crews

    val joinedCrewIds: StateFlow<Set<String>> = crewRepository.joinedCrewIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val boardPosts: StateFlow<List<Post>> = communityRepository.boardPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allPosts: StateFlow<List<Post>> = communityRepository.posts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val balance: StateFlow<Double> = rewardRepository.balance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // ── 순위표 ──────────────────────────────────────────────────────
    //
    // 서버에서 가져온다. 예전에는 상대 15명이 코드에 박혀 있었고, 그러면
    // "당신은 3등입니다"가 거짓말이 된다.
    //
    // 부문별로 받아 두고 다시 쓴다. 탭을 오갈 때마다 다시 물으면 같은 답을
    // 받으려고 네트워크를 쓰는 셈이다.

    /** 순위표 하나를 가리키는 열쇠 — 부문 하나에 기간 넷이라 둘이 함께 와야 한다 */
    private data class BoardKey(val board: RankBoard, val period: RankPeriod)

    private val boards = MutableStateFlow<Map<BoardKey, RankingState>>(emptyMap())

    private val selectedBoard = MutableStateFlow(RankBoard.TOP_SPEED)

    private val _period = MutableStateFlow(RankPeriod.ALL)

    /** 지금 보고 있는 기간 */
    val period: StateFlow<RankPeriod> = _period

    /** 지금 보고 있는 부문·기간의 순위 */
    val ranking: StateFlow<RankingState> =
        combine(selectedBoard, _period, boards) { board, period, cache ->
            cache[BoardKey(board, period)] ?: RankingState.Loading
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RankingState.Loading)

    private val factionBoards = MutableStateFlow<Map<RankPeriod, FactionRankingState>>(emptyMap())

    val factionRanking: StateFlow<FactionRankingState> =
        combine(_period, factionBoards) { period, cache ->
            cache[period] ?: FactionRankingState.Loading
        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                FactionRankingState.Loading,
            )

    // ── 크루 순위 ──────────────────────────────────────────────────
    //
    // 이것만 기기 안에서 계산한다. 크루와 크루 러닝이 아직 이 기기에만
    // 있기 때문이다. 서버에 없는 것을 서버에 묻는 척할 수는 없다.

    private val crewBoards = MutableStateFlow<Map<RankPeriod, List<CrewRank>>>(emptyMap())

    /** 지금 기간의 크루 순위. 아직 세는 중이면 null 이다. */
    val crewRanking: StateFlow<List<CrewRank>?> =
        combine(_period, crewBoards) { period, cache -> cache[period] }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 커뮤니티 첫 화면의 "내 순위" 미리보기. 아직 모르면 null 이다.
     *
     * 적립 부문 · 전체기간을 쓴다 — 세 부문 중 누구에게나 값이 있는 축이고,
     * 미리보기 한 줄에 "최근 30일 기준"까지 붙일 자리는 없다.
     */
    val mySupRank: StateFlow<Int?> = boards
        .map {
            (it[BoardKey(RankBoard.TOTAL_SUP, RankPeriod.ALL)] as? RankingState.Ready)?.me?.rank
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 기간 탭. 보고 있던 부문은 그대로 두고 기간만 바꾼다.
     *
     * 여기서 불러오지는 않는다. 지금 어느 부문을 보고 있는지는 화면만 알고,
     * 화면이 기간이 바뀐 것을 보고 그 부문 하나를 불러온다. 여기서 셋을 다
     * 불러 두면 탭 한 번에 요청이 셋 나가고, 그중 둘은 아무도 안 본다.
     */
    fun selectPeriod(next: RankPeriod) {
        _period.value = next
    }

    /**
     * @param meLabel 내 줄에 붙일 이름. 화면의 문자열 자원에서 온다.
     * @param force 이미 받아 둔 것도 다시 받는다 (당겨서 새로고침)
     */
    fun loadRanking(board: RankBoard, meLabel: String, force: Boolean = false) {
        selectedBoard.value = board
        val key = BoardKey(board, _period.value)
        if (!force && boards.value[key] is RankingState.Ready) return
        viewModelScope.launch {
            boards.value = boards.value + (key to RankingState.Loading)
            boards.value = boards.value +
                (key to rankingRepository.personal(key.board, key.period, meLabel))
        }
    }

    fun loadFactionRanking(force: Boolean = false) {
        val period = _period.value
        if (!force && factionBoards.value[period] is FactionRankingState.Ready) return
        viewModelScope.launch {
            factionBoards.value = factionBoards.value + (period to FactionRankingState.Loading)
            val myFaction = sneakerRepository.equipped.first()?.faction
            val next = rankingRepository.factions(myFaction, period)
            factionBoards.value = factionBoards.value + (period to next)
        }
    }

    fun loadCrewRanking(force: Boolean = false) {
        val period = _period.value
        if (!force && crewBoards.value.containsKey(period)) return
        viewModelScope.launch {
            crewBoards.value = crewBoards.value +
                (period to crewRepository.ranking(period.sinceMillis()))
        }
    }

    /** 선택된 세그먼트 — 탭을 오갔다 와도 유지된다 */
    val tab = MutableStateFlow(CommunityTab.BOARD)

    /** 게시판 필터 */
    val boardFilter = MutableStateFlow(BoardFilter.ALL)

    /** 이번 주 핫글 — 점수 높은 순으로 최대 30개 */
    val hotPosts: StateFlow<List<Post>> = communityRepository.hotPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 댓글 창을 열어 둔 글의 id. null이면 닫혀 있다 */
    val openCommentsFor = MutableStateFlow<Long?>(null)

    /**
     * 알림에서 눌러 들어온 댓글. 댓글 창이 이 댓글까지 스크롤하고 표시해 준다.
     * 0이면 특정 댓글을 가리키지 않는다.
     */
    val focusCommentId = MutableStateFlow(0L)

    /** 알림함이 남긴 "이 댓글로" 신호 */
    val commentFocus: StateFlow<CommentTarget?> = communityRepository.commentFocus

    fun selectFilter(filter: BoardFilter) {
        boardFilter.value = filter
    }

    fun openComments(postId: Long) {
        focusCommentId.value = 0L
        openCommentsFor.value = postId
    }

    /** 알림에서 들어온 경로 — 글의 댓글 창을 열고 그 댓글을 가리킨다 */
    fun openCommentsFocused(target: CommentTarget) {
        openCommentsFor.value = target.postId
        focusCommentId.value = target.commentId
        communityRepository.clearCommentFocus()
    }

    fun closeComments() {
        openCommentsFor.value = null
        focusCommentId.value = 0L
    }

    fun commentThreads(postId: Long): Flow<List<CommentThread>> =
        communityRepository.commentThreads(postId)

    fun sendComment(postId: Long, body: String, parentId: Long, author: String) {
        viewModelScope.launch {
            communityRepository.addComment(postId, body, author, parentId)
        }
    }

    fun deleteComment(id: Long) {
        viewModelScope.launch { communityRepository.deleteComment(id) }
    }

    fun crewOf(id: String): Crew? = crewRepository.crewOf(id)

    fun crewPosts(crewId: String): Flow<List<Post>> = communityRepository.crewPosts(crewId)

    fun selectTab(next: CommunityTab) {
        tab.value = next
    }

    fun toggleJoin(crewId: String) {
        viewModelScope.launch {
            if (joinedCrewIds.value.contains(crewId)) {
                crewRepository.leave(crewId)
            } else {
                crewRepository.join(crewId)
            }
        }
    }

    fun toggleLike(postId: Long) {
        viewModelScope.launch { communityRepository.toggleLike(postId) }
    }

    fun toggleJoinFlash(postId: Long) {
        viewModelScope.launch { communityRepository.toggleJoinFlash(postId) }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch { communityRepository.delete(postId) }
    }

    fun writePost(
        category: PostCategory,
        title: String,
        body: String,
        author: String,
        crewId: String,
        place: String,
        distanceKm: Double,
        meetInMinutes: Int,
        capacity: Int,
    ) {
        viewModelScope.launch {
            communityRepository.write(
                category = category,
                title = title,
                body = body,
                author = author,
                crewId = crewId,
                place = place,
                distanceKm = distanceKm,
                meetInMinutes = meetInMinutes,
                capacity = capacity,
            )
        }
    }

    fun createCrew(name: String, tagline: String, area: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val id = crewRepository.create(name, tagline, area)
            if (id.isNotEmpty()) onCreated(id)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CommunityViewModel(
                    ServiceLocator.crewRepository,
                    ServiceLocator.communityRepository,
                    ServiceLocator.rankingRepository,
                    ServiceLocator.rewardRepository,
                    ServiceLocator.sneakerRepository,
                )
            }
        }
    }
}
