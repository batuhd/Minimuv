package com.sinop.minimuv.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sinop.minimuv.R
import com.sinop.minimuv.core.NotificationHelper
import com.sinop.minimuv.core.PartnerEventsRuntime
import com.sinop.minimuv.core.SupabaseProvider
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.ui.components.MinimuvButton
import com.sinop.minimuv.ui.screens.achievements.AchievementsScreen
import com.sinop.minimuv.ui.screens.add.AddScreen
import com.sinop.minimuv.ui.screens.detail.DetailScreen
import com.sinop.minimuv.ui.screens.detail.DetailViewModel
import com.sinop.minimuv.ui.screens.list.ListScreen
import com.sinop.minimuv.ui.screens.list.ListViewModel
import com.sinop.minimuv.ui.screens.settings.SettingsScreen
import com.sinop.minimuv.ui.screens.setup.ProfileSelectScreen
import com.sinop.minimuv.ui.screens.setup.SupabaseSetupScreen
import com.sinop.minimuv.ui.screens.stats.HeatmapScreen
import com.sinop.minimuv.ui.screens.stats.WrappedScreen
import com.sinop.minimuv.ui.screens.wheel.WheelScreen
import com.sinop.minimuv.ui.theme.MinimuvTheme
import com.sinop.minimuv.ui.theme.ThemeAccent
import com.sinop.minimuv.ui.theme.icons.MinimuvIcons
import kotlinx.coroutines.launch

sealed class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object List : BottomTab("list", "Liste", MinimuvIcons.ListIcon)
    data object Wheel : BottomTab("wheel", "Çark", MinimuvIcons.WheelIcon)
    data object Badges : BottomTab("badges", "Rozetler", MinimuvIcons.BadgeIcon)
    data object Profile : BottomTab("profile", "Profil", MinimuvIcons.ProfileIcon)
    data object Settings : BottomTab("settings", "Ayarlar", MinimuvIcons.SettingsIcon)
}

private val TABS = listOf(BottomTab.List, BottomTab.Wheel, BottomTab.Badges, BottomTab.Profile, BottomTab.Settings)

@Composable
fun MinimuvApp() {
    val context = LocalContext.current.applicationContext
    val settings = remember { SettingsStore(context) }
    val prefs by settings.rawPrefs.collectAsState(initial = null)
    val url = settings.urlFrom(prefs)
    val key = settings.keyFrom(prefs)
    val profileId = settings.profileFrom(prefs)
    val accentName = prefs?.get(com.sinop.minimuv.data.SettingsStore.KEY_THEME_ACCENT)
    val accent = ThemeAccent.entries.firstOrNull { it.name == accentName } ?: ThemeAccent.BLUE

    MinimuvTheme(accent = accent) {
        when {
            // DataStore'un İLK emisyonunu bekliyoruz — boş veri ile karışmasın
            prefs == null -> Splash()
            url.isNullOrBlank() || key.isNullOrBlank() -> {
                SupabaseSetupScreen(settings = settings, onDone = {})
            }
            else -> {
                remember(url, key) {
                    SupabaseProvider.configure(url!!, key!!)
                }
                if (profileId.isNullOrBlank()) {
                    ProfileSelectScreen(settings = settings, onDone = {})
                } else {
                    MainApp(settings = settings, profileId = profileId)
                }
            }
        }
    }
}

@Composable
private fun Splash() {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.minimuv_logo),
            contentDescription = null,
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(22.dp)),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Minimuv",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(settings: SettingsStore, profileId: String) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val context = LocalContext.current
    val appScope = rememberCoroutineScope()
    val savedListView by settings.listView.collectAsState(initial = null)

    LaunchedEffect(profileId) {
        NotificationHelper.ensureChannels(context)
        val activity = context as? android.app.Activity
        if (activity != null && Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101,
            )
        }
        // Uygulama açıkken realtime anlık bildirimler
        runCatching { PartnerEventsRuntime.start(context) }
        // Uygulama kapalıyken bile bildirimler: WorkManager ~15 dk'da bir kontrol eder
        com.sinop.minimuv.core.BackgroundNotifier.schedule(context)
        // FCM: cihaz token'ını sunucuya kaydet — kapalıyken anlık bildirimler için
        runCatching {
            com.sinop.minimuv.data.TokenRepository(context.applicationContext).saveToken()
        }
    }

    val onboardingDone by settings.onboardingDone.collectAsState(initial = false)
    if (!onboardingDone) {
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { scope.launch { settings.setOnboardingDone() } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🎬", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(6.dp))
                Text("Minimuv'a hoş geldiniz!", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                Text(
                    "📋 Liste: izlediklerimiz, puanlarımız, notlarımız.\n" +
                        "🎡 Çark: kararsız kaldığımızda karar versin.\n" +
                        "🔒 Ayrı ayrı izlerken bölüm notları, diğeri o bölüme gelene kadar kilitli kalır.\n" +
                        "🏅 Rozetler: sizin değil, ikinizin birlikte kazandığı başarılar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                MinimuvButton(
                    label = "Başlayalım 🎬",
                    onClick = { scope.launch { settings.setOnboardingDone() } },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            val isMainTab = TABS.any { tab ->
                currentDestination?.hierarchy?.any { it.route == tab.route } == true
            }
            if (isMainTab) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    TABS.forEach { tab ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.padding(bottom = 2.dp),
                                )
                            },
                            label = { Text(tab.label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.sinop.minimuv.ui.theme.onColorFor(MaterialTheme.colorScheme.primary),
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentDestination?.hierarchy?.any { it.route == BottomTab.List.route } == true) {
                FloatingActionButton(
                    onClick = { navController.navigate("add") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = com.sinop.minimuv.ui.theme.onColorFor(MaterialTheme.colorScheme.primary),
                ) {
                    Text("+", fontSize = 26.sp)
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.List.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(BottomTab.List.route) {
                val vm: ListViewModel = viewModel()
                ListScreen(
                    vm = vm,
                    viewMode = com.sinop.minimuv.ui.screens.list.ViewMode.fromDb(savedListView),
                    onViewModeChange = { mode ->
                        appScope.launch { runCatching { settings.saveListView(mode.name) } }
                    },
                    onOpenTitle = { navController.navigate("detail/${it}") },
                    onEditTitle = { navController.navigate("detail/${it}?edit=true") },
                    onOpenPlanOrder = { navController.navigate("plan_order") },
                )
            }
            composable("plan_order") {
                com.sinop.minimuv.ui.screens.list.PlanOrderScreen(onBack = { navController.popBackStack() })
            }
            composable(BottomTab.Wheel.route) {
                WheelScreen(onOpenTitle = { navController.navigate("detail/${it}") })
            }
            composable(BottomTab.Badges.route) {
                AchievementsScreen()
            }
            composable(BottomTab.Profile.route) {
                com.sinop.minimuv.ui.screens.profile.ProfileScreen(
                    settings = settings,
                    onSwitchProfile = {
                        navController.navigate(BottomTab.List.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onOpenHeatmap = { navController.navigate("heatmap") },
                    onOpenWrapped = { navController.navigate("wrapped") },
                    onOpenTitle = { navController.navigate("detail/${it}") },
                )
            }
            composable(BottomTab.Settings.route) {
                SettingsScreen(
                    settings = settings,
                    onSwitchProfile = {
                        navController.navigate(BottomTab.List.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable("add") {
                AddScreen(
                    settings = settings,
                    onBack = { navController.popBackStack() },
                    onPicked = { navController.navigate("detail/draft") },
                )
            }
            composable(
                "detail/{titleId}?edit={edit}",
                arguments = listOf(
                    navArgument("edit") {
                        type = NavType.StringType
                        defaultValue = "false"
                    },
                ),
            ) { entry ->
                val titleId = entry.arguments?.getString("titleId") ?: return@composable
                val startInEdit = entry.arguments?.getString("edit") == "true"
                val vm: DetailViewModel = viewModel()
                DetailScreen(
                    titleId = titleId,
                    vm = vm,
                    profileId = profileId,
                    startInEdit = startInEdit,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack(BottomTab.List.route, inclusive = false)
                    },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable("heatmap") {
                HeatmapScreen(onBack = { navController.popBackStack() })
            }
            composable("wrapped") {
                WrappedScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
