package com.giwa.strideup.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.ui.components.IconBadge
import com.giwa.strideup.ui.components.StatItem
import com.giwa.strideup.ui.components.StrideCard
import com.giwa.strideup.ui.theme.NeonAmber
import com.giwa.strideup.ui.theme.NeonCyan
import com.giwa.strideup.ui.theme.NeonGreen
import com.giwa.strideup.ui.theme.NeonPurple

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("프로필", style = MaterialTheme.typography.titleLarge)
        }

        item {
            StrideCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    IconBadge(icon = Icons.Filled.Person, tint = NeonCyan, size = 56.dp)
                    Column {
                        Text("StrideUp 러너", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "GIWA STEPN 프로젝트 · M2E 데모",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            StrideCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("내 스니커즈", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Lv.${state.sneakerLevel}",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonGreen,
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(
                            icon = Icons.Filled.TrendingUp,
                            tint = NeonGreen,
                            label = "적립 배율",
                            value = "x%.2f".format(state.multiplier),
                        )
                        StatItem(
                            icon = Icons.Filled.Bolt,
                            tint = NeonAmber,
                            label = "최대 에너지",
                            value = "%.0f".format(state.maxEnergy),
                        )
                        StatItem(
                            icon = Icons.Filled.LocalFireDepartment,
                            tint = NeonPurple,
                            label = "스트릭",
                            value = "${state.streak}일",
                        )
                    }
                    Button(
                        onClick = { viewModel.upgradeSneaker() },
                        enabled = state.balance >= state.upgradeCost,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("업그레이드 (%.0f SUP)".format(state.upgradeCost))
                    }
                    Text(
                        "레벨이 오르면 적립 배율 +0.15, 최대 에너지 +2가 적용돼요. " +
                            "보유 SUP: %,.2f".format(state.balance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            var sliderValue by remember(state.goal) { mutableFloatStateOf(state.goal.toFloat()) }
            StrideCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = NeonCyan,
                            )
                            Text("일일 목표", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "%,d 걸음".format(sliderValue.toInt()),
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonCyan,
                        )
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { viewModel.setGoal(sliderValue.toInt()) },
                        valueRange = UserPrefs.MIN_GOAL.toFloat()..UserPrefs.MAX_GOAL.toFloat(),
                        steps = (UserPrefs.MAX_GOAL - UserPrefs.MIN_GOAL) / 500 - 1,
                    )
                    Text(
                        "목표를 달성하면 매일 보너스 SUP가 지급되고, 연속 달성 시 보너스가 커져요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            StrideCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("앱 정보", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "StrideUp v1.0.0 — Move to Earn 데모\n" +
                            "걷기 → SUP 포인트 적립 → GIWA 체인 온체인 전환(예정)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
