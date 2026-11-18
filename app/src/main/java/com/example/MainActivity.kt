package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.SoundEngine
import com.example.auth.authStateFlow
import com.example.auth.performSignOut
import com.example.data.AdminRepository
import com.example.data.GameRepository
import com.example.data.PlayerDataRepository
import com.example.data.PreferencesRepository
import com.example.data.ShopRepository
import com.example.data.WalletRepository
import com.example.data.model.DailyChallenge
import com.example.data.model.GameMode
import com.example.data.model.TimeChallenge
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.challenges.ChallengesScreen
import com.example.ui.components.StatusPill
import com.example.ui.game.GameplayScreen
import com.example.ui.home.HomeDashboardScreen
import com.example.ui.home.PlayModesAndLevelsScreen
import com.example.ui.rewards.CoinShopScreen
import com.example.ui.rewards.DailyRewardsScreen
import com.example.ui.social.HelpSupportScreen
import com.example.ui.social.LeaderboardScreen
import com.example.ui.social.ProfileScreen
import com.example.ui.social.SettingsScreen
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.PrismBlockTheme
import com.example.ui.theme.PrismCrimson
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismGold
import com.example.ui.theme.PrismViolet
import com.example.ui.theme.SurfaceBorder
import com.example.ui.wallet.WalletHubScreen
import com.example.ui.wallet.WalletTab
import com.example.viewmodel.AdminAccessState
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.GamePlayViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrismBlockTheme {
                PrismBlockAppRoot()
            }
        }
    }
}

private enum class BottomTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    PLAY("Play", Icons.Filled.GridView),
    CHALLENGES("Challenges", Icons.Filled.EmojiEvents),
    REWARDS("Rewards", Icons.Filled.Redeem),
    WALLET("Wallet", Icons.Filled.AccountBalanceWallet)
}

private enum class SecondaryScreen {
    NONE,
    COIN_SHOP,
    LEADERBOARD,
    PROFILE,
    SETTINGS,
    HELP_SUPPORT
}

private enum class FullScreenOverlay {
    NONE,
    GAMEPLAY,
    ADMIN
}

@Composable
private fun PrismBlockAppRoot() {
    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    if (currentUser == null) {
        // Unauthenticated users see ONLY AuthScreen — zero Firestore listeners created.
        AuthScreen(onAuthSuccess = {})
    } else {
        key(currentUser!!.uid) {
            AuthenticatedAppShell(currentUserId = currentUser!!.uid)
        }
    }
}

@Composable
private fun AuthenticatedAppShell(currentUserId: String) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    // Named Firestore database instance (strictly passing R.string.firestore_database_id)
    val db = remember(appContext) {
        FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
    }
    val soundEngine = remember { SoundEngine() }

    val appViewModel: AppViewModel = viewModel(
        key = "app_vm_$currentUserId",
        factory = viewModelFactory {
            initializer {
                AppViewModel(
                    gameRepository = GameRepository(db),
                    playerDataRepository = PlayerDataRepository(db),
                    walletRepository = WalletRepository(db),
                    shopRepository = ShopRepository(db),
                    adminRepository = AdminRepository(db),
                    preferencesRepository = PreferencesRepository(appContext),
                    soundEngine = soundEngine,
                    currentUserId = currentUserId
                )
            }
        }
    )

    val gamePlayViewModel: GamePlayViewModel = viewModel(
        key = "game_vm_$currentUserId",
        factory = viewModelFactory {
            initializer {
                GamePlayViewModel(soundEngine = appViewModel.soundEngine)
            }
        }
    )

    val user by appViewModel.userAccount.collectAsStateWithLifecycle()
    val settings by appViewModel.globalSettings.collectAsStateWithLifecycle()
    val adminAccess by appViewModel.adminAccess.collectAsStateWithLifecycle()
    val audioPrefs by appViewModel.audioPreferences.collectAsStateWithLifecycle()
    val statusMessage by appViewModel.statusMessage.collectAsStateWithLifecycle()
    val errorMessage by appViewModel.errorMessage.collectAsStateWithLifecycle()

    // Pause background music cleanly when app backgrounds
    DisposableEffect(lifecycleOwner, audioPrefs.musicEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> soundEngine.stopMusic()
                Lifecycle.Event.ON_RESUME -> if (audioPrefs.musicEnabled) soundEngine.startMusic()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            soundEngine.stopMusic()
        }
    }

    var activeTab by rememberSaveable { mutableStateOf(BottomTab.HOME) }
    var secondaryScreen by rememberSaveable { mutableStateOf(SecondaryScreen.NONE) }
    var fullScreenOverlay by rememberSaveable { mutableStateOf(FullScreenOverlay.NONE) }
    var walletStartTab by remember { mutableStateOf(WalletTab.OVERVIEW) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            appViewModel.clearMessages()
        }
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            appViewModel.clearMessages()
        }
    }

    fun launchGame(
        mode: GameMode,
        levelNumber: Long = 1L,
        dailyChallenge: DailyChallenge? = null,
        timeChallenge: TimeChallenge? = null
    ) {
        if (!settings.gameEnabled && !adminAccess.isAdmin) {
            appViewModel.showError(
                settings.maintenanceMessage.ifBlank { "Gameplay is currently paused by Admin for maintenance." }
            )
            return
        }
        val permLevel = (user?.currentLevel ?: 1L).coerceIn(1L, 100L)
        val permLines = (user?.currentLevelLines ?: 0L).coerceIn(0L, 1000L)
        val permCompleted = (user?.completedLevelsCount ?: 0L).coerceIn(0L, 100L)
        val effectiveLevelArg = if (mode == GameMode.LEVEL) levelNumber else permLevel
        gamePlayViewModel.startNewGame(
            mode = mode,
            levelNumber = effectiveLevelArg,
            levelExistingLines = permLines,
            permanentLevelNumber = permLevel,
            permanentLevelLines = permLines,
            permanentCompletedLevels = permCompleted,
            dailyChallenge = dailyChallenge,
            timeChallenge = timeChallenge
        )
        fullScreenOverlay = FullScreenOverlay.GAMEPLAY
    }

    fun handleSignOut() {
        soundEngine.stopMusic()
        performSignOut(credentialManager = credentialManager, scope = scope)
    }

    // Blocked Account Gate (Admins always retain Super Admin recovery)
    if (user?.blocked == true && !adminAccess.isSuperAdmin) {
        BlockedAccountScreen(onSignOut = { handleSignOut() })
        return
    }

    // Full-screen Gameplay (no overlapping bottom bar or drawer)
    if (fullScreenOverlay == FullScreenOverlay.GAMEPLAY) {
        GameplayScreen(
            appViewModel = appViewModel,
            gamePlayViewModel = gamePlayViewModel,
            onExitGame = { fullScreenOverlay = FullScreenOverlay.NONE }
        )
        return
    }

    // Full-screen Protected Admin Dashboard (AdminViewModel ONLY mounted when authorized!)
    if (fullScreenOverlay == FullScreenOverlay.ADMIN && adminAccess.isAdmin) {
        AdminOverlayHost(
            db = db,
            adminUid = currentUserId,
            appViewModel = appViewModel,
            adminAccess = adminAccess,
            onExitAdmin = { fullScreenOverlay = FullScreenOverlay.NONE }
        )
        return
    }

    // Back handling for secondary screens & open navigation drawer
    if (drawerState.isOpen) {
        BackHandler { scope.launch { drawerState.close() } }
    } else if (secondaryScreen != SecondaryScreen.NONE) {
        BackHandler { secondaryScreen = SecondaryScreen.NONE }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            PrismSideDrawerContent(
                displayName = user?.displayName ?: "Player",
                email = user?.email ?: "",
                adminAccess = adminAccess,
                onNavigate = { target ->
                    scope.launch { drawerState.close() }
                    target()
                },
                onOpenProfile = { secondaryScreen = SecondaryScreen.PROFILE },
                onOpenWallet = {
                    secondaryScreen = SecondaryScreen.NONE
                    walletStartTab = WalletTab.OVERVIEW
                    activeTab = BottomTab.WALLET
                },
                onOpenShop = { secondaryScreen = SecondaryScreen.COIN_SHOP },
                onOpenRewards = {
                    secondaryScreen = SecondaryScreen.NONE
                    activeTab = BottomTab.REWARDS
                },
                onOpenLeaderboard = { secondaryScreen = SecondaryScreen.LEADERBOARD },
                onOpenSettings = { secondaryScreen = SecondaryScreen.SETTINGS },
                onOpenHelp = { secondaryScreen = SecondaryScreen.HELP_SUPPORT },
                onOpenAdmin = {
                    if (adminAccess.isAdmin) fullScreenOverlay = FullScreenOverlay.ADMIN
                },
                onSignOut = { handleSignOut() }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MidnightBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = CardSurface,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            bottomBar = {
                if (secondaryScreen == SecondaryScreen.NONE) {
                    NavigationBar(
                        containerColor = DeepSurface,
                        tonalElevation = 8.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        BottomTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    secondaryScreen = SecondaryScreen.NONE
                                    activeTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrismCyan,
                                    selectedTextColor = PrismCyan,
                                    indicatorColor = PrismCyan.copy(alpha = 0.16f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("bottom_nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF130F2A), MidnightBg)))
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                ) {
                    AnimatedVisibility(visible = !settings.gameEnabled) {
                        Surface(
                            color = PrismGold.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, PrismGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = PrismGold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = settings.maintenanceMessage.ifBlank { "Maintenance mode active — gameplay temporarily paused by Admin." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (secondaryScreen) {
                            SecondaryScreen.COIN_SHOP -> CoinShopScreen(
                                appViewModel = appViewModel,
                                onBack = { secondaryScreen = SecondaryScreen.NONE }
                            )
                            SecondaryScreen.LEADERBOARD -> LeaderboardScreen(
                                appViewModel = appViewModel,
                                onBack = { secondaryScreen = SecondaryScreen.NONE }
                            )
                            SecondaryScreen.PROFILE -> ProfileScreen(
                                appViewModel = appViewModel,
                                onBack = { secondaryScreen = SecondaryScreen.NONE },
                                onSignOut = { handleSignOut() }
                            )
                            SecondaryScreen.SETTINGS -> SettingsScreen(
                                appViewModel = appViewModel,
                                onBack = { secondaryScreen = SecondaryScreen.NONE }
                            )
                            SecondaryScreen.HELP_SUPPORT -> HelpSupportScreen(
                                onBack = { secondaryScreen = SecondaryScreen.NONE }
                            )
                            SecondaryScreen.NONE -> when (activeTab) {
                                BottomTab.HOME -> HomeDashboardScreen(
                                    appViewModel = appViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onStartClassic = { launchGame(GameMode.CLASSIC) },
                                    onOpenLevelSelector = { activeTab = BottomTab.PLAY },
                                    onOpenChallenges = { activeTab = BottomTab.CHALLENGES },
                                    onOpenRewards = { activeTab = BottomTab.REWARDS },
                                    onOpenWallet = {
                                        walletStartTab = WalletTab.OVERVIEW
                                        activeTab = BottomTab.WALLET
                                    },
                                    onOpenShop = { secondaryScreen = SecondaryScreen.COIN_SHOP },
                                    onOpenLeaderboard = { secondaryScreen = SecondaryScreen.LEADERBOARD },
                                    onOpenAdminPanel = {
                                        if (adminAccess.isAdmin) fullScreenOverlay = FullScreenOverlay.ADMIN
                                    }
                                )
                                BottomTab.PLAY -> PlayModesAndLevelsScreen(
                                    appViewModel = appViewModel,
                                    onLaunchClassic = { launchGame(GameMode.CLASSIC) },
                                    onLaunchLevel = { lvl -> launchGame(GameMode.LEVEL, levelNumber = lvl) },
                                    onOpenChallenges = { activeTab = BottomTab.CHALLENGES }
                                )
                                BottomTab.CHALLENGES -> ChallengesScreen(
                                    appViewModel = appViewModel,
                                    onPlayDailyChallenge = { dc -> launchGame(GameMode.DAILY, dailyChallenge = dc) },
                                    onPlayTimeChallenge = { tc -> launchGame(GameMode.TIME, timeChallenge = tc) }
                                )
                                BottomTab.REWARDS -> DailyRewardsScreen(
                                    appViewModel = appViewModel,
                                    onOpenCoinShop = { secondaryScreen = SecondaryScreen.COIN_SHOP }
                                )
                                BottomTab.WALLET -> WalletHubScreen(
                                    appViewModel = appViewModel,
                                    initialTab = walletStartTab
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminOverlayHost(
    db: FirebaseFirestore,
    adminUid: String,
    appViewModel: AppViewModel,
    adminAccess: AdminAccessState,
    onExitAdmin: () -> Unit
) {
    val adminViewModel: AdminViewModel = viewModel(
        key = "admin_vm_$adminUid",
        factory = viewModelFactory {
            initializer {
                AdminViewModel(
                    adminRepository = AdminRepository(db),
                    adminUid = adminUid
                )
            }
        }
    )
    AdminDashboardScreen(
        appViewModel = appViewModel,
        adminViewModel = adminViewModel,
        adminAccess = adminAccess,
        onExitAdmin = onExitAdmin
    )
}

@Composable
private fun PrismSideDrawerContent(
    displayName: String,
    email: String,
    adminAccess: AdminAccessState,
    onNavigate: (() -> Unit) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenRewards: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = DeepSurface,
        drawerContentColor = Color.White,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(PrismCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = PrismCyan)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayName, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
                    Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Spacer(Modifier.height(10.dp))
            StatusPill(
                label = when {
                    adminAccess.isSuperAdmin -> "SUPER ADMIN"
                    adminAccess.isAdmin -> "AUTHORIZED ADMIN"
                    else -> "PRISM PLAYER"
                },
                color = if (adminAccess.isAdmin) PrismGold else PrismCyan
            )
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = SurfaceBorder)
            Spacer(Modifier.height(8.dp))

            DrawerMenuItem(Icons.Filled.Person, "Profile", PrismCyan) { onNavigate(onOpenProfile) }
            DrawerMenuItem(Icons.Filled.AccountBalanceWallet, "Dual Wallet", PrismCyan) { onNavigate(onOpenWallet) }
            DrawerMenuItem(Icons.Filled.Palette, "Premium Coin Shop", PrismViolet) { onNavigate(onOpenShop) }
            DrawerMenuItem(Icons.Filled.Redeem, "Daily Rewards", PrismGold) { onNavigate(onOpenRewards) }
            DrawerMenuItem(Icons.Filled.Leaderboard, "Leaderboards", PrismCyan) { onNavigate(onOpenLeaderboard) }
            DrawerMenuItem(Icons.Filled.Settings, "Settings & Audio", MaterialTheme.colorScheme.onSurfaceVariant) { onNavigate(onOpenSettings) }
            DrawerMenuItem(Icons.AutoMirrored.Filled.HelpOutline, "Help & Support", MaterialTheme.colorScheme.onSurfaceVariant) { onNavigate(onOpenHelp) }

            if (adminAccess.isAdmin) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = SurfaceBorder)
                Spacer(Modifier.height(8.dp))
                DrawerMenuItem(
                    icon = Icons.Filled.AdminPanelSettings,
                    label = if (adminAccess.isSuperAdmin) "Super Admin Panel" else "Admin Dashboard",
                    tint = PrismGold,
                    tag = "drawer_admin_panel"
                ) { onNavigate(onOpenAdmin) }
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider(color = SurfaceBorder)
            Spacer(Modifier.height(8.dp))
            DrawerMenuItem(Icons.AutoMirrored.Filled.Logout, "Sign Out", PrismCrimson, tag = "drawer_sign_out") {
                onNavigate(onSignOut)
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    tag: String = "drawer_${label.lowercase().replace(" ", "_")}",
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label, style = MaterialTheme.typography.titleSmall) },
        selected = false,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label, tint = tint) },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = Color.White
        ),
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun BlockedAccountScreen(onSignOut: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DeepSurface,
            border = BorderStroke(1.5.dp, PrismCrimson),
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Block, contentDescription = "Account blocked", tint = PrismCrimson, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("Account Suspended", style = MaterialTheme.typography.headlineSmall, color = PrismCrimson)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Your account has been restricted by an Administrator. Contact Support for assistance or sign out below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrismCrimson, contentColor = Color.White)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
