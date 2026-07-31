package com.giwa.strideup.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.data.repo.SneakerRepository
import com.giwa.strideup.data.local.WalkSessionDao
import com.giwa.strideup.domain.Faction
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.repo.CommunityRepository
import com.giwa.strideup.data.repo.Crew
import com.giwa.strideup.data.repo.CrewRepository
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.domain.CommentThread
import com.giwa.strideup.domain.Post
import com.giwa.strideup.domain.PostCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 커뮤니티 최상단 세그먼트 */
enum class CommunityTab { BOARD, CREW }

class CommunityViewModel(
    private val crewRepository: CrewRepository,
    private val communityRepository: CommunityRepository,
    rewardRepository: RewardRepository,
    userPrefs: UserPrefs,
    sneakerRepository: SneakerRepository,
    walkSessionDao: WalkSessionDao,
) : ViewModel() {

    val crews: StateFlow<List<Crew>> = crewRepository.crews

    val joinedCrewIds: StateFlow<Set<String>> = crewRepository.joinedCrewIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val boardPosts: StateFlow<List<Post>> = communityRepository.boardPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allPosts: StateFlow<List<Post>> = communityRepository.posts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val balance: StateFlow<Double> = rewardRepository.balance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** 역대 최고 속도(km/h) — 러닝 판정을 통과한 구간에서만 기록된다 */
    val topSpeedKmh: StateFlow<Double> = userPrefs.topSpeedKmh
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** 러닝에 쓴 누적 시간(초). 무효 판정된 세션은 0초로 기록돼 여기 안 들어온다. */
    val totalActiveSec: StateFlow<Long> = walkSessionDao.observeDurationSince(0L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** 종족별 내 누적 거리(km) */
    val factionKm: StateFlow<Map<Faction, Double>> = userPrefs.factionKm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** 지금 신고 있는 신발의 종족 — 종족 랭킹에서 "우리 편"을 표시한다 */
    val myFaction: StateFlow<Faction?> = sneakerRepository.equipped
        .map { it?.faction }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 선택된 세그먼트 — 탭을 오갔다 와도 유지된다 */
    val tab = MutableStateFlow(CommunityTab.BOARD)

    /** 게시판 카테고리 필터. null이면 전체 */
    val categoryFilter = MutableStateFlow<PostCategory?>(null)

    /** 댓글 창을 열어 둔 글의 id. null이면 닫혀 있다 */
    val openCommentsFor = MutableStateFlow<Long?>(null)

    fun openComments(postId: Long) {
        openCommentsFor.value = postId
    }

    fun closeComments() {
        openCommentsFor.value = null
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

    fun selectCategory(next: PostCategory?) {
        categoryFilter.value = next
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
                    ServiceLocator.rewardRepository,
                    ServiceLocator.userPrefs,
                    ServiceLocator.sneakerRepository,
                    ServiceLocator.database.walkSessionDao(),
                )
            }
        }
    }
}
