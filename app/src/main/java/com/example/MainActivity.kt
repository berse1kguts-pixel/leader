package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.IosSheetMode
import com.example.ui.NavigationViewModel
import com.example.ui.SheetDetent
import com.example.ui.components.HudCockpitModal
import com.example.ui.components.InteractiveMapCanvas
import com.example.ui.components.IosActiveNavigationBottomCard
import com.example.ui.components.IosExploreBottomSheet
import com.example.ui.components.IosMapModeSelectorSheet
import com.example.ui.components.IosPlaceDetailSheet
import com.example.ui.components.IosRoutePreviewSheet
import com.example.ui.components.IosTopTurnByTurnBanner
import com.example.ui.components.MapTileStyle
import com.example.ui.screens.OpenSourceGuideScreen
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosSheetBgLight
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = false) {
                PureNavIosApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PureNavIosApp(navViewModel: NavigationViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by navViewModel.uiState.collectAsStateWithLifecycle()
    val telemetry by navViewModel.telemetry.collectAsStateWithLifecycle()
    val savedPlaces by navViewModel.savedPlaces.collectAsStateWithLifecycle()
    val recentRoutes by navViewModel.recentRoutes.collectAsStateWithLifecycle()

    var mapStyle by remember { mutableStateOf(MapTileStyle.CN_STREETS) }
    var expandRouteSteps by rememberSaveable { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        navViewModel.sensorTracker.startHardwareTracking(granted)
    }

    DisposableEffect(hasLocationPermission) {
        navViewModel.sensorTracker.startHardwareTracking(hasLocationPermission)
        onDispose {
            navViewModel.sensorTracker.stopHardwareTracking()
        }
    }

    // 严谨的 iOS 层级返回逻辑
    BackHandler(
        enabled = uiState.isHudMode ||
            uiState.showEngineArchitectureSheet ||
            uiState.showMapSettingsSheet ||
            uiState.showSearchSheet ||
            uiState.sheetMode != IosSheetMode.EXPLORE ||
            uiState.sheetDetent == SheetDetent.EXPANDED
    ) {
        when {
            uiState.isHudMode -> navViewModel.toggleHudMode()
            uiState.showEngineArchitectureSheet -> navViewModel.toggleEngineArchitectureSheet(false)
            uiState.showMapSettingsSheet -> navViewModel.toggleMapSettingsSheet(false)
            uiState.showSearchSheet -> navViewModel.dismissSearchSheet()
            uiState.sheetMode == IosSheetMode.ACTIVE_NAVIGATION -> navViewModel.stopNavigation()
            uiState.sheetMode == IosSheetMode.ROUTE_PREVIEW -> navViewModel.closePlaceOrRouteBackToExplore()
            uiState.sheetMode == IosSheetMode.PLACE_DETAIL -> navViewModel.closePlaceOrRouteBackToExplore()
            uiState.sheetDetent == SheetDetent.EXPANDED -> navViewModel.setSheetDetent(SheetDetent.HALF)
        }
    }

    if (uiState.isHudMode) {
        val activeStep = uiState.activeRoute?.steps?.getOrNull(uiState.currentStepIndex)
        HudCockpitModal(
            speedKmh = telemetry.speedKmh,
            bearingDegrees = telemetry.bearingDegrees,
            currentStep = activeStep,
            distanceToNextTurnMeters = uiState.distanceToNextTurnMeters,
            remainingDistanceMeters = uiState.remainingDistanceMeters,
            remainingDurationSeconds = uiState.remainingDurationSeconds,
            isMirrored = uiState.isHudMirrored,
            onToggleMirror = { navViewModel.toggleHudMirror() },
            onExitHud = { navViewModel.toggleHudMode() }
        )
        return
    }

    // 1. iOS「选择地图」设置半屏弹窗（点击右上角地图图层图标弹出）
    if (uiState.showMapSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { navViewModel.toggleMapSettingsSheet(false) },
            containerColor = IosSheetBgLight,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
        ) {
            IosMapModeSelectorSheet(
                currentStyle = mapStyle,
                preferredEngine = uiState.preferredEngine,
                onSelectStyle = { mapStyle = it },
                onSelectEngine = { navViewModel.selectEngine(it) },
                onOpenArchitectureDetails = {
                    navViewModel.toggleMapSettingsSheet(false)
                    navViewModel.toggleEngineArchitectureSheet(true)
                },
                onDismiss = { navViewModel.toggleMapSettingsSheet(false) }
            )
        }
    }

    // 2. 开源架构对比与腾讯接口状态详情弹窗
    if (uiState.showEngineArchitectureSheet) {
        ModalBottomSheet(
            onDismissRequest = { navViewModel.toggleEngineArchitectureSheet(false) },
            containerColor = IosSheetBgLight,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "开源架构与双引擎诊断",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { navViewModel.toggleEngineArchitectureSheet(false) }) {
                        Text("完成", color = IosBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                OpenSourceGuideScreen(
                    preferredEngine = uiState.preferredEngine,
                    isTencentKeyConfigured = uiState.isTencentKeyConfigured,
                    usedRamMb = uiState.usedRamMb,
                    onSelectEngine = { navViewModel.selectEngine(it) }
                )
            }
        }
    }

    // 动态计算右下角天气微胶囊需要避让的抽屉高度
    val reservedBottomPadding = when (uiState.sheetMode) {
        IosSheetMode.EXPLORE -> when (uiState.sheetDetent) {
            SheetDetent.COLLAPSED -> 96.dp
            SheetDetent.HALF -> 350.dp
            SheetDetent.EXPANDED -> 350.dp
        }
        IosSheetMode.PLACE_DETAIL -> 270.dp
        IosSheetMode.ROUTE_PREVIEW -> 310.dp
        IosSheetMode.ACTIVE_NAVIGATION -> 110.dp
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 全屏沉浸式高德 3D 车道级 / 步行罗盘 / 骑行绿道 / 2D 全览画布
        InteractiveMapCanvas(
            currentLocationWgs84 = telemetry.currentWgs84,
            bearingDegrees = telemetry.bearingDegrees,
            speedKmh = telemetry.speedKmh,
            travelMode = uiState.travelMode,
            selectedPlace = uiState.selectedPlace,
            activeRoute = uiState.activeRoute,
            isNavigating = uiState.isNavigating,
            is3dPitchEnabled = uiState.is3dPitchEnabled,
            mapStyle = mapStyle,
            bottomSheetReservedPadding = reservedBottomPadding,
            onOpenMapSettings = { navViewModel.toggleMapSettingsSheet(true) },
            onToggle3dPitch = { navViewModel.toggle3dPitch() },
            onDropPinOnMap = { pt -> navViewModel.dropPinOnMap(pt) },
            nearbyTransitStations = uiState.nearbyTransitStations,
            selectedTransitStation = uiState.selectedTransitStation,
            onSelectTransitStation = { st -> navViewModel.selectTransitStation(st) },
            currentStepIndex = uiState.currentStepIndex,
            distanceToNextTurnMeters = if (uiState.activeRoute != null) uiState.distanceToNextTurnMeters else 129,
            modifier = Modifier.fillMaxSize()
        )

        // 顶部高德同款深色圆角诱导大卡片 / 公交地铁实时电子站牌
        if (uiState.is3dPitchEnabled || uiState.sheetMode == IosSheetMode.ACTIVE_NAVIGATION || uiState.travelMode.name == "TRANSIT") {
            IosTopTurnByTurnBanner(
                uiState = uiState,
                isVoiceEnabled = telemetry.isVoiceEnabled,
                onToggleVoice = { navViewModel.toggleVoiceAnnouncements() },
                onSelectTransitStation = { st -> navViewModel.selectTransitStation(st) },
                onWalkToTransitStation = { st -> navViewModel.walkToTransitStation(st) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // 底部四态 iOS Apple Maps 抽屉流转
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            when (uiState.sheetMode) {
                IosSheetMode.EXPLORE -> {
                    IosExploreBottomSheet(
                        uiState = uiState,
                        savedPlaces = savedPlaces,
                        recentRoutes = recentRoutes,
                        hasLocationPermission = hasLocationPermission,
                        onRequestLocationPermission = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        navViewModel = navViewModel
                    )
                }

                IosSheetMode.PLACE_DETAIL -> {
                    IosPlaceDetailSheet(
                        uiState = uiState,
                        navViewModel = navViewModel
                    )
                }

                IosSheetMode.ROUTE_PREVIEW -> {
                    IosRoutePreviewSheet(
                        uiState = uiState,
                        expandSteps = expandRouteSteps,
                        onToggleExpandSteps = { expandRouteSteps = !expandRouteSteps },
                        navViewModel = navViewModel
                    )
                }

                IosSheetMode.ACTIVE_NAVIGATION -> {
                    IosActiveNavigationBottomCard(
                        uiState = uiState,
                        onOpenHud = { navViewModel.toggleHudMode() },
                        onEndRoute = { navViewModel.stopNavigation() }
                    )
                }
            }
        }
    }
}
