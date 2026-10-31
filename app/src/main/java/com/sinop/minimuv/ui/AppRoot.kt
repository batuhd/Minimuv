package com.sinop.minimuv.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.sinop.minimuv.core.PersonSource
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.ui.components.BottomPillItem
import com.sinop.minimuv.ui.components.BottomPillNav
import com.sinop.minimuv.ui.components.InAppUpdateHost
import com.sinop.minimuv.ui.screens.add.AddScreen
import com.sinop.minimuv.ui.screens.detail.DetailScreen
import com.sinop.minimuv.ui.screens.detail.DetailViewModel
import com.sinop.minimuv.ui.screens.list.LibraryScreen
import com.sinop.minimuv.ui.screens.list.ListViewModel
import com.sinop.minimuv.ui.screens.person.PersonScreen
import com.sinop.minimuv.ui.screens.settings.SettingsScreen
import com.sinop.minimuv.ui.screens.setup.ProfileSelectScreen
import com.sinop.minimuv.ui.screens.setup.SupabaseSetupScreen
import com.sinop.minimuv.ui.screens.stats.HeatmapScreen
import com.sinop.minimuv.ui.screens.stats.WrappedScreen
import com.sinop.minimuv.ui.screens.studio.StudioScreen
import com.sinop.minimuv.ui.screens.wheel.WheelScreen
import com.sinop.minimuv.ui.theme.ThemeAccent
import com.sinop.minimuv.ui.theme.MinimuvTheme
import kotlinx.coroutines.launch

// Yeni alt sekmeler: Kütüphane / Ara / Çark / Aşk
private val TAB_ROUTES = listOf("list", "search", "wheel", "us")
private val PILLS = listOf(
    BottomPillItem("Kütüphane", com.sinop.minimuv.ui.components.DcPaths.LIBRARY),
    BottomPillItem("Ara", com.sinop.minimuv.ui.components.DcPaths.SEARCH),
    BottomPillItem("Çark", com.sinop.minimuv.ui.components.DcPaths.WHEEL),
    BottomPillItem("Aşk", com.sinop.minimuv.ui.components.DcPaths.HEART),
)

@Composable
fun MinimuvApp() {
    val context = LocalContext.current.applicationContext
    val settings = remember { SettingsStore(context) }
    val prefs by settings.rawPrefs.collectAsState(initial = null)
    val url = settings.urlFrom(prefs)
    val key = settings.keyFrom(prefs)
    val profileId = settings.profileFrom(prefs)
    val accentVanName by settings.themeAccentVan.collectAsState(initial = null)
    val accentSinopName by settings.themeAccentSinop.collectAsState(initial = null)
    val van = ThemeAccent.entries.firstOrNull { it.name == accentVanName } ?: ThemeAccent.BLUE
    val sinop = ThemeAccent.entries.firstOrNull { it.name == accentSinopName } ?: ThemeAccent.TEAL

    MinimuvTheme(van = van.primary, sinop = sinop.primary) {
        when {
            // DataStore'un İLK emisyonunu bekliyoruz — boş veri ile karışmasın
            prefs == null -> Splash()
            url.isNullOrBlank() || key.isNullOrBlank() -> {
                SupabaseSetupScreen(settings = settings, onDone = {})
            }
            else -> {
                // İlk frame güvenliği: alt ekranların ilk kompozisyonu client'ı bulabilmeli.
                SupabaseProvider.configure(url, key)
                LaunchedEffect(url, key) { SupabaseProvider.configure(url, key) }
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

@Composable
fun MainApp(settings: SettingsStore, profileId: String) {
    val navController = rememberNavController()
    val mainScope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val context = LocalContext.current
    val savedListView by settings.listView.collectAsState(initial = null)
    val savedDisplayLang by settings.displayLang.collectAsState(initial = null)
    val showNav = currentRoute in TAB_ROUTES
    var notificationAsked by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(profileId) {
        NotificationHelper.ensureChannels(context)
        val activity = context as? android.app.Activity
        if (!notificationAsked && activity != null && Build.VERSION.SDK_INT >= 33) {
            notificationAsked = true
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

    // Kişi ve stüdyo sayfalarındaki ortak "yapıma git" mantığı: kayıtlıysa detay,
    // değilse taslak ekranı. Lite sorgu yeterli — yalnızca eşleşme alanları kullanılır.
    val openWork: (com.sinop.minimuv.core.PersonWork) -> Unit = { work ->
        mainScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val existing = runCatching {
                com.sinop.minimuv.data.TitleRepository().getTitlesLite().firstOrNull { t ->
                    t.type == work.type &&
                        (t.externalId == work.externalId ||
                            t.title.equals(work.title, ignoreCase = true))
                }
            }.getOrNull()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (existing != null) {
                    navController.navigate("detail/${existing.id}")
                } else {
                    com.sinop.minimuv.ui.screens.add.DraftHolder.draft = com.sinop.minimuv.data.TitleDraft(
                        type = work.type,
                        externalId = work.externalId,
                        title = work.title,
                        titleEn = work.titleEn,
                        posterUrl = work.posterUrl,
                    )
                    navController.navigate("detail/draft") { launchSingleTop = true }
                }
            }
        }
    }

    // Tam ekran ekranların alt menüye binmesine izin ver, sekmelerde içermin altını boş bırak
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = "list",
            modifier = Modifier
                .fillMaxSize()
                .then(if (showNav) Modifier.padding(bottom = 120.dp) else Modifier),
        ) {
            composable("list") {
                val vm: ListViewModel = viewModel()
                val listScope = androidx.compose.runtime.rememberCoroutineScope()
                LibraryScreen(
                    vm = vm,
                    viewMode = com.sinop.minimuv.ui.screens.list.ViewMode.fromDb(savedListView),
                    displayLang = savedDisplayLang,
                    onOpenTitle = { navController.navigate("detail/${it}") },
                    onEditTitle = { navController.navigate("detail/${it}?edit=true") },
                    onViewModeChange = { m ->
                        listScope.launch { settings.saveListView(m.name) }
                    },
                    onOpenPlanOrder = { navController.navigate("plan_order") },
                )
            }
            composable("plan_order") {
                com.sinop.minimuv.ui.screens.list.PlanOrderScreen(onBack = { navController.popBackStack() })
            }
            composable("search") {
                AddScreen(
                    settings = settings,
                    onPicked = { navController.navigate("detail/draft") { launchSingleTop = true } },
                    onOpenPerson = { source, id ->
                        navController.navigate("person/${if (source == PersonSource.ANIME) "anime" else "tmdb"}/$id")
                    },
                    onOpenStudio = { type, id ->
                        navController.navigate("studio/${type.db}/$id")
                    },
                )
            }
            composable("wheel") {
                WheelScreen(onOpenTitle = { navController.navigate("detail/${it}") })
            }
            composable("us") {
                com.sinop.minimuv.ui.screens.profile.ProfileScreen(
                    settings = settings,
                    onSwitchProfile = {
                        mainScope.launch { settings.clearProfile() }
                        navController.navigate("list") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenWrapped = { navController.navigate("wrapped") },
                    onOpenBadges = { navController.navigate("achievements") },
                    onOpenSettings = { navController.navigate("settings") },
                )
            }
            composable("settings") {
                SettingsScreen(
                    settings = settings,
                    onSwitchProfile = {
                        mainScope.launch { settings.clearProfile() }
                        navController.navigate("list") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() },
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
                    displayLang = savedDisplayLang,
                    onOpenPerson = { source, id ->
                        navController.navigate("person/${if (source == PersonSource.ANIME) "anime" else "tmdb"}/$id")
                    },
                    onOpenStudio = { type, id ->
                        navController.navigate("studio/${type.db}/$id")
                    },
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack("list", inclusive = false)
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
            composable("achievements") {
                com.sinop.minimuv.ui.screens.achievements.AchievementsScreen(onBack = { navController.popBackStack() })
            }
            composable("person/{source}/{externalId}") { entry ->
                val sourceName = entry.arguments?.getString("source") ?: return@composable
                val externalId = entry.arguments?.getString("externalId") ?: return@composable
                PersonScreen(
                    source = if (sourceName == "anime") PersonSource.ANIME else PersonSource.TMDB,
                    externalId = externalId,
                    profileId = profileId,
                    displayLang = savedDisplayLang,
                    onBack = { navController.popBackStack() },
                    onOpenWork = openWork,
                )
            }
            composable("studio/{type}/{externalId}") { entry ->
                val typeName = entry.arguments?.getString("type") ?: return@composable
                val externalId = entry.arguments?.getString("externalId") ?: return@composable
                StudioScreen(
                    type = com.sinop.minimuv.data.ContentType.fromDb(typeName),
                    externalId = externalId,
                    profileId = profileId,
                    displayLang = savedDisplayLang,
                    onBack = { navController.popBackStack() },
                    onOpenWork = openWork,
                )
            }
        }

        if (showNav) {
            val selected = TAB_ROUTES.indexOfFirst { it == currentRoute }.coerceAtLeast(0)
            BottomPillNav(
                items = PILLS,
                selected = selected,
                onSelect = { idx ->
                    val route = TAB_ROUTES[idx]
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            )
        }
    }

    // Onboarding ana içerikten SONRA çizilir ki görünür olsun (eskiden arkada kalıyordu).
    if (!onboardingDone) {
        PendingOnboarding(settings = settings)
    }

    InAppUpdateHost()
}

/** Mevcut onboarding mantığı: ilk açılışta karşılama — tam tasarımı sonra. */
@Composable
private fun PendingOnboarding(settings: SettingsStore) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .padding(24.dp),
        ) {
            Text("Minimuv'a hoş geldiniz!", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(18.dp))
            com.sinop.minimuv.ui.components.DcButton(
                label = "Başlayalım",
                onClick = { scope.launch { settings.setOnboardingDone() } },
            )
        }
    }
}


