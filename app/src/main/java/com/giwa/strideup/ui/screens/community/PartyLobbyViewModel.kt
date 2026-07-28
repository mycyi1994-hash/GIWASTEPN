package com.giwa.strideup.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.repo.CrewRepository
import com.giwa.strideup.data.repo.PartyState
import kotlinx.coroutines.flow.StateFlow

class PartyLobbyViewModel(private val crewRepository: CrewRepository) : ViewModel() {

    val party: StateFlow<PartyState> = crewRepository.party

    fun openLobby(crewId: String) = crewRepository.openLobby(crewId)

    fun setReady(ready: Boolean) = crewRepository.setMyReady(ready)

    fun leaveLobby() = crewRepository.leaveLobby()

    fun dismissResult() = crewRepository.dismissResult()

    companion object {
        val Factory = viewModelFactory {
            initializer { PartyLobbyViewModel(ServiceLocator.crewRepository) }
        }
    }
}
