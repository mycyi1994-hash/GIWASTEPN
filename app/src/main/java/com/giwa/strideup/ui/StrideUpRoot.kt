package com.giwa.strideup.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.giwa.strideup.R
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.NightCanvas
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.guide.GuideOverlay
import com.giwa.strideup.ui.guide.GuideTour
import com.giwa.strideup.ui.screens.community.CommunityScreen
import com.giwa.strideup.ui.screens.community.CrewBoardScreen
import com.giwa.strideup.ui.screens.community.CrewCreateScreen
import com.giwa.strideup.ui.screens.community.PartyLobbyScreen
import com.giwa.strideup.ui.screens.community.PostComposeScreen
import com.giwa.strideup.ui.screens.community.RankingScreen
import com.giwa.strideup.ui.screens.events.EventsScreen
import com.giwa.strideup.ui.screens.home.HomeScreen
import com.giwa.strideup.ui.screens.login.LoginScreen
import com.giwa.strideup.ui.screens.items.ItemsScreen
import com.giwa.strideup.ui.screens.items.SneakerDetailScreen
import com.giwa.strideup.ui.screens.notifications.NotificationsScreen
import com.giwa.strideup.ui.screens.profile.AchievementsScreen
import com.giwa.strideup.ui.screens.profile.AnalyticsScreen
import com.giwa.strideup.ui.screens.profile.ProfileScreen
import com.giwa.strideup.ui.screens.rewards.WalletScreen
import com.giwa.strideup.ui.screens.settings.ConnectedAccountsScreen
import com.giwa.strideup.ui.screens.settings.LanguageScreen
import com.giwa.strideup.ui.screens.settings.NotificationSettingsScreen
import com.giwa.strideup.ui.screens.settings.PrivacyScreen
import com.giwa.strideup.ui.screens.settings.SupportScreen
import com.giwa.strideup.ui.screens.splash.SplashScreen
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
private val tabRoutes = bottomTabs.map { it.route }.toSet()

object Routes {
    const val RUN = "run"
    const val WALLET = "wallet"
    const val NOTIFICATIONS = "notifications"
    const val ACHIEVEMENTS = "achievements"
    const val ANALYTICS = "analytics"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_PRIVACY = "settings/privacy"
    const val SETTINGS_SUPPORT = "settings/support"
    const val SETTINGS_CONNECTED = "settings/connected"
    const val SETTINGS_LANGUAGE = "settings/language"
    const val SNEAKER = "sneaker/{id}"
    const val LOBBY = "lobby/{crewId}"
    const val RANKING = "ranking"
    const val CREW_CREATE = "crew/create"
    const val CREW_BOARD = "crew/board/{crewId}"
    const val POST_COMPOSE = "post/compose/{crewId}"

    fun sneaker(id: Long) = "sneaker/$id"
    fun lobby(crewId: String) = "lobby/$crewId"
    fun crewBoard(crewId: String) = "crew/board/$crewId"

    /** crewId가 비어 있으면 전체 게시판에 쓰는 글 */
    fun postCompose(crewId: String) = "post/compose/${crewId.ifBlank { NO_CREW }}"

    const val NO_CREW = "_"
}

@Composable
fun StrideUpRoot() {
    var ready by rememberSaveable { mutableStateOf(false) }
    // 로그인/가이드는 DataStore 값이 로드될 때까지 null — 스플래시가 그 시간을 가려준다.
    val loginMethod by ServiceLocator.userPrefs.loginMethod
        .collectAsState(initial = null)
    val guideSeen by ServiceLocator.userPrefs.guideSeen
        .collectAsState(initial = null)

    Box(Modifier.fillMaxSize()) {
        NightCanvas(Modifier.fillMaxSize())

        when {
            !ready || loginMethod == null || guideSeen == null ->
                SplashScreen(onReady = { ready = true })

            loginMethod!!.isEmpty() -> LoginScreen(onDone = {})

            else -> MainScaffold(startTour = guideSeen == false)
        }
    }
}

@Composable
private fun MainScaffold(startTour: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 첫 실행이면 화면이 자리를 잡은 뒤 스포트라이트 투어를 시작한다
    LaunchedEffect(startTour) {
        if (startTour) {
            kotlinx.coroutines.delay(450)
            GuideTour.start()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (StepPermissions.hasActivityRecognition(context)) {
            ServiceLocator.stepRepository.startTracking()
        }
    }

    LaunchedEffect(Unit) {
        val missing = StepPermissions.missing(context)
        if (missing.isNotEmpty()) permissionLauncher.launch(missing)
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in tabRoutes

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                VoltNavBar(navController, currentRoute)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onStartRun = { navController.navigate(Routes.RUN) },
                    onOpenWallet = { navController.navigate(Routes.WALLET) },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenProfile = { navController.navigate(Routes.ANALYTICS) },
                    onOpenItems = { navController.switchTab(Screen.Items) },
                )
            }
            composable(Screen.Community.route) {
                CommunityScreen(
                    onOpenLobby = { crewId -> navController.navigate(Routes.lobby(crewId)) },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenRanking = { navController.navigate(Routes.RANKING) },
                    onOpenCrew = { crewId -> navController.navigate(Routes.crewBoard(crewId)) },
                    onCreateCrew = { navController.navigate(Routes.CREW_CREATE) },
                    onWritePost = { crewId -> navController.navigate(Routes.postCompose(crewId)) },
                )
            }
            composable(Screen.Items.route) {
                ItemsScreen(onOpenSneaker = { id -> navController.navigate(Routes.sneaker(id)) })
            }
            composable(Screen.Events.route) {
                EventsScreen(onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) })
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onOpenGuide = {
                        navController.switchTab(Screen.Home)
                        GuideTour.start()
                    },
                    onOpenWallet = { navController.navigate(Routes.WALLET) },
                    onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                    onOpenAnalytics = { navController.navigate(Routes.ANALYTICS) },
                    onOpenNotificationSettings = { navController.navigate(Routes.SETTINGS_NOTIFICATIONS) },
                    onOpenPrivacy = { navController.navigate(Routes.SETTINGS_PRIVACY) },
                    onOpenSupport = { navController.navigate(Routes.SETTINGS_SUPPORT) },
                    onOpenConnected = { navController.navigate(Routes.SETTINGS_CONNECTED) },
                    onOpenLanguage = { navController.navigate(Routes.SETTINGS_LANGUAGE) },
                    onOpenItems = { navController.switchTab(Screen.Items) },
                )
            }

            composable(Routes.RUN) { RunScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.WALLET) { WalletScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLobby = { crewId -> navController.navigate(Routes.lobby(crewId)) },
                )
            }
            composable(Routes.ACHIEVEMENTS) {
                AchievementsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ANALYTICS) {
                AnalyticsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_NOTIFICATIONS) {
                NotificationSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_PRIVACY) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_SUPPORT) {
                SupportScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_CONNECTED) {
                ConnectedAccountsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_LANGUAGE) {
                LanguageScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.SNEAKER,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                SneakerDetailScreen(
                    sneakerId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.LOBBY,
                arguments = listOf(navArgument("crewId") { type = NavType.StringType }),
            ) { entry ->
                PartyLobbyScreen(
                    crewId = entry.arguments?.getString("crewId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onRunStarted = { navController.navigate(Routes.RUN) },
                )
            }
            composable(Routes.RANKING) {
                RankingScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CREW_CREATE) {
                CrewCreateScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { crewId ->
                        navController.popBackStack()
                        navController.navigate(Routes.crewBoard(crewId))
                    },
                )
            }
            composable(
                route = Routes.CREW_BOARD,
                arguments = listOf(navArgument("crewId") { type = NavType.StringType }),
            ) { entry ->
                CrewBoardScreen(
                    crewId = entry.arguments?.getString("crewId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenLobby = { crewId -> navController.navigate(Routes.lobby(crewId)) },
                    onWritePost = { crewId -> navController.navigate(Routes.postCompose(crewId)) },
                )
            }
            composable(
                route = Routes.POST_COMPOSE,
                arguments = listOf(navArgument("crewId") { type = NavType.StringType }),
            ) { entry ->
                val raw = entry.arguments?.getString("crewId").orEmpty()
                val crewId = if (raw == Routes.NO_CREW) "" else raw
                PostComposeScreen(
                    crewId = crewId,
                    crewName = ServiceLocator.crewRepository.crewOf(crewId)?.name.orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }

    // 스포트라이트 가이드 오버레이 — 하단 바까지 덮는다
    if (GuideTour.active) {
        GuideOverlay(
            onSwitchTab = { route ->
                bottomTabs.firstOrNull { it.route == route }?.let { navController.switchTab(it) }
            },
            onFinished = {
                scope.launch { ServiceLocator.userPrefs.setGuideSeen() }
            },
        )
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

@Composable
private fun VoltNavBar(navController: NavHostController, currentRoute: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Carbon.copy(alpha = 0.97f), Night))),
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
