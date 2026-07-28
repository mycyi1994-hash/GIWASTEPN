package com.giwa.strideup.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.repo.Crew
import com.giwa.strideup.data.repo.CrewRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CommunityViewModel(private val crewRepository: CrewRepository) : ViewModel() {

    val crews: List<Crew> = crewRepository.crews

    val joinedCrewIds: StateFlow<Set<String>> = crewRepository.joinedCrewIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleJoin(crewId: String) {
        viewModelScope.launch {
            if (joinedCrewIds.value.contains(crewId)) {
                crewRepository.leave(crewId)
            } else {
                crewRepository.join(crewId)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { CommunityViewModel(ServiceLocator.crewRepository) }
        }
    }
}
