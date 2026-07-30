package com.giwa.strideup.ui.screens.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.data.repo.CourseRepository
import com.giwa.strideup.domain.GeoPoint
import com.giwa.strideup.domain.RunCourse
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseHubViewModel(
    private val courseRepository: CourseRepository,
    prefs: UserPrefs,
) : ViewModel() {

    val courses: StateFlow<List<RunCourse>> = courseRepository.courses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedId: StateFlow<Long> = prefs.selectedCourseId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1L)

    fun select(id: Long) {
        viewModelScope.launch {
            // 같은 코스를 다시 누르면 선택 해제
            if (selectedId.value == id) courseRepository.clearSelection()
            else courseRepository.select(id)
        }
    }

    fun toggleLike(id: Long) {
        viewModelScope.launch { courseRepository.toggleLike(id) }
    }

    fun setShared(id: Long, shared: Boolean) {
        viewModelScope.launch { courseRepository.setShared(id, shared) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { courseRepository.delete(id) }
    }

    fun create(name: String, area: String, track: List<GeoPoint>, shared: Boolean) {
        viewModelScope.launch {
            val id = courseRepository.create(name, area, track, shared)
            if (id != null) courseRepository.select(id)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CourseHubViewModel(
                    ServiceLocator.courseRepository,
                    ServiceLocator.userPrefs,
                )
            }
        }
    }
}
