package com.stepup.android.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.PartyState
import kotlinx.coroutines.flow.StateFlow

class PartyLobbyViewModel(private val crewRepository: CrewRepository) : ViewModel() {

    val party: StateFlow<PartyState> = crewRepository.party

    fun openLobby(crewId: String) = crewRepository.openLobby(crewId)

    /** 번개러닝 로비. 크루 로비와 같은 판을 쓴다. */
    fun openFlashLobby(postId: Long, title: String, others: Int) =
        crewRepository.openFlashLobby(postId, title, others)

    fun setReady(ready: Boolean) = crewRepository.setMyReady(ready)

    fun leaveLobby() = crewRepository.leaveLobby()

    fun dismissResult() = crewRepository.dismissResult()

    // ── 파티장 권한 ──

    fun startParty() = crewRepository.startParty()

    fun kick(memberId: String) = crewRepository.kick(memberId)

    fun invite(name: String) = crewRepository.invite(name)

    fun inviteCandidates(): List<String> = crewRepository.inviteCandidates()

    companion object {
        val Factory = viewModelFactory {
            initializer { PartyLobbyViewModel(ServiceLocator.crewRepository) }
        }
    }
}
