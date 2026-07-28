package com.giwa.strideup.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.giwa.strideup.R
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.NightCanvas
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.screens.community.CommunityScreen
import com.giwa.strideup.ui.screens.events.EventsScreen
import com.giwa.strideup.ui.screens.home.HomeScreen
import com.giwa.strideup.ui.screens.items.ItemsScreen
import com.giwa.strideup.ui.screens.profile.ProfileScreen
import com.giwa.strideup.ui.screens.rewards.WalletScreen
import com.giwa.strideup.ui.screens.walk.RunScreen
import com.giwa.strideup.ui.theme.Carbon
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Volt

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Home : Screen("home", R.string.tab_home, Icons.Filled.Hexagon)
    data object Community : Screen("community", R.string.tab_community, Icons.Filled.Groups)
    data object Items : Screen("items", R.string.tab_items, Icons.Filled.ShoppingBag)
    data object Events : Screen("events", R.string.tab_events, Icons.Filled.Event)
    data object Profile : Screen("profile", R.string.tab_profile, Icons.Filled.Person)
}

private val bottomTabs = listOf(Screen.Home, Screen.Community, Screen.Items, Screen.Events, Screen.Profile)

const val ROUTE_RUN = "run"
const val ROUTE_WALLET = "wallet"

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
        NightCanvas(Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = { VoltNavBar(navController) },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onStartRun = { navController.navigate(ROUTE_RUN) },
                        onOpenWallet = { navController.navigate(ROUTE_WALLET) },
                        onOpenEvents = { navController.switchTab(Screen.Events) },
                        onOpenProfile = { navController.switchTab(Screen.Profile) },
                        onOpenItems = { navController.switchTab(Screen.Items) },
                    )
                }
                composable(Screen.Community.route) { CommunityScreen() }
                composable(Screen.Items.route) { ItemsScreen() }
                composable(Screen.Events.route) { EventsScreen() }
                composable(Screen.Profile.route) {
                    ProfileScreen(onOpenWallet = { navController.navigate(ROUTE_WALLET) })
                }
                composable(ROUTE_RUN) { RunScreen(onBack = { navController.popBackStack() }) }
                composable(ROUTE_WALLET) { WalletScreen(onBack = { navController.popBackStack() }) }
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

/** 딥 블랙 하단 내비게이션 — 활성 탭은 볼트 + 라벨 아래 점. */
@Composable
private fun VoltNavBar(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Carbon.copy(alpha = 0.97f), Night)),
            ),
    ) {
        HairlineDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomTabs.forEach { screen ->
                NavTab(
                    screen = screen,
                    selected = currentRoute == screen.route,
                    onClick = { navController.switchTab(screen) },
                )
            }
        }
    }
}

@Composable
private fun NavTab(screen: Screen, selected: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(
        targetValue = if (selected) Volt else Slate,
        label = "navTabTint",
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        label = "navTabDot",
    )
    Column(
        modifier = Modifier
            .quietClickable(onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = stringResource(screen.labelRes),
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(screen.labelRes),
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.3.sp,
        )
        Box(
            modifier = Modifier
                .size(4.dp)
                .alpha(dotAlpha)
                .background(Volt, CircleShape),
        )
    }
}
