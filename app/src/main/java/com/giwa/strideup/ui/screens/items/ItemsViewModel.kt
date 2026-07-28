package com.giwa.strideup.ui.screens.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.repo.ActiveBoost
import com.giwa.strideup.data.repo.BoostRepository
import com.giwa.strideup.data.repo.PurchaseError
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.SneakerRepository
import com.giwa.strideup.domain.BoostType
import com.giwa.strideup.domain.Sneaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 화면에 한 번만 보여줄 메시지 */
sealed interface ItemsMessage {
    data object NotEnoughBalance : ItemsMessage
    data object BoostAlreadyActive : ItemsMessage
    data object BoostBought : ItemsMessage
    data object MaxLevel : ItemsMessage
    data class Upgraded(val name: String) : ItemsMessage
    data class Equipped(val name: String) : ItemsMessage
}

class ItemsViewModel(
    private val sneakerRepository: SneakerRepository,
    private val boostRepository: BoostRepository,
    rewardRepository: RewardRepository,
) : ViewModel() {

    val inventory: StateFlow<List<Sneaker>> = sneakerRepository.inventory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val equipped: StateFlow<Sneaker?> = sneakerRepository.equipped
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val balance: StateFlow<Double> = rewardRepository.balance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val activeBoosts: StateFlow<List<ActiveBoost>> = boostRepository.active
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val collectionProgress: StateFlow<Pair<Int, Int>> = sneakerRepository.collectionProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0 to 0)

    /** 민팅 성공 시 결과 다이얼로그용 */
    val mintResult = MutableStateFlow<Sneaker?>(null)

    val message = MutableStateFlow<ItemsMessage?>(null)

    fun equip(id: Long) {
        viewModelScope.launch {
            sneakerRepository.equip(id)
            val name = inventory.value.firstOrNull { it.id == id }?.displayName.orEmpty()
            message.value = ItemsMessage.Equipped(name)
        }
    }

    fun upgrade(id: Long) {
        viewModelScope.launch {
            val target = inventory.value.firstOrNull { it.id == id }
            if (target != null && !target.canUpgrade) {
                message.value = ItemsMessage.MaxLevel
                return@launch
            }
            val result = sneakerRepository.upgrade(id)
            message.value = if (result != null) {
                ItemsMessage.Upgraded(result.displayName)
            } else {
                ItemsMessage.NotEnoughBalance
            }
        }
    }

    fun mint() {
        viewModelScope.launch {
            val minted = sneakerRepository.mint()
            if (minted == null) {
                message.value = ItemsMessage.NotEnoughBalance
            } else {
                mintResult.value = minted
            }
        }
    }

    fun buyBoost(type: BoostType) {
        viewModelScope.launch {
            message.value = when (boostRepository.purchase(type)) {
                null -> ItemsMessage.BoostBought
                PurchaseError.NOT_ENOUGH_BALANCE -> ItemsMessage.NotEnoughBalance
                PurchaseError.ALREADY_ACTIVE -> ItemsMessage.BoostAlreadyActive
            }
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    fun dismissMintResult() {
        mintResult.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ItemsViewModel(
                    ServiceLocator.sneakerRepository,
                    ServiceLocator.boostRepository,
                    ServiceLocator.rewardRepository,
                )
            }
        }
    }
}
