package com.giwa.strideup.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.ui.components.AmbientBackdrop
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.screens.home.HomeScreen
import com.giwa.strideup.ui.screens.profile.ProfileScreen
import com.giwa.strideup.ui.screens.rewards.RewardsScreen
import com.giwa.strideup.ui.screens.walk.WalkScreen
import com.giwa.strideup.ui.theme.Border
import com.giwa.strideup.ui.theme.CardWhite
import com.giwa.strideup.ui.theme.Coral
import com.giwa.strideup.ui.theme.Ink
import com.giwa.strideup.ui.theme.SunsetPlate
import com.giwa.strideup.ui.theme.TaupeLight

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "홈", Icons.Filled.Home)
    data object Walk : Screen("walk", "워킹", Icons.AutoMirrored.Filled.DirectionsWalk)
    data object Rewards : Screen("rewards", "리워드", Icons.Filled.WorkspacePremium)
    data object Profile : Screen("profile", "프로필", Icons.Filled.Person)
}

private val bottomTabs = listOf(Screen.Home, Screen.Walk, Screen.Rewards, Screen.Profile)

@Composable
fun StrideUpRoot() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (StepPermissions.hasActivityRecognition(context)) {
            ServiceLocator.stepRepository.startTracking()
        }
    }

    // 앱 첫 진입 시 필요한 권한을 한 번 요청한다.
    LaunchedEffect(Unit) {
        val missing = StepPermissions.missing(context)
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing)
        }
    }

    val navController = rememberNavController()

    Box(Modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = { FloatingDock(navController) },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(onStartWalk = { navController.switchTab(Screen.Walk) })
                }
                composable(Screen.Walk.route) { WalkScreen() }
                composable(Screen.Rewards.route) { RewardsScreen() }
                composable(Screen.Profile.route) { ProfileScreen() }
            }
        }
    }
}

/** 탭 전환 — 백스택을 쌓지 않고 각 탭의 상태를 보존한다. */
private fun NavHostController.switchTab(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** 떠 있는 pill 독 내비게이션. 활성 탭은 코럴, 인디케이터는 작은 선셋 점 하나. */
@Composable
private fun FloatingDock(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val shape = RoundedCornerShape(34.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = shape,
                    spotColor = Ink.copy(alpha = 0.22f),
                    ambientColor = Ink.copy(alpha = 0.10f),
                )
                .background(CardWhite, shape)
                .border(1.dp, Border, shape)
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomTabs.forEach { screen ->
                DockTab(
                    screen = screen,
                    selected = currentRoute == screen.route,
                    onClick = { navController.switchTab(screen) },
                )
            }
        }
    }
}

@Composable
private fun DockTab(screen: Screen, selected: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(
        targetValue = if (selected) Coral else TaupeLight,
        label = "dockTabTint",
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        label = "dockTabDot",
    )
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .quietClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = screen.label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = screen.label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.4.sp,
        )
        Box(
            modifier = Modifier
                .size(4.dp)
                .alpha(dotAlpha)
                .background(SunsetPlate, CircleShape),
        )
    }
}
