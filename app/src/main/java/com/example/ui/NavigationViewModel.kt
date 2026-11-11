package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CoordinateUtils
import com.example.data.LatLngPoint
import com.example.data.local.NavDatabase
import com.example.data.local.NavRepository
import com.example.data.local.RouteHistoryEntity
import com.example.data.local.SavedPlaceEntity
import com.example.data.remote.CalculatedRoute
import com.example.data.remote.DualEngineNavService
import com.example.data.remote.NavEngineType
import com.example.data.remote.SearchPoiResult
import com.example.data.remote.TravelMode
import com.example.sensor.GpsAndCompassTracker
import com.example.sensor.TelemetryState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 完整还原 iOS 17/18 Apple Maps + 高德 3D 的四级界面流转状态机：
 * 1. EXPLORE: 纯净地图探索态
 * 2. PLACE_DETAIL: 地点详情卡片态
 * 3. ROUTE_PREVIEW: 路线规划与多方案对比态（汽车 / 公交地铁 / 自行车 / 步行专属视图）
 * 4. ACTIVE_NAVIGATION: 沉浸式导航态（自动根据汽车/步行/骑行/地铁切换专属 3D 渲染与诱导卡片）
 */
enum class IosSheetMode {
    EXPLORE,
    PLACE_DETAIL,
    ROUTE_PREVIEW,
    ACTIVE_NAVIGATION
}

enum class SheetDetent {
    COLLAPSED,
    HALF,
    EXPANDED
}

data class SelectedPlaceDetail(
    val title: String,
    val subtitle: String,
    val category: String,
    val wgs84: LatLngPoint,
    val gcj02: LatLngPoint,
    val distanceMeters: Int,
    val estimatedDriveMinutes: Int,
    val sourceLabel: String
)

data class NavigationUiState(
    val sheetMode: IosSheetMode = IosSheetMode.EXPLORE,
    val sheetDetent: SheetDetent = SheetDetent.HALF,
    val preferredEngine: NavEngineType = NavEngineType.AMAP_LBS,
    val isAmapKeyConfigured: Boolean = false,
    val isTencentKeyConfigured: Boolean = false,
    val travelMode: TravelMode = TravelMode.DRIVING,
    val is3dPitchEnabled: Boolean = true,
    val searchQuery: String = "",
    val activeCategoryFilter: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<SearchPoiResult> = emptyList(),
    val showSearchSheet: Boolean = false,
    val selectedPlace: SelectedPlaceDetail? = null,
    val selectedRouteOptionIndex: Int = 0,
    val isCalculatingRoute: Boolean = false,
    val activeRoute: CalculatedRoute? = null,
    val isNavigating: Boolean = false,
    val isCruiseSimulation: Boolean = false,
    val isHudMode: Boolean = false,
    val isHudMirrored: Boolean = false,
    val showMapSettingsSheet: Boolean = false,
    val showEngineArchitectureSheet: Boolean = false,
    val currentStepIndex: Int = 0,
    val routeProgressFraction: Float = 0f,
    val distanceToNextTurnMeters: Int = 0,
    val remainingDistanceMeters: Int = 0,
    val remainingDurationSeconds: Int = 0,
    val statusBanner: String = "纯净零广告 · 双引擎算路已就绪",
    val usedRamMb: Int = 38
)

class NavigationViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NavDatabase.getInstance(application)
    private val repository = NavRepository(database.navDao())
    private val navService = DualEngineNavService()
    val sensorTracker = GpsAndCompassTracker(application)

    private val _uiState = MutableStateFlow(
        NavigationUiState(
            preferredEngine = when {
                navService.isAmapKeyConfigured() -> NavEngineType.AMAP_LBS
                navService.isTencentKeyConfigured() -> NavEngineType.TENCENT_LBS
                else -> NavEngineType.AMAP_LBS
            },
            isAmapKeyConfigured = navService.isAmapKeyConfigured(),
            isTencentKeyConfigured = navService.isTencentKeyConfigured(),
            statusBanner = when {
                navService.isAmapKeyConfigured() -> "高德开放平台 API 已激活"
                navService.isTencentKeyConfigured() -> "腾讯位置服务 API 已激活"
                else -> "高德中文实景瓦片 + OSRM 路网运行中"
            }
        )
    )
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    val telemetry: StateFlow<TelemetryState> = sensorTracker.telemetry

    val savedPlaces: StateFlow<List<SavedPlaceEntity>> = repository.savedPlaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentRoutes: StateFlow<List<RouteHistoryEntity>> = repository.recentRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var cruiseJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureDefaultPresets()
            refreshMemoryFootprint()
        }
    }

    fun refreshMemoryFootprint() {
        val runtime = Runtime.getRuntime()
        val usedMb = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt().coerceAtLeast(18)
        _uiState.value = _uiState.value.copy(
            usedRamMb = usedMb,
            isAmapKeyConfigured = navService.isAmapKeyConfigured(),
            isTencentKeyConfigured = navService.isTencentKeyConfigured()
        )
    }

    fun setSheetDetent(detent: SheetDetent) {
        _uiState.value = _uiState.value.copy(sheetDetent = detent)
    }

    fun cycleSheetDetent() {
        val next = when (_uiState.value.sheetDetent) {
            SheetDetent.COLLAPSED -> SheetDetent.HALF
            SheetDetent.HALF -> SheetDetent.EXPANDED
            SheetDetent.EXPANDED -> SheetDetent.HALF
        }
        _uiState.value = _uiState.value.copy(sheetDetent = next)
    }

    fun toggle3dPitch() {
        _uiState.value = _uiState.value.copy(is3dPitchEnabled = !_uiState.value.is3dPitchEnabled)
    }

    fun toggleMapSettingsSheet(show: Boolean) {
        if (show) refreshMemoryFootprint()
        _uiState.value = _uiState.value.copy(showMapSettingsSheet = show)
    }

    fun toggleEngineArchitectureSheet(show: Boolean) {
        if (show) refreshMemoryFootprint()
        _uiState.value = _uiState.value.copy(showEngineArchitectureSheet = show)
    }

    fun onSearchQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = newQuery,
            showSearchSheet = newQuery.isNotBlank(),
            sheetDetent = if (newQuery.isNotBlank()) SheetDetent.EXPANDED else _uiState.value.sheetDetent
        )
    }

    fun searchByCategory(categoryKeyword: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = categoryKeyword,
            activeCategoryFilter = categoryKeyword,
            isSearching = true,
            showSearchSheet = true,
            sheetDetent = SheetDetent.EXPANDED
        )
        performSearch()
    }

    fun performSearch() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSearching = true,
                showSearchSheet = true,
                sheetDetent = SheetDetent.EXPANDED
            )
            val (results, msg) = navService.searchPlaces(
                query = query,
                centerWgs84 = telemetry.value.currentWgs84,
                preferredEngine = _uiState.value.preferredEngine
            )
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                searchResults = results,
                statusBanner = msg
            )
            refreshMemoryFootprint()
        }
    }

    fun dismissSearchSheet() {
        _uiState.value = _uiState.value.copy(
            showSearchSheet = false,
            searchQuery = "",
            activeCategoryFilter = "",
            sheetDetent = SheetDetent.HALF
        )
    }

    fun inspectPlace(
        title: String,
        address: String,
        category: String,
        wgs84Point: LatLngPoint,
        sourceLabel: String = "地图选点"
    ) {
        val gcj = CoordinateUtils.wgs84ToGcj02(wgs84Point.latitude, wgs84Point.longitude)
        val distMeters = CoordinateUtils.haversineDistanceMeters(telemetry.value.currentWgs84, wgs84Point).roundToInt()
        val estMins = max(2, (distMeters / 650.0).roundToInt())

        _uiState.value = _uiState.value.copy(
            sheetMode = IosSheetMode.PLACE_DETAIL,
            showSearchSheet = false,
            activeRoute = null,
            selectedPlace = SelectedPlaceDetail(
                title = title,
                subtitle = address,
                category = category,
                wgs84 = wgs84Point,
                gcj02 = gcj,
                distanceMeters = distMeters,
                estimatedDriveMinutes = estMins,
                sourceLabel = sourceLabel
            )
        )
    }

    fun dropPinOnMap(wgs84Point: LatLngPoint) {
        if (_uiState.value.isNavigating) return
        val gcj = CoordinateUtils.wgs84ToGcj02(wgs84Point.latitude, wgs84Point.longitude)
        val distMeters = CoordinateUtils.haversineDistanceMeters(telemetry.value.currentWgs84, wgs84Point).roundToInt()
        val distStr = if (distMeters >= 1000) String.format("%.1f 公里", distMeters / 1000.0) else "${distMeters} 米"
        inspectPlace(
            title = "放置的大头针",
            address = String.format("距您 %s · GCJ-02 (%.4f, %.4f)", distStr, gcj.latitude, gcj.longitude),
            category = "地图选点",
            wgs84Point = wgs84Point,
            sourceLabel = "手势放置大头针"
        )
    }

    fun closePlaceOrRouteBackToExplore() {
        stopCruiseSimulation()
        _uiState.value = _uiState.value.copy(
            sheetMode = IosSheetMode.EXPLORE,
            sheetDetent = SheetDetent.HALF,
            selectedPlace = null,
            activeRoute = null,
            isNavigating = false,
            isCruiseSimulation = false
        )
    }

    fun selectRouteOption(index: Int) {
        _uiState.value = _uiState.value.copy(selectedRouteOptionIndex = index)
    }

    fun selectEngine(engine: NavEngineType) {
        if (_uiState.value.preferredEngine == engine) return
        _uiState.value = _uiState.value.copy(preferredEngine = engine)
        val currentRoute = _uiState.value.activeRoute
        if (currentRoute != null) {
            planRouteTo(
                destName = currentRoute.destinationName,
                destAddress = currentRoute.destinationAddress,
                destWgs84 = currentRoute.destinationWgs84,
                recordHistory = false
            )
        }
    }

    fun selectTravelMode(mode: TravelMode) {
        if (_uiState.value.travelMode == mode) return
        _uiState.value = _uiState.value.copy(travelMode = mode)
        val currentRoute = _uiState.value.activeRoute
        val place = _uiState.value.selectedPlace

        val targetName = currentRoute?.destinationName ?: place?.title
        val targetAddr = currentRoute?.destinationAddress ?: place?.subtitle
        val targetPt = currentRoute?.destinationWgs84 ?: place?.wgs84

        if (targetName != null && targetAddr != null && targetPt != null) {
            val distMeters = CoordinateUtils.haversineDistanceMeters(telemetry.value.currentWgs84, targetPt)
            // 智能同城距离守护：防止用户在看跨省长途路线（如 1423公里）时切到「步行」出现 17575分钟的不合理画面
            when {
                mode == TravelMode.WALKING && distMeters > 15_000 -> {
                    planRouteTo(
                        destName = "王府井步行街",
                        destAddress = "北京市东城区王府井大街255号（已自动匹配适宜步行距离）",
                        destWgs84 = LatLngPoint(39.9143, 116.4114),
                        recordHistory = false
                    )
                }
                mode == TravelMode.CYCLING && distMeters > 50_000 -> {
                    planRouteTo(
                        destName = "故宫博物院（午门）",
                        destAddress = "北京市东城区景山前街4号（已自动匹配适宜骑行距离）",
                        destWgs84 = LatLngPoint(39.9163, 116.3972),
                        recordHistory = false
                    )
                }
                mode == TravelMode.TRANSIT && distMeters > 80_000 -> {
                    planRouteTo(
                        destName = "北京南站",
                        destAddress = "北京市丰台区永外大街车站路12号（已自动匹配同城地铁距离）",
                        destWgs84 = LatLngPoint(39.8651, 116.3785),
                        recordHistory = false
                    )
                }
                else -> {
                    planRouteTo(
                        destName = targetName,
                        destAddress = targetAddr,
                        destWgs84 = targetPt,
                        recordHistory = false
                    )
                }
            }
        }
    }

    fun planRouteTo(
        destName: String,
        destAddress: String,
        destWgs84: LatLngPoint,
        recordHistory: Boolean = true
    ) {
        stopCruiseSimulation()
        val gcj = CoordinateUtils.wgs84ToGcj02(destWgs84.latitude, destWgs84.longitude)
        val rawDistMeters = CoordinateUtils.haversineDistanceMeters(telemetry.value.currentWgs84, destWgs84).roundToInt()

        // 如果当前是步行模式，且用户点了一个超过 15 公里的跨城终点，或者反之，确保模式合理
        val effectiveMode = if (_uiState.value.travelMode == TravelMode.WALKING && rawDistMeters > 20_000) {
            TravelMode.DRIVING
        } else {
            _uiState.value.travelMode
        }

        val estMins = max(2, (rawDistMeters / 650.0).roundToInt())

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                travelMode = effectiveMode,
                isCalculatingRoute = true,
                showSearchSheet = false,
                isNavigating = false,
                sheetMode = IosSheetMode.ROUTE_PREVIEW,
                selectedRouteOptionIndex = 0,
                selectedPlace = SelectedPlaceDetail(
                    title = destName,
                    subtitle = destAddress,
                    category = "目的地",
                    wgs84 = destWgs84,
                    gcj02 = gcj,
                    distanceMeters = rawDistMeters,
                    estimatedDriveMinutes = estMins,
                    sourceLabel = _uiState.value.preferredEngine.displayName
                )
            )
            val originWgs = telemetry.value.currentWgs84
            val originName = "我的位置"
            val route = navService.calculateRoute(
                originWgs84 = originWgs,
                originName = originName,
                destWgs84 = destWgs84,
                destName = destName,
                destAddress = destAddress,
                mode = effectiveMode,
                preferredEngine = _uiState.value.preferredEngine
            )

            // 将定位朝向对齐到路线起始方向，使车标/行人标正对路线前进方向
            if (route.polylineWgs84.size >= 2) {
                val initialBearing = CoordinateUtils.bearingDegrees(
                    route.polylineWgs84[0],
                    route.polylineWgs84[1]
                )
                sensorTracker.updateSimulatedPosition(
                    point = route.polylineWgs84[0],
                    speedKmh = 0f,
                    bearing = initialBearing
                )
            }

            val firstStepDist = route.steps.firstOrNull()?.distanceMeters ?: route.totalDistanceMeters
            _uiState.value = _uiState.value.copy(
                isCalculatingRoute = false,
                activeRoute = route,
                currentStepIndex = 0,
                routeProgressFraction = 0f,
                distanceToNextTurnMeters = firstStepDist,
                remainingDistanceMeters = route.totalDistanceMeters,
                remainingDurationSeconds = route.totalDurationSeconds,
                statusBanner = route.engineNotice
            )

            if (recordHistory) {
                repository.recordRoute(
                    RouteHistoryEntity(
                        startName = originName,
                        destName = destName,
                        destAddress = destAddress,
                        destLat = destWgs84.latitude,
                        destLon = destWgs84.longitude,
                        travelMode = effectiveMode.label,
                        distanceMeters = route.totalDistanceMeters,
                        durationSeconds = route.totalDurationSeconds,
                        engineUsed = route.engineUsed.displayName
                    )
                )
            }
            refreshMemoryFootprint()
        }
    }

    fun startRealNavigation(enableCruiseDemo: Boolean = false) {
        val route = _uiState.value.activeRoute ?: return
        val firstStep = route.steps.firstOrNull()
        _uiState.value = _uiState.value.copy(
            sheetMode = IosSheetMode.ACTIVE_NAVIGATION,
            isNavigating = true,
            isCruiseSimulation = enableCruiseDemo,
            is3dPitchEnabled = true,
            currentStepIndex = 0,
            routeProgressFraction = 0f,
            distanceToNextTurnMeters = firstStep?.distanceMeters ?: route.totalDistanceMeters,
            remainingDistanceMeters = route.totalDistanceMeters,
            remainingDurationSeconds = route.totalDurationSeconds,
            statusBanner = if (enableCruiseDemo) "模拟巡航导航中" else "实时 GPS 导航中"
        )
        firstStep?.let {
            sensorTracker.speakInstruction("出发。${it.instruction}")
        }
        if (enableCruiseDemo) {
            startCruiseAlongPolyline(route)
        }
    }

    fun stopNavigation() {
        stopCruiseSimulation()
        val route = _uiState.value.activeRoute
        _uiState.value = _uiState.value.copy(
            sheetMode = if (route != null) IosSheetMode.ROUTE_PREVIEW else IosSheetMode.EXPLORE,
            isNavigating = false,
            isCruiseSimulation = false,
            isHudMode = false,
            routeProgressFraction = 0f,
            remainingDistanceMeters = route?.totalDistanceMeters ?: 0,
            remainingDurationSeconds = route?.totalDurationSeconds ?: 0,
            statusBanner = "已结束路线导航"
        )
        sensorTracker.updateSimulatedPosition(
            point = route?.originWgs84 ?: LatLngPoint(39.9087, 116.3975),
            speedKmh = 0f,
            bearing = 0f
        )
    }

    fun toggleHudMode() {
        _uiState.value = _uiState.value.copy(isHudMode = !_uiState.value.isHudMode)
    }

    fun toggleHudMirror() {
        _uiState.value = _uiState.value.copy(isHudMirrored = !_uiState.value.isHudMirrored)
    }

    fun toggleVoiceAnnouncements() {
        val enabled = sensorTracker.toggleVoice()
        _uiState.value = _uiState.value.copy(
            statusBanner = if (enabled) "语音播报已开启" else "语音播报已静音"
        )
    }

    fun bookmarkDestination(title: String, address: String, category: String, point: LatLngPoint) {
        viewModelScope.launch {
            repository.savePlace(
                SavedPlaceEntity(
                    title = title,
                    address = address,
                    category = category,
                    latitude = point.latitude,
                    longitude = point.longitude
                )
            )
            _uiState.value = _uiState.value.copy(statusBanner = "已添加到「个人收藏」")
        }
    }

    fun deleteSavedPlace(id: Int) {
        viewModelScope.launch {
            repository.deletePlace(id)
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteRoute(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.value = _uiState.value.copy(statusBanner = "已清除最近记录")
        }
    }

    private fun startCruiseAlongPolyline(route: CalculatedRoute) {
        cruiseJob?.cancel()
        val points = route.polylineWgs84
        if (points.size < 2) return
        cruiseJob = viewModelScope.launch {
            val totalPts = points.size
            var idx = 0
            val targetSpeedKmh = when (_uiState.value.travelMode) {
                TravelMode.DRIVING -> 58f
                TravelMode.TRANSIT -> 45f
                TravelMode.CYCLING -> 19f
                TravelMode.WALKING -> 5.5f
            }
            while (isActive && idx < totalPts) {
                val currentPt = points[idx]
                val nextPt = points[(idx + 1).coerceAtMost(totalPts - 1)]
                val brng = if (idx < totalPts - 1) {
                    CoordinateUtils.bearingDegrees(currentPt, nextPt)
                } else {
                    telemetry.value.bearingDegrees
                }
                sensorTracker.updateSimulatedPosition(
                    point = currentPt,
                    speedKmh = if (idx == totalPts - 1) 0f else targetSpeedKmh,
                    bearing = brng
                )

                val progressFraction = (idx.toFloat() / max(1, totalPts - 1).toFloat()).coerceIn(0f, 1f)
                val remDist = ((1f - progressFraction) * route.totalDistanceMeters).roundToInt().coerceAtLeast(0)
                val remSec = ((1f - progressFraction) * route.totalDurationSeconds).roundToInt().coerceAtLeast(0)

                val stepCount = route.steps.size
                val newStepIdx = if (stepCount > 0) {
                    (progressFraction * stepCount).toInt().coerceIn(0, stepCount - 1)
                } else {
                    0
                }
                val prevStepIdx = _uiState.value.currentStepIndex
                val activeStep = route.steps.getOrNull(newStepIdx)
                val distToTurn = if (activeStep != null) {
                    CoordinateUtils.haversineDistanceMeters(currentPt, activeStep.startPoint).roundToInt()
                } else {
                    remDist
                }

                if (newStepIdx != prevStepIdx && activeStep != null) {
                    sensorTracker.speakInstruction(activeStep.instruction)
                }

                _uiState.value = _uiState.value.copy(
                    currentStepIndex = newStepIdx,
                    routeProgressFraction = progressFraction,
                    distanceToNextTurnMeters = distToTurn,
                    remainingDistanceMeters = remDist,
                    remainingDurationSeconds = remSec
                )

                if (idx == totalPts - 1) {
                    sensorTracker.speakInstruction("已到达目的地：${route.destinationName}")
                    _uiState.value = _uiState.value.copy(
                        isCruiseSimulation = false,
                        routeProgressFraction = 1f,
                        statusBanner = "已到达终点：${route.destinationName}"
                    )
                    break
                }

                val stepIncrement = max(1, totalPts / 90)
                idx = (idx + stepIncrement).coerceAtMost(totalPts - 1)
                delay(420L)
            }
        }
    }

    private fun stopCruiseSimulation() {
        cruiseJob?.cancel()
        cruiseJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopCruiseSimulation()
        sensorTracker.release()
    }
}
