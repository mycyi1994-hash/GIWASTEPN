package com.giwa.strideup.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.repo.CommunityRepository
import com.giwa.strideup.data.repo.Crew
import com.giwa.strideup.data.repo.CrewRepository
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.StepRepository
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
    stepRepository: StepRepository,
    rewardRepository: RewardRepository,
) : ViewModel() {

    val crews: StateFlow<List<Crew>> = crewRepository.crews

    val joinedCrewIds: StateFlow<Set<String>> = crewRepository.joinedCrewIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val boardPosts: StateFlow<List<Post>> = communityRepository.boardPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allPosts: StateFlow<List<Post>> = communityRepository.posts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 이번 주(최근 7일) 걸음 합계 — 랭킹 산정에 쓴다 */
    val weeklySteps: StateFlow<Long> = stepRepository.observeWeek()
        .map { days -> days.sumOf { it.steps.toLong() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val balance: StateFlow<Double> = rewardRepository.balance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** 선택된 세그먼트 — 탭을 오갔다 와도 유지된다 */
    val tab = MutableStateFlow(CommunityTab.BOARD)

    /** 게시판 카테고리 필터. null이면 전체 */
    val categoryFilter = MutableStateFlow<PostCategory?>(null)

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
                    ServiceLocator.stepRepository,
                    ServiceLocator.rewardRepository,
                )
            }
        }
    }
}
