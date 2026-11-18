package com.example.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BoosterType
import com.example.data.model.DEFAULT_LINES_PER_LEVEL
import com.example.data.model.GameMode
import com.example.game.BOARD_SIZE
import com.example.game.BlockDragCalculator
import com.example.game.BlockPuzzleEngine
import com.example.game.BlockSkinRenderStyle
import com.example.game.BlockThemes
import com.example.game.BoardBackgroundSpec
import com.example.game.BoardGeometry
import com.example.game.BoardThemeSpec
import com.example.game.CosmeticCatalog
import com.example.game.GameProgressCalculator
import com.example.game.PolyominoShape
import com.example.game.SnapPreview
import com.example.game.SpecialEffectSpec
import com.example.game.TrayPiece
import com.example.integrations.RewardedAdProvider
import com.example.integrations.RewardedAdStatus
import com.example.ui.components.StatusPill
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PrismCoral
import com.example.ui.theme.PrismCrimson
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismGold
import com.example.ui.theme.PrismMint
import com.example.ui.theme.SurfaceBorder
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.GamePlayUiState
import com.example.viewmodel.GamePlayViewModel
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt

private data class SnapBackAnimation(
    val id: Long,
    val shape: PolyominoShape,
    val fromCenterInRoot: Offset,
    val toCenterInRoot: Offset
)

@Composable
fun GameplayScreen(
    appViewModel: AppViewModel,
    gamePlayViewModel: GamePlayViewModel,
    onExitGame: () -> Unit
) {
    val gameState by gamePlayViewModel.state.collectAsStateWithLifecycle()
    val user by appViewModel.userAccount.collectAsStateWithLifecycle()
    val settings by appViewModel.globalSettings.collectAsStateWithLifecycle()
    val inventory by appViewModel.playerInventory.collectAsStateWithLifecycle()
    val shopItems by appViewModel.shopItems.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val activeTheme = remember(inventory.equippedThemeId, user?.selectedThemeId) {
        val id = inventory.equippedThemeId.ifBlank { user?.selectedThemeId ?: BlockThemes.simpleClassic.id }
        BlockThemes.byId(id)
    }
    val activeBlockSkin = remember(inventory.equippedBlockSkinId) {
        CosmeticCatalog.blockSkinByKey(inventory.equippedBlockSkinId)
    }
    val activeBackground = remember(inventory.equippedBackgroundId) {
        CosmeticCatalog.backgroundByKey(inventory.equippedBackgroundId)
    }
    val activeEffect = remember(inventory.equippedEffectId) {
        CosmeticCatalog.effectByKey(inventory.equippedEffectId)
    }

    // Dynamic booster configs from live Coin Shop items (respects Admin edits immediately!)
    fun boosterLimits(type: BoosterType): Pair<Int, Int> {
        val item = shopItems.firstOrNull {
            it.category == "GAME_BOOSTER" && it.subType.equals(type.wireName, ignoreCase = true)
        }
        val maxPerGame = (item?.boosterMaxPerGame?.toInt() ?: type.defaultMaxPerGame).coerceAtLeast(1)
        val radius = (item?.boosterPowerRadius?.toInt() ?: if (type == BoosterType.HAMMER) 1 else 0).coerceAtLeast(0)
        return maxPerGame to radius
    }

    val rewardedAdProvider = remember { RewardedAdProvider() }
    val rewardedAdStatus = remember(settings) { rewardedAdProvider.getStatus(settings) }

    var showAdConfigDetails by remember { mutableStateOf(false) }
    var isContinuingWithCoins by remember { mutableStateOf(false) }
    var boosterBusy by remember { mutableStateOf(false) }

    // Auto-save permanent level progress to Firestore whenever lines are cleared mid-session
    LaunchedEffect(gameState.sessionId, gameState.linesCleared) {
        if (gameState.linesCleared > 0L && !gameState.isGameOver && !gameState.isLevelCompleted) {
            gamePlayViewModel.saveLiveProgressToDatabase(appViewModel)
        }
    }

    // Automatically finalize session score, leaderboards & rewards on Game Over or Level Complete
    LaunchedEffect(gameState.isGameOver, gameState.isLevelCompleted) {
        if (gameState.isGameOver || gameState.isLevelCompleted) {
            gamePlayViewModel.syncSessionToBackend(appViewModel)
        }
    }

    fun saveAndExit() {
        if (gameState.linesCleared > 0L) {
            gamePlayViewModel.syncSessionToBackend(appViewModel) {
                onExitGame()
            }
        } else {
            onExitGame()
        }
    }

    BackHandler { saveAndExit() }

    GameplayBoardStage(
        gameState = gameState,
        activeTheme = activeTheme,
        blockSkinStyle = activeBlockSkin.renderStyle,
        boardBackground = activeBackground,
        specialEffect = activeEffect,
        hammerCount = inventory.hammerCount,
        undoCount = inventory.undoCount,
        shuffleCount = inventory.shuffleCount,
        rowBlasterCount = inventory.rowBlasterCount,
        continueCost = settings.continueCostCoins,
        playerCoins = user?.totalCoins ?: 0L,
        isContinuingWithCoins = isContinuingWithCoins,
        rewardedAdStatus = rewardedAdStatus,
        showAdConfigDetails = showAdConfigDetails,
        onTryPlacePiece = { slot, row, col ->
            gamePlayViewModel.tryPlacePiece(slot, row, col)
        },
        onBoosterButtonClick = { type ->
            if (boosterBusy) return@GameplayBoardStage
            val owned = inventory.boosterCount(type)
            val (maxPerGame, _) = boosterLimits(type)
            when (type) {
                BoosterType.HAMMER, BoosterType.ROW_BLASTER -> {
                    gamePlayViewModel.toggleBoosterTargeting(type, owned, maxPerGame)
                        .onFailure { appViewModel.showError(it.message ?: "Cannot use ${type.label}.") }
                }
                BoosterType.UNDO -> {
                    val check = gamePlayViewModel.validateBoosterUsable(type, owned, maxPerGame)
                    if (check.isFailure) {
                        appViewModel.showError(check.exceptionOrNull()?.message ?: "Cannot Undo.")
                    } else {
                        scope.launch {
                            boosterBusy = true
                            val consumed = appViewModel.consumeBooster(BoosterType.UNDO)
                            if (consumed.isSuccess) {
                                gamePlayViewModel.executeUndo(owned, maxPerGame)
                            }
                            boosterBusy = false
                        }
                    }
                }
                BoosterType.SHUFFLE -> {
                    val check = gamePlayViewModel.validateBoosterUsable(type, owned, maxPerGame)
                    if (check.isFailure) {
                        appViewModel.showError(check.exceptionOrNull()?.message ?: "Cannot Shuffle.")
                    } else {
                        scope.launch {
                            boosterBusy = true
                            val consumed = appViewModel.consumeBooster(BoosterType.SHUFFLE)
                            if (consumed.isSuccess) {
                                gamePlayViewModel.executeShuffle(owned, maxPerGame)
                            }
                            boosterBusy = false
                        }
                    }
                }
            }
        },
        onBoardBoosterTap = { row, col ->
            if (boosterBusy) return@GameplayBoardStage
            val mode = gameState.activeBoosterMode ?: return@GameplayBoardStage
            val owned = inventory.boosterCount(mode)
            val (maxPerGame, powerRadius) = boosterLimits(mode)
            when (mode) {
                BoosterType.HAMMER -> {
                    if (!gamePlayViewModel.canHammerAt(row, col)) {
                        appViewModel.showError("Tap a filled 3D block to smash it!")
                        return@GameplayBoardStage
                    }
                    scope.launch {
                        boosterBusy = true
                        val consumed = appViewModel.consumeBooster(BoosterType.HAMMER)
                        if (consumed.isSuccess) {
                            gamePlayViewModel.executeHammerAt(row, col, owned, maxPerGame, powerRadius)
                        }
                        boosterBusy = false
                    }
                }
                BoosterType.ROW_BLASTER -> {
                    if (!gamePlayViewModel.canBlastRow(row)) {
                        appViewModel.showError("Tap a row containing at least one block!")
                        return@GameplayBoardStage
                    }
                    scope.launch {
                        boosterBusy = true
                        val consumed = appViewModel.consumeBooster(BoosterType.ROW_BLASTER)
                        if (consumed.isSuccess) {
                            gamePlayViewModel.executeRowBlasterAt(row, owned, maxPerGame)
                        }
                        boosterBusy = false
                    }
                }
                else -> Unit
            }
        },
        onCancelBoosterMode = { gamePlayViewModel.cancelBoosterTargeting() },
        onBackClick = { saveAndExit() },
        onRestartClick = {
            gamePlayViewModel.restartPreservingPermanentLevel(
                appViewModel = appViewModel,
                targetMode = gameState.mode
            )
        },
        onPlayNextLevel = { nextLvl ->
            gamePlayViewModel.restartPreservingPermanentLevel(
                appViewModel = appViewModel,
                targetMode = GameMode.LEVEL,
                overrideLevelNumber = nextLvl
            )
        },
        onContinueWithCoins = {
            scope.launch {
                isContinuingWithCoins = true
                val res = appViewModel.spendCoinsForContinue()
                isContinuingWithCoins = false
                if (res.isSuccess) {
                    gamePlayViewModel.applyContinueAfterPayment()
                }
            }
        },
        onToggleAdDetails = { showAdConfigDetails = !showAdConfigDetails },
        onExitGame = onExitGame
    )
}

/**
 * Interactive 10x10 3D block puzzle stage with unified pointer-event drag & drop,
 * live equipped Block Skins, Board Backgrounds, Special Effects, Dual Progress Bars,
 * and interactive Game Boosters (Hammer, Undo, Shuffle, Line Blaster).
 */
@Composable
fun GameplayBoardStage(
    gameState: GamePlayUiState,
    activeTheme: BoardThemeSpec,
    continueCost: Long = 50L,
    playerCoins: Long = 0L,
    isContinuingWithCoins: Boolean = false,
    rewardedAdStatus: RewardedAdStatus = RewardedAdStatus.Ready,
    showAdConfigDetails: Boolean = false,
    onTryPlacePiece: (slotIndex: Int, anchorRow: Int, anchorCol: Int) -> Boolean,
    onBackClick: () -> Unit = {},
    onRestartClick: () -> Unit = {},
    onPlayNextLevel: (Long) -> Unit = {},
    onContinueWithCoins: () -> Unit = {},
    onToggleAdDetails: () -> Unit = {},
    onExitGame: () -> Unit = {},
    blockSkinStyle: BlockSkinRenderStyle = BlockSkinRenderStyle.DEFAULT_GLOSSY,
    boardBackground: BoardBackgroundSpec = CosmeticCatalog.defaultBackground,
    specialEffect: SpecialEffectSpec = CosmeticCatalog.defaultEffect,
    hammerCount: Long = 0L,
    undoCount: Long = 0L,
    shuffleCount: Long = 0L,
    rowBlasterCount: Long = 0L,
    onBoosterButtonClick: (BoosterType) -> Unit = {},
    onBoardBoosterTap: (row: Int, col: Int) -> Unit = { _, _ -> },
    onCancelBoosterMode: () -> Unit = {}
) {
    val density = LocalDensity.current
    val touchSlopPx = LocalViewConfiguration.current.touchSlop
    val touchLiftPx = with(density) { 64.dp.toPx() }

    var overlayOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var boardBoundsInRoot by remember { mutableStateOf(Rect.Zero) }

    var draggedSlotIndex by remember { mutableIntStateOf(-1) }
    var dragFingerInRoot by remember { mutableStateOf(Offset.Zero) }
    var dragPointerLiftPx by remember { mutableFloatStateOf(0f) }
    var selectedSlotForTap by remember { mutableIntStateOf(-1) }

    var snapBackRequest by remember { mutableStateOf<SnapBackAnimation?>(null) }
    val snapBackProgress = remember { Animatable(0f) }

    LaunchedEffect(snapBackRequest?.id) {
        val req = snapBackRequest ?: return@LaunchedEffect
        snapBackProgress.snapTo(0f)
        snapBackProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing)
        )
        if (snapBackRequest?.id == req.id) {
            snapBackRequest = null
        }
    }

    val latestGrid by rememberUpdatedState(gameState.grid)
    val latestTray by rememberUpdatedState(gameState.tray)
    val latestIsGameOver by rememberUpdatedState(gameState.isGameOver)
    val latestIsLevelCompleted by rememberUpdatedState(gameState.isLevelCompleted)
    val latestActiveBooster by rememberUpdatedState(gameState.activeBoosterMode)
    val latestOnTryPlace by rememberUpdatedState(onTryPlacePiece)
    val latestOnBoardBoosterTap by rememberUpdatedState(onBoardBoosterTap)

    val activeDraggedPiece: TrayPiece? = remember(draggedSlotIndex, gameState.tray) {
        if (draggedSlotIndex >= 0) {
            gameState.tray.firstOrNull { it.slotIndex == draggedSlotIndex && !it.used }
        } else null
    }

    val boardGeometry: BoardGeometry = remember(boardBoundsInRoot) {
        BoardGeometry.fromBounds(boardBoundsInRoot)
    }

    val snapTarget: SnapPreview? = remember(
        activeDraggedPiece,
        dragFingerInRoot,
        dragPointerLiftPx,
        boardGeometry,
        gameState.grid
    ) {
        val piece = activeDraggedPiece ?: return@remember null
        BlockDragCalculator.computeSnapTarget(
            grid = gameState.grid,
            shape = piece.shape,
            pointerInRoot = dragFingerInRoot,
            liftPx = dragPointerLiftPx,
            board = boardGeometry
        )
    }

    val potentialClears = remember(snapTarget, activeDraggedPiece, gameState.grid) {
        val piece = activeDraggedPiece
        val target = snapTarget
        if (piece != null && target != null && target.isValid) {
            BlockPuzzleEngine.previewPotentialClears(
                gameState.grid,
                piece.shape,
                target.anchorRow,
                target.anchorCol
            )
        } else emptySet()
    }

    val pulseTransition = rememberInfiniteTransition(label = "clearPulse")
    val clearPulseAlpha by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(240), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    // Outer unpadded container with Equipped Board Background (Dark, Gradient, Colorful, Premium or Animated)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                overlayOriginInRoot = coords.boundsInRoot().topLeft
            }
    ) {
        EquippedBoardBackground(
            spec = boardBackground,
            modifier = Modifier.fillMaxSize()
        )

        // Safe-inset main gameplay layout (HUD + Dual Progress + 10x10 Board + Boosters + 3-Block Tray)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Game HUD Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("game_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Save and exit game",
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when (gameState.mode) {
                                    GameMode.CLASSIC -> "Classic Mode"
                                    GameMode.LEVEL -> "Level ${gameState.activeLevelNumber} / 100"
                                    GameMode.DAILY -> "Daily Challenge"
                                    GameMode.TIME -> gameState.activeTimeChallenge?.title ?: "Time Challenge"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.testTag("game_mode_title")
                            )
                            StatusPill(
                                label = activeTheme.name.substringBefore(" "),
                                color = activeTheme.previewAccent
                            )
                        }
                        if (gameState.mode == GameMode.TIME) {
                            val mins = gameState.remainingSeconds / 60
                            val secs = gameState.remainingSeconds % 60
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = "Countdown timer",
                                    tint = if (gameState.remainingSeconds <= 15) PrismCrimson else PrismCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "%02d:%02d remaining".format(mins, secs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (gameState.remainingSeconds <= 15) PrismCrimson else PrismCyan
                                )
                            }
                        } else if (gameState.mode == GameMode.DAILY && gameState.activeDailyChallenge != null) {
                            Text(
                                text = "Goal: ${gameState.linesCleared}/${gameState.activeDailyChallenge.targetLines} lines (+${gameState.activeDailyChallenge.bonusCoins} bonus Coins)",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrismGold
                            )
                        }
                    }

                    IconButton(
                        onClick = onRestartClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("game_restart_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Restart board",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Dual Premium 3D Animated Progress Bars (Permanent Level Progress + Current Game Progress)
                val levelMetrics = remember(
                    gameState.permanentLevelNumber,
                    gameState.effectivePermanentLevelLines
                ) {
                    GameProgressCalculator.computeLevelProgress(
                        levelNumber = gameState.permanentLevelNumber,
                        clearedLines = gameState.effectivePermanentLevelLines,
                        requiredLines = DEFAULT_LINES_PER_LEVEL
                    )
                }
                val gameMetrics = remember(
                    gameState.mode,
                    gameState.linesCleared,
                    gameState.activeDailyChallenge,
                    gameState.activeTimeChallenge
                ) {
                    GameProgressCalculator.computeGameSessionProgress(
                        mode = gameState.mode,
                        sessionLinesCleared = gameState.linesCleared,
                        dailyChallenge = gameState.activeDailyChallenge,
                        timeChallenge = gameState.activeTimeChallenge
                    )
                }

                DualGameProgressPanel(
                    levelMetrics = levelMetrics,
                    gameMetrics = gameMetrics
                )

                Spacer(Modifier.height(6.dp))

                // Score / Lines / Coins Live Counter Bar (Exact 10 Score + 1 Coin per line)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HudCounterBox(
                        label = "SCORE",
                        value = "%,d".format(gameState.score),
                        accent = PrismCyan,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hud_score")
                    )
                    HudCounterBox(
                        label = "LINES",
                        value = "%,d".format(gameState.linesCleared),
                        accent = PrismMint,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hud_lines")
                    )
                    HudCounterBox(
                        label = "COINS",
                        value = "+%,d".format(gameState.coinsEarned),
                        accent = PrismGold,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hud_coins")
                    )
                }

                Spacer(Modifier.height(6.dp))

                // 10x10 Interactive Square 3D Game Board
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val boardSide = minOf(maxWidth, maxHeight)

                    Surface(
                        modifier = Modifier
                            .size(boardSide)
                            .testTag("puzzle_board_10x10"),
                        shape = RoundedCornerShape(20.dp),
                        color = activeTheme.boardBgBottom,
                        border = BorderStroke(
                            width = if (gameState.activeBoosterMode != null) 2.5.dp else 2.dp,
                            color = if (gameState.activeBoosterMode != null) PrismGold else activeTheme.boardFrameBorder.copy(alpha = 0.7f)
                        ),
                        shadowElevation = 12.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Board10x10Canvas(
                                grid = gameState.grid,
                                theme = activeTheme,
                                clearingCells = gameState.clearingCells,
                                previewShape = activeDraggedPiece?.shape,
                                previewAnchorRow = snapTarget?.anchorRow,
                                previewAnchorCol = snapTarget?.anchorCol,
                                previewValid = snapTarget?.isValid ?: false,
                                potentialClearCells = potentialClears,
                                clearPulseAlpha = clearPulseAlpha,
                                blockSkinStyle = blockSkinStyle,
                                boosterTargetingActive = gameState.activeBoosterMode != null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                                    .testTag("puzzle_board_canvas")
                                    .onGloballyPositioned { coords ->
                                        boardBoundsInRoot = coords.boundsInRoot()
                                    }
                                    .pointerInput(Unit) {
                                        detectTapGestures { tapOffset ->
                                            val booster = latestActiveBooster
                                            if (booster == BoosterType.HAMMER || booster == BoosterType.ROW_BLASTER) {
                                                val side = minOf(size.width.toFloat(), size.height.toFloat())
                                                val startX = (size.width - side) / 2f
                                                val startY = (size.height - side) / 2f
                                                val cellSize = side / BOARD_SIZE.toFloat()
                                                if (cellSize > 0f) {
                                                    val col = floor((tapOffset.x - startX) / cellSize).toInt()
                                                    val row = floor((tapOffset.y - startY) / cellSize).toInt()
                                                    if (row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE) {
                                                        latestOnBoardBoosterTap(row, col)
                                                    }
                                                }
                                                return@detectTapGestures
                                            }
                                            val slot = selectedSlotForTap
                                            if (slot >= 0 && !latestIsGameOver && !latestIsLevelCompleted) {
                                                val piece = latestTray.firstOrNull { it.slotIndex == slot && !it.used }
                                                    ?: return@detectTapGestures
                                                val anchor = BlockDragCalculator.computeTapAnchor(
                                                    tapOffsetInCanvas = tapOffset,
                                                    canvasWidth = size.width.toFloat(),
                                                    canvasHeight = size.height.toFloat(),
                                                    shape = piece.shape
                                                ) ?: return@detectTapGestures
                                                if (latestOnTryPlace(slot, anchor.row, anchor.col)) {
                                                    selectedSlotForTap = -1
                                                }
                                            }
                                        }
                                    }
                            )

                            // Equipped Special Effects Layer (Glow, Fire, Lightning, Rainbow, Particles, Shockwave, Crystal, Supernova)
                            SpecialEffectOverlay(
                                effectSpec = specialEffect,
                                clearingCells = gameState.clearingCells,
                                lastPlacedCells = gameState.lastPlacedCells,
                                clearEffectToken = gameState.clearEffectToken,
                                placementEffectToken = gameState.placementEffectToken,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                            )
                        }
                    }

                    // Floating Combo & Score/Coin/Booster Reward Banner
                    androidx.compose.animation.AnimatedVisibility(
                        visible = gameState.floatingPopup != null,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        val popup = gameState.floatingPopup
                        if (popup != null) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color(0xEE181334),
                                border = BorderStroke(1.5.dp, PrismGold)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (popup.comboLabel != null) {
                                        Text(
                                            text = popup.comboLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = PrismCyan
                                        )
                                    }
                                    Text(
                                        text = popup.text,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PrismGold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Real Gameplay Boosters Bar (or live Targeting Banner when Hammer / Line Blaster selected)
                if (gameState.activeBoosterMode != null) {
                    val activeB = gameState.activeBoosterMode
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PrismGold.copy(alpha = 0.20f),
                        border = BorderStroke(1.5.dp, PrismGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("booster_targeting_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when (activeB) {
                                    BoosterType.HAMMER -> "HAMMER READY — Tap any filled block on the 10×10 board to smash it!"
                                    BoosterType.ROW_BLASTER -> "LINE BLASTER READY — Tap any row on the board to vaporize it!"
                                    else -> "Booster Active"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onCancelBoosterMode,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.testTag("booster_cancel_button")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel booster", tint = PrismGold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Cancel", color = PrismGold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gameplay_booster_bar"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BoosterActionChip(
                            icon = Icons.Filled.Construction,
                            label = "Hammer",
                            count = hammerCount,
                            accent = PrismCoral,
                            enabled = !gameState.isGameOver && !gameState.isLevelCompleted,
                            onClick = { onBoosterButtonClick(BoosterType.HAMMER) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("booster_btn_hammer")
                        )
                        BoosterActionChip(
                            icon = Icons.AutoMirrored.Filled.Undo,
                            label = "Undo",
                            count = undoCount,
                            accent = PrismCyan,
                            enabled = gameState.canUndo && !gameState.isLevelCompleted,
                            onClick = { onBoosterButtonClick(BoosterType.UNDO) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("booster_btn_undo")
                        )
                        BoosterActionChip(
                            icon = Icons.Filled.Shuffle,
                            label = "Shuffle",
                            count = shuffleCount,
                            accent = PrismMint,
                            enabled = !gameState.isGameOver && !gameState.isLevelCompleted,
                            onClick = { onBoosterButtonClick(BoosterType.SHUFFLE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("booster_btn_shuffle")
                        )
                        BoosterActionChip(
                            icon = Icons.Filled.Bolt,
                            label = "Blaster",
                            count = rowBlasterCount,
                            accent = PrismGold,
                            enabled = !gameState.isGameOver && !gameState.isLevelCompleted,
                            onClick = { onBoosterButtonClick(BoosterType.ROW_BLASTER) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("booster_btn_row_blaster")
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 3-Piece Interactive Tray with unified pointerdown/pointermove/pointerup/pointercancel handling
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("block_tray"),
                    shape = RoundedCornerShape(22.dp),
                    color = DeepSurface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        gameState.tray.forEach { piece ->
                            val slotIndex = piece.slotIndex
                            val slotOriginState = remember(slotIndex) { mutableStateOf(Offset.Zero) }
                            val slotCenterState = remember(slotIndex) { mutableStateOf(Offset.Zero) }
                            val isDraggingThis = draggedSlotIndex == slotIndex && !piece.used
                            val isSelectedForTap = selectedSlotForTap == slotIndex && !piece.used

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .testTag("tray_piece_$slotIndex")
                                    .onGloballyPositioned { coords ->
                                        val bounds = coords.boundsInRoot()
                                        slotOriginState.value = bounds.topLeft
                                        slotCenterState.value = bounds.center
                                    }
                                    .pointerInput(slotIndex) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val livePiece = latestTray.firstOrNull {
                                                it.slotIndex == slotIndex && !it.used
                                            } ?: return@awaitEachGesture
                                            if (latestIsGameOver || latestIsLevelCompleted) {
                                                return@awaitEachGesture
                                            }

                                            down.consume()

                                            val pointerId = down.id
                                            val activeLift = if (down.type == PointerType.Touch) touchLiftPx else 0f
                                            val startRootPos = slotOriginState.value + down.position
                                            var currentRootPos = startRootPos
                                            var maxTravel = 0f
                                            var cancelled = false
                                            var finishedNormally = false

                                            snapBackRequest = null
                                            draggedSlotIndex = slotIndex
                                            dragPointerLiftPx = activeLift
                                            dragFingerInRoot = currentRootPos

                                            try {
                                                while (true) {
                                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                                    val change = event.changes.firstOrNull { it.id == pointerId }
                                                    if (change == null) {
                                                        cancelled = true
                                                        break
                                                    }

                                                    if (!change.pressed) {
                                                        if (change.isConsumed || !change.previousPressed) {
                                                            cancelled = true
                                                        } else {
                                                            currentRootPos = slotOriginState.value + change.position
                                                            dragFingerInRoot = currentRootPos
                                                            change.consume()
                                                        }
                                                        break
                                                    }

                                                    if (change.positionChanged()) {
                                                        currentRootPos = slotOriginState.value + change.position
                                                        dragFingerInRoot = currentRootPos
                                                        val travel = (currentRootPos - startRootPos).getDistance()
                                                        if (travel > maxTravel) maxTravel = travel
                                                        change.consume()
                                                    }
                                                }
                                                finishedNormally = true
                                            } finally {
                                                if (!finishedNormally && draggedSlotIndex == slotIndex) {
                                                    draggedSlotIndex = -1
                                                }
                                            }

                                            val liveBoard = BoardGeometry.fromBounds(boardBoundsInRoot)
                                            val releaseTarget = if (!cancelled) {
                                                BlockDragCalculator.computeSnapTarget(
                                                    grid = latestGrid,
                                                    shape = livePiece.shape,
                                                    pointerInRoot = currentRootPos,
                                                    liftPx = activeLift,
                                                    board = liveBoard
                                                )
                                            } else null

                                            if (!cancelled && releaseTarget != null && releaseTarget.isValid) {
                                                selectedSlotForTap = -1
                                                draggedSlotIndex = -1
                                                latestOnTryPlace(
                                                    livePiece.slotIndex,
                                                    releaseTarget.anchorRow,
                                                    releaseTarget.anchorCol
                                                )
                                            } else {
                                                val wasTap = !cancelled && maxTravel < touchSlopPx && releaseTarget == null
                                                draggedSlotIndex = -1
                                                if (wasTap) {
                                                    selectedSlotForTap = if (selectedSlotForTap == slotIndex) -1 else slotIndex
                                                } else {
                                                    val homeCenter = slotCenterState.value
                                                    if (homeCenter != Offset.Zero) {
                                                        snapBackRequest = SnapBackAnimation(
                                                            id = System.nanoTime(),
                                                            shape = livePiece.shape,
                                                            fromCenterInRoot = Offset(
                                                                currentRootPos.x,
                                                                currentRootPos.y - activeLift
                                                            ),
                                                            toCenterInRoot = homeCenter
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelectedForTap) CardSurface else Color(0xFF1A1533),
                                border = BorderStroke(
                                    width = if (isSelectedForTap || isDraggingThis) 2.dp else 1.dp,
                                    color = when {
                                        isDraggingThis -> PrismGold
                                        isSelectedForTap -> PrismCyan
                                        else -> SurfaceBorder.copy(alpha = 0.5f)
                                    }
                                )
                            ) {
                                if (!piece.used) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Polyomino3DCanvas(
                                            shape = piece.shape,
                                            theme = activeTheme,
                                            alpha = if (isDraggingThis) 0.18f else 1f,
                                            blockSkinStyle = blockSkinStyle,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "USED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Unpadded Full-Screen Floating 3D Drag & Snap-Back Overlay Layer (zIndex 50, purely visual)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(50f)
        ) {
            if (activeDraggedPiece != null && boardGeometry.isValid) {
                val cellPx = boardGeometry.cellPx
                val widthPx = activeDraggedPiece.shape.cols * cellPx
                val heightPx = activeDraggedPiece.shape.rows * cellPx
                val topLeft = BlockDragCalculator.computeFloatingOverlayTopLeft(
                    pointerInRoot = dragFingerInRoot,
                    liftPx = dragPointerLiftPx,
                    shape = activeDraggedPiece.shape,
                    board = boardGeometry,
                    overlayOriginInRoot = overlayOriginInRoot
                )

                Polyomino3DCanvas(
                    shape = activeDraggedPiece.shape,
                    theme = activeTheme,
                    maxCellSizePx = cellPx,
                    alpha = 0.96f,
                    blockSkinStyle = blockSkinStyle,
                    modifier = Modifier
                        .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
                        .size(
                            width = with(density) { widthPx.toDp() },
                            height = with(density) { heightPx.toDp() }
                        )
                        .testTag("floating_drag_piece")
                )
            } else {
                val snapBack = snapBackRequest
                if (snapBack != null && boardGeometry.isValid) {
                    val t = snapBackProgress.value
                    val curCenterX = snapBack.fromCenterInRoot.x + (snapBack.toCenterInRoot.x - snapBack.fromCenterInRoot.x) * t
                    val curCenterY = snapBack.fromCenterInRoot.y + (snapBack.toCenterInRoot.y - snapBack.fromCenterInRoot.y) * t
                    val cellPx = boardGeometry.cellPx * (1f - 0.35f * t)
                    val widthPx = snapBack.shape.cols * cellPx
                    val heightPx = snapBack.shape.rows * cellPx
                    val localX = (curCenterX - overlayOriginInRoot.x) - widthPx / 2f
                    val localY = (curCenterY - overlayOriginInRoot.y) - heightPx / 2f

                    Polyomino3DCanvas(
                        shape = snapBack.shape,
                        theme = activeTheme,
                        maxCellSizePx = cellPx,
                        alpha = (0.92f * (1f - 0.25f * t)).coerceIn(0f, 1f),
                        blockSkinStyle = blockSkinStyle,
                        modifier = Modifier
                            .offset { IntOffset(localX.roundToInt(), localY.roundToInt()) }
                            .size(
                                width = with(density) { widthPx.toDp() },
                                height = with(density) { heightPx.toDp() }
                            )
                    )
                }
            }
        }

        // LEVEL COMPLETION & GAME OVER OVERLAYS
        Box(modifier = Modifier.fillMaxSize().zIndex(80f)) {
            LevelCompleteOverlay(
                visible = gameState.isLevelCompleted,
                gameState = gameState,
                onPlayNextLevel = onPlayNextLevel,
                onExitGame = onExitGame
            )

            GameOverOverlay(
                visible = gameState.isGameOver && !gameState.isLevelCompleted,
                gameState = gameState,
                continueCost = continueCost,
                playerCoins = playerCoins,
                isContinuingWithCoins = isContinuingWithCoins,
                rewardedAdStatus = rewardedAdStatus,
                showAdConfigDetails = showAdConfigDetails,
                onContinueWithCoins = onContinueWithCoins,
                onToggleAdDetails = onToggleAdDetails,
                onExitGame = onExitGame,
                onPlayAgain = onRestartClick
            )
        }
    }
}

@Composable
private fun BoosterActionChip(
    icon: ImageVector,
    label: String,
    count: Long,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasCharge = count > 0L
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (hasCharge) accent.copy(alpha = 0.16f) else DeepSurface.copy(alpha = 0.85f),
        border = BorderStroke(
            width = 1.dp,
            color = if (hasCharge) accent.copy(alpha = 0.65f) else SurfaceBorder.copy(alpha = 0.55f)
        ),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$label booster ($count left)",
                tint = if (hasCharge) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (hasCharge) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
                Text(
                    text = "×$count",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = if (hasCharge) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Start,
                    maxLines = 1
                )
            }
        }
    }
}
