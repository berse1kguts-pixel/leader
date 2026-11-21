package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Typeface
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.BitmapDescriptor
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import com.example.BuildConfig
import com.example.data.CoordinateUtils
import com.example.data.LatLngPoint
import com.example.data.remote.CalculatedRoute
import com.example.data.remote.TransitStationInfo
import com.example.data.remote.TravelMode
import com.example.ui.SelectedPlaceDetail
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosSystemGray
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

import android.os.Build

/**
 * 检查是否已在 Secrets 面板中配置真实的高德 Android 3D SDK Key (AMAP_ANDROID_KEY)
 */
fun isAmapAndroidSdkKeyConfigured(): Boolean {
    val key = BuildConfig.AMAP_ANDROID_KEY.trim()
    return key.isNotEmpty() && !key.startsWith("YOUR_") && key.length >= 16
}

/**
 * 检测是否运行在云端/虚拟化安卓模拟器（Cuttlefish / Goldfish / Ranchu 等）中。
 */
fun isVirtualEmulatorEnvironment(): Boolean {
    val fingerprint = Build.FINGERPRINT.lowercase()
    val model = Build.MODEL.lowercase()
    val product = Build.PRODUCT.lowercase()
    val hardware = Build.HARDWARE.lowercase()
    val brand = Build.BRAND.lowercase()
    val device = Build.DEVICE.lowercase()
    val manufacturer = Build.MANUFACTURER.lowercase()

    return fingerprint.startsWith("generic") ||
        fingerprint.startsWith("unknown") ||
        fingerprint.contains("emulator") ||
        fingerprint.contains("test-keys") ||
        model.contains("google_sdk") ||
        model.contains("emulator") ||
        model.contains("android sdk built for") ||
        model.contains("cuttlefish") ||
        hardware.contains("goldfish") ||
        hardware.contains("ranchu") ||
        hardware.contains("cutf") ||
        product.contains("sdk_gphone") ||
        product.contains("cf_") ||
        product.contains("cuttlefish") ||
        product.contains("emulator") ||
        device.contains("vsoc") ||
        (brand.startsWith("generic") && device.startsWith("generic")) ||
        manufacturer.contains("genymotion")
}

/**
 * 智能 3D 地图入口：
 * - 支持在【高德官方原生 3D 地图 SDK (MapView / GLSurfaceView)】与【轻量高清底图】之间无缝切换。
 */
@Composable
fun PureNavMapHost(
    currentLocationWgs84: LatLngPoint,
    bearingDegrees: Float,
    speedKmh: Float,
    travelMode: TravelMode,
    selectedPlace: SelectedPlaceDetail?,
    activeRoute: CalculatedRoute?,
    isNavigating: Boolean,
    is3dPitchEnabled: Boolean,
    useNativeAmap3d: Boolean,
    mapStyle: MapTileStyle,
    bottomSheetReservedPadding: Dp,
    onOpenMapSettings: () -> Unit,
    onToggle3dPitch: () -> Unit,
    onSwitchToNative3d: () -> Unit = {},
    onFallbackToLightweight: () -> Unit = {},
    onDropPinOnMap: (LatLngPoint) -> Unit,
    nearbyTransitStations: List<TransitStationInfo> = emptyList(),
    selectedTransitStation: TransitStationInfo? = null,
    onSelectTransitStation: (TransitStationInfo) -> Unit = {},
    currentStepIndex: Int = 0,
    distanceToNextTurnMeters: Int = 129,
    modifier: Modifier = Modifier
) {
    val hasNativeKey = remember { isAmapAndroidSdkKeyConfigured() }
    var nativeInitErrorMsg by remember { mutableStateOf<String?>(null) }

    if (hasNativeKey && useNativeAmap3d && nativeInitErrorMsg == null) {
        AmapNative3dMapView(
            currentLocationWgs84 = currentLocationWgs84,
            bearingDegrees = bearingDegrees,
            speedKmh = speedKmh,
            travelMode = travelMode,
            selectedPlace = selectedPlace,
            activeRoute = activeRoute,
            isNavigating = isNavigating,
            is3dPitchEnabled = is3dPitchEnabled,
            mapStyle = mapStyle,
            bottomSheetReservedPadding = bottomSheetReservedPadding,
            onOpenMapSettings = onOpenMapSettings,
            onToggle3dPitch = onToggle3dPitch,
            onFallbackToLightweight = onFallbackToLightweight,
            onDropPinOnMap = onDropPinOnMap,
            onNativeInitError = { msg -> nativeInitErrorMsg = msg },
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            InteractiveMapCanvas(
                currentLocationWgs84 = currentLocationWgs84,
                bearingDegrees = bearingDegrees,
                speedKmh = speedKmh,
                travelMode = travelMode,
                selectedPlace = selectedPlace,
                activeRoute = activeRoute,
                isNavigating = isNavigating,
                is3dPitchEnabled = is3dPitchEnabled,
                mapStyle = mapStyle,
                bottomSheetReservedPadding = bottomSheetReservedPadding,
                onOpenMapSettings = onOpenMapSettings,
                onToggle3dPitch = onToggle3dPitch,
                onDropPinOnMap = onDropPinOnMap,
                nearbyTransitStations = nearbyTransitStations,
                selectedTransitStation = selectedTransitStation,
                onSelectTransitStation = onSelectTransitStation,
                currentStepIndex = currentStepIndex,
                distanceToNextTurnMeters = distanceToNextTurnMeters,
                modifier = Modifier.fillMaxSize()
            )

            val hintText = when {
                !hasNativeKey && is3dPitchEnabled && !isNavigating ->
                    "请在 Secrets 填入 AMAP_ANDROID_KEY 开启高德真 3D"
                hasNativeKey && !useNativeAmap3d && is3dPitchEnabled && !isNavigating ->
                    "安全底图 · 点此切换高德原生 3D"
                nativeInitErrorMsg != null && !isNavigating ->
                    nativeInitErrorMsg
                else -> null
            }
            if (hintText != null) {
                Surface(
                    color = Color.White.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 92.dp, start = 14.dp, end = 68.dp)
                        .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.12f))
                        .border(0.5.dp, Color.White, RoundedCornerShape(14.dp))
                        .then(
                            if (hasNativeKey && !useNativeAmap3d) {
                                Modifier.clickable { onSwitchToNative3d() }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(IosBlue, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = hintText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C1E)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 高德官方原生 Android 3D 地图 SDK (com.amap.api:3dmap) Compose 封装
 * 使用 GLSurfaceView 驱动的官方标准 MapView（相比 TextureMapView 避免 ANGLE/SwiftShader 模拟器黑屏问题）。
 */
@Composable
private fun AmapNative3dMapView(
    currentLocationWgs84: LatLngPoint,
    bearingDegrees: Float,
    speedKmh: Float,
    travelMode: TravelMode,
    selectedPlace: SelectedPlaceDetail?,
    activeRoute: CalculatedRoute?,
    isNavigating: Boolean,
    is3dPitchEnabled: Boolean,
    mapStyle: MapTileStyle,
    bottomSheetReservedPadding: Dp,
    onOpenMapSettings: () -> Unit,
    onToggle3dPitch: () -> Unit,
    onFallbackToLightweight: () -> Unit,
    onDropPinOnMap: (LatLngPoint) -> Unit,
    onNativeInitError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val pxScale = density.density

    var recenterTrigger by remember { mutableIntStateOf(0) }
    var isFollowingUser by remember { mutableStateOf(true) }
    val effectiveMode = activeRoute?.travelMode ?: travelMode

    // 视图与引擎就绪状态
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var aMapRef by remember { mutableStateOf<AMap?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var showGlSlowWarning by remember { mutableStateOf(false) }

    // 图标在引擎就绪后惰性创建，避免在 mapcore 初始化前调用 BitmapDescriptorFactory
    var userPuckIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var navArrowIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var originPinIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var destPinIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    var userMarker by remember { mutableStateOf<Marker?>(null) }
    var originMarker by remember { mutableStateOf<Marker?>(null) }
    var destMarker by remember { mutableStateOf<Marker?>(null) }
    var routeHaloPolyline by remember { mutableStateOf<Polyline?>(null) }
    var routeMainPolyline by remember { mutableStateOf<Polyline?>(null) }

    // 若 4.5 秒后云端模拟器的虚拟 GPU 仍未完成首帧回调，提示用户可一键切回轻量底图
    LaunchedEffect(mapViewRef, isMapLoaded) {
        if (mapViewRef != null && !isMapLoaded) {
            delay(4500L)
            if (!isMapLoaded) {
                showGlSlowWarning = true
            }
        } else {
            showGlSlowWarning = false
        }
    }

    // 严格成对管理 MapView (GLSurfaceView) 生命周期，并在附着窗口后安全恢复渲染
    DisposableEffect(lifecycleOwner, mapViewRef) {
        val mv = mapViewRef
        if (mv == null) {
            onDispose { }
        } else {
            var isResumed = false
            var isDestroyed = false
            val resumeSafely = {
                if (!isResumed && !isDestroyed) {
                    runCatching {
                        mv.onResume()
                        isResumed = true
                    }
                }
            }
            val observer = LifecycleEventObserver { _, event ->
                runCatching {
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            mv.post { resumeSafely() }
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            if (isResumed && !isDestroyed) {
                                mv.onPause()
                                isResumed = false
                            }
                        }
                        Lifecycle.Event.ON_DESTROY -> {
                            if (!isDestroyed) {
                                mv.onDestroy()
                                isDestroyed = true
                            }
                        }
                        else -> Unit
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                mv.post { resumeSafely() }
            }
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                runCatching {
                    if (isResumed && !isDestroyed) {
                        mv.onPause()
                        isResumed = false
                    }
                    if (!isDestroyed) {
                        mv.onDestroy()
                        isDestroyed = true
                    }
                }
            }
        }
    }

    // 1. 引擎加载完毕后配置 3D 建筑、手势、路况、图标与初始镜头
    LaunchedEffect(aMapRef, isMapLoaded) {
        val map = aMapRef ?: return@LaunchedEffect
        if (!isMapLoaded) return@LaunchedEffect
        runCatching {
            map.showBuildings(true)
            map.showMapText(true)
            map.isTrafficEnabled = true
            map.uiSettings.apply {
                isZoomControlsEnabled = false
                isMyLocationButtonEnabled = false
                isCompassEnabled = false
                isScaleControlsEnabled = false
                isTiltGesturesEnabled = true
                isRotateGesturesEnabled = true
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
            }
            map.setOnMapLongClickListener { gcjLatLng ->
                val wgs = CoordinateUtils.gcj02ToWgs84(gcjLatLng.latitude, gcjLatLng.longitude)
                onDropPinOnMap(wgs)
            }
            if (userPuckIcon == null) userPuckIcon = createIosBluePuckIcon(pxScale)
            if (navArrowIcon == null) navArrowIcon = createIosNavChevronIcon(pxScale)
            if (originPinIcon == null) originPinIcon = createCircleBadgeIcon("起", AndroidColor.rgb(52, 199, 89), pxScale)
            if (destPinIcon == null) destPinIcon = createCircleBadgeIcon("终", AndroidColor.rgb(255, 59, 48), pxScale)

            val startGcj = wgsToAmapLatLng(currentLocationWgs84)
            map.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(startGcj)
                        .zoom(if (is3dPitchEnabled) 17.8f else 15.8f)
                        .tilt(if (is3dPitchEnabled) 56f else 0f)
                        .bearing(0f)
                        .build()
                )
            )
        }
    }

    // 2. 同步地图样式（标准日间 / 夜间护眼 / 导航清爽 / 卫星实景）
    LaunchedEffect(aMapRef, isMapLoaded, mapStyle) {
        val map = aMapRef ?: return@LaunchedEffect
        if (!isMapLoaded) return@LaunchedEffect
        runCatching {
            map.mapType = when (mapStyle) {
                MapTileStyle.CN_STREETS -> AMap.MAP_TYPE_NORMAL
                MapTileStyle.CARTO_VOYAGER -> AMap.MAP_TYPE_NAVI
                MapTileStyle.CARTO_DARK -> AMap.MAP_TYPE_NIGHT
                MapTileStyle.OSM_STANDARD -> AMap.MAP_TYPE_SATELLITE
            }
            map.showBuildings(true)
        }
    }

    // 3. 同步用户位置标记与导航跟随视角
    LaunchedEffect(aMapRef, isMapLoaded, currentLocationWgs84, bearingDegrees, isNavigating, userPuckIcon, navArrowIcon) {
        val map = aMapRef ?: return@LaunchedEffect
        if (!isMapLoaded) return@LaunchedEffect
        val icon = if (isNavigating) navArrowIcon else userPuckIcon
        if (icon == null) return@LaunchedEffect
        runCatching {
            val gcj = wgsToAmapLatLng(currentLocationWgs84)
            val marker = userMarker
            if (marker == null) {
                userMarker = map.addMarker(
                    MarkerOptions()
                        .position(gcj)
                        .icon(icon)
                        .anchor(0.5f, 0.5f)
                        .setFlat(true)
                        .zIndex(30f)
                )
            } else {
                marker.position = gcj
                marker.setIcon(icon)
                marker.rotateAngle = if (isNavigating) (360f - bearingDegrees) % 360f else 0f
            }

            if (isNavigating) {
                map.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(gcj)
                            .zoom(
                                when (effectiveMode) {
                                    TravelMode.WALKING -> 18.4f
                                    TravelMode.CYCLING -> 18.0f
                                    TravelMode.TRANSIT -> 16.5f
                                    TravelMode.DRIVING -> 18.1f
                                }
                            )
                            .tilt(if (is3dPitchEnabled) 60f else 0f)
                            .bearing(bearingDegrees)
                            .build()
                    ),
                    240L,
                    null
                )
            }
        }
    }

    // 4. 同步路线折线与起/终点标记
    LaunchedEffect(aMapRef, isMapLoaded, activeRoute, selectedPlace, isNavigating, effectiveMode, originPinIcon, destPinIcon) {
        val map = aMapRef ?: return@LaunchedEffect
        if (!isMapLoaded) return@LaunchedEffect
        runCatching {
            routeHaloPolyline?.remove()
            routeMainPolyline?.remove()
            originMarker?.remove()
            destMarker?.remove()
            routeHaloPolyline = null
            routeMainPolyline = null
            originMarker = null
            destMarker = null

            if (activeRoute != null && activeRoute.polylineWgs84.size >= 2) {
                val gcjPoints = activeRoute.polylineWgs84.map { wgsToAmapLatLng(it) }
                val mainColor = when (effectiveMode) {
                    TravelMode.WALKING, TravelMode.CYCLING -> AndroidColor.rgb(52, 199, 89)
                    TravelMode.TRANSIT -> AndroidColor.rgb(10, 132, 255)
                    TravelMode.DRIVING -> AndroidColor.rgb(0, 122, 255)
                }
                val haloColor = when (effectiveMode) {
                    TravelMode.WALKING, TravelMode.CYCLING -> AndroidColor.rgb(20, 118, 52)
                    TravelMode.TRANSIT -> AndroidColor.rgb(0, 74, 158)
                    TravelMode.DRIVING -> AndroidColor.rgb(0, 68, 168)
                }

                routeHaloPolyline = map.addPolyline(
                    PolylineOptions()
                        .addAll(gcjPoints)
                        .width(12f * pxScale)
                        .color(haloColor)
                        .zIndex(9f)
                )
                routeMainPolyline = map.addPolyline(
                    PolylineOptions()
                        .addAll(gcjPoints)
                        .width(8.5f * pxScale)
                        .color(mainColor)
                        .zIndex(10f)
                )

                val oIcon = originPinIcon
                if (!isNavigating && oIcon != null) {
                    originMarker = map.addMarker(
                        MarkerOptions()
                            .position(wgsToAmapLatLng(activeRoute.originWgs84))
                            .icon(oIcon)
                            .anchor(0.5f, 0.5f)
                            .zIndex(20f)
                    )
                }
                val dIcon = destPinIcon
                if (dIcon != null) {
                    destMarker = map.addMarker(
                        MarkerOptions()
                            .position(wgsToAmapLatLng(activeRoute.destinationWgs84))
                            .icon(dIcon)
                            .anchor(0.5f, 0.5f)
                            .zIndex(22f)
                    )
                }
            } else if (selectedPlace != null) {
                val dIcon = destPinIcon
                if (dIcon != null) {
                    destMarker = map.addMarker(
                        MarkerOptions()
                            .position(wgsToAmapLatLng(selectedPlace.wgs84))
                            .icon(dIcon)
                            .anchor(0.5f, 0.5f)
                            .zIndex(22f)
                    )
                }
            }
        }
    }

    // 5. 响应 3D/2D 切换、路线全览、选点与定位回正镜头动画
    LaunchedEffect(aMapRef, isMapLoaded, is3dPitchEnabled, isNavigating, activeRoute, selectedPlace, recenterTrigger) {
        val map = aMapRef ?: return@LaunchedEffect
        if (!isMapLoaded || isNavigating) return@LaunchedEffect
        runCatching {
            when {
                activeRoute != null && !is3dPitchEnabled -> {
                    val builder = LatLngBounds.Builder()
                    activeRoute.polylineWgs84.forEach { builder.include(wgsToAmapLatLng(it)) }
                    builder.include(wgsToAmapLatLng(activeRoute.originWgs84))
                    builder.include(wgsToAmapLatLng(activeRoute.destinationWgs84))
                    val padH = (56 * pxScale).roundToInt()
                    val padTop = (110 * pxScale).roundToInt()
                    val padBottom = ((bottomSheetReservedPadding.value + 44f) * pxScale).roundToInt()
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngBoundsRect(
                            builder.build(),
                            padH,
                            padH,
                            padTop,
                            padBottom
                        ),
                        420L,
                        null
                    )
                }
                selectedPlace != null -> {
                    val target = wgsToAmapLatLng(selectedPlace.wgs84)
                    map.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(target)
                                .zoom(if (is3dPitchEnabled) 17.8f else 16.2f)
                                .tilt(if (is3dPitchEnabled) 58f else 0f)
                                .bearing(if (is3dPitchEnabled) 22f else 0f)
                                .build()
                        ),
                        420L,
                        null
                    )
                }
                else -> {
                    val target = wgsToAmapLatLng(currentLocationWgs84)
                    map.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(target)
                                .zoom(if (is3dPitchEnabled) 17.8f else 15.8f)
                                .tilt(if (is3dPitchEnabled) 58f else 0f)
                                .bearing(if (is3dPitchEnabled) 24f else 0f)
                                .build()
                        ),
                        450L,
                        null
                    )
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 使用官方标准 GLSurfaceView 驱动的 MapView（彻底规避 TextureMapView 在云端模拟器的黑屏问题）
        AndroidView(
            factory = { ctx ->
                runCatching {
                    val appCtx = ctx.applicationContext
                    MapsInitializer.updatePrivacyShow(appCtx, true, true)
                    MapsInitializer.updatePrivacyAgree(appCtx, true)
                    MapsInitializer.setApiKey(BuildConfig.AMAP_ANDROID_KEY.trim())
                    MapView(ctx).apply {
                        val mv = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        onCreate(null)
                        mapViewRef = mv
                        val map = mv.map
                        aMapRef = map
                        map?.apply {
                            mapType = AMap.MAP_TYPE_NORMAL
                            showBuildings(true)
                            showMapText(true)
                            isTrafficEnabled = true
                            val initTarget = wgsToAmapLatLng(currentLocationWgs84)
                            moveCamera(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(initTarget)
                                        .zoom(if (is3dPitchEnabled) 17.6f else 15.8f)
                                        .tilt(if (is3dPitchEnabled) 56f else 0f)
                                        .bearing(if (is3dPitchEnabled) 20f else 0f)
                                        .build()
                                )
                            )
                            setOnMapLoadedListener {
                                mv.post { isMapLoaded = true }
                            }
                        }
                    }
                }.getOrElse { err ->
                    val reason = if (err is UnsatisfiedLinkError || err.cause is UnsatisfiedLinkError) {
                        "当前模拟器暂不支持高德原生 3D 底层库"
                    } else {
                        "高德 3D 初始化异常：${err.message?.take(32) ?: "未知错误"}"
                    }
                    onNativeInitError(reason)
                    android.view.View(ctx)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("amap_native_3d_view")
        )

        // 若云端模拟器虚拟 GPU 无法渲染高德原生 OpenGL，提供一键切回高清底图
        if (showGlSlowWarning && !isNavigating) {
            Surface(
                color = Color.White.copy(alpha = 0.95f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 92.dp, start = 14.dp, end = 68.dp)
                    .shadow(10.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.22f))
                    .clickable { onFallbackToLightweight() }
                    .testTag("fallback_lightweight_map_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFFF9500), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "模拟器 GPU 未出图 · 点此切回高清底图",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E)
                    )
                }
            }
        }

        // ==================== 左上角：仅在导航态显示 Apple 车速表 + 路况条；路线预览显示轻量提示 ====================
        val speedoTopPadding = if (isNavigating) 134.dp else 48.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = speedoTopPadding, start = 14.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isNavigating) {
                if (activeRoute != null) {
                    Surface(
                        color = Color.White.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.12f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(IosBlue, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "路线全览 · 请点击「出发」",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C1C1E)
                            )
                        }
                    }
                }
            } else {
                when (effectiveMode) {
                    TravelMode.WALKING -> {
                        val estSteps = ((activeRoute?.totalDistanceMeters ?: 860) * 1.35).roundToInt()
                        Surface(
                            color = Color.White.copy(alpha = 0.92f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(62.dp)
                                .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.16f))
                                .border(0.5.dp, Color.White, CircleShape)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("$estSteps", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
                                Text("步", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF34C759))
                            }
                        }
                    }

                    TravelMode.CYCLING -> {
                        Surface(
                            color = Color.White.copy(alpha = 0.92f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(62.dp)
                                .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.16f))
                                .border(0.5.dp, Color.White, CircleShape)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (speedKmh >= 1f) "${speedKmh.roundToInt()}" else "16",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1C1E)
                                )
                                Text("km/h", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF34C759))
                            }
                        }
                    }

                    TravelMode.TRANSIT -> {
                        Surface(
                            color = Color.White.copy(alpha = 0.92f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.16f))
                                .border(0.5.dp, Color.White, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsSubway,
                                    contentDescription = "公交地铁",
                                    tint = IosBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    TravelMode.DRIVING -> {
                        Surface(
                            color = Color.White.copy(alpha = 0.92f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(62.dp)
                                .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.16f))
                                .border(0.5.dp, Color.White, CircleShape)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (speedKmh >= 1f) "${speedKmh.roundToInt()}" else "0",
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1C1E),
                                    letterSpacing = (-0.5).sp
                                )
                                Text("km/h", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = IosSystemGray)
                            }
                        }

                        if (activeRoute != null) {
                            Surface(
                                color = Color.White.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .width(12.dp)
                                    .height(110.dp)
                                    .shadow(6.dp, RoundedCornerShape(10.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp)
                                ) {
                                    val smoothWeight = activeRoute.smoothPercent.coerceAtLeast(10).toFloat()
                                    val slowWeight = activeRoute.slowPercent.coerceAtLeast(5).toFloat()
                                    val congWeight = activeRoute.congestedPercent.coerceAtLeast(3).toFloat()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(smoothWeight * 0.5f)
                                            .background(Color(0xFF34C759), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(congWeight)
                                            .background(Color(0xFFFF3B30))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(slowWeight)
                                            .background(Color(0xFFFF9500))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(smoothWeight * 0.5f)
                                            .background(Color(0xFF34C759), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==================== 右上角：纯正 iOS 18 苹果地图毛玻璃竖条工具胶囊（地图图层 + 定位 + 3D） ====================
        val rightTopPadding = if (isNavigating) 134.dp else 48.dp
        val glassSurfaceColor = if (mapStyle.isDarkStyle) {
            Color(0xFF1C1C1E).copy(alpha = 0.86f)
        } else {
            Color(0xFFFDFDFD).copy(alpha = 0.88f)
        }
        val primaryIconTint = if (mapStyle.isDarkStyle) Color.White else Color(0xFF1C1C1E)

        Surface(
            color = glassSurfaceColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = rightTopPadding, end = 12.dp)
                .width(44.dp)
                .shadow(10.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.14f))
                .border(0.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onOpenMapSettings() }
                        .testTag("switch_map_style_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = "选择地图",
                        tint = primaryIconTint.copy(alpha = 0.82f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                HorizontalDivider(
                    color = Color(0xFF3C3C43).copy(alpha = 0.16f),
                    thickness = 0.5.dp
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable {
                            isFollowingUser = true
                            recenterTrigger += 1
                        }
                        .testTag("recenter_map_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFollowingUser) Icons.Default.NearMe else Icons.Outlined.NearMe,
                        contentDescription = "定位回正",
                        tint = IosBlue,
                        modifier = Modifier.size(19.dp)
                    )
                }

                HorizontalDivider(
                    color = Color(0xFF3C3C43).copy(alpha = 0.16f),
                    thickness = 0.5.dp
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onToggle3dPitch() }
                        .testTag("toggle_3d_pitch_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (is3dPitchEnabled) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(IosBlue, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "3D",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Text(
                            text = "3D",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryIconTint.copy(alpha = 0.80f)
                        )
                    }
                }
            }
        }

        // ==================== 右下角：Apple Maps 紧凑型天气与空气质量毛玻璃微胶囊 ====================
        if (!isNavigating) {
            Surface(
                color = glassSurfaceColor,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = bottomSheetReservedPadding + 10.dp)
                    .shadow(6.dp, RoundedCornerShape(10.dp), ambientColor = Color.Black.copy(alpha = 0.12f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "☀ 23°",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryIconTint
                    )
                    Surface(
                        color = Color(0xFF34C759).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "优",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF248A3D),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun wgsToAmapLatLng(wgs: LatLngPoint): LatLng {
    val gcj = CoordinateUtils.wgs84ToGcj02(wgs.latitude, wgs.longitude)
    return LatLng(gcj.latitude, gcj.longitude)
}

private fun createIosBluePuckIcon(density: Float): BitmapDescriptor {
    val size = (44 * density).roundToInt().coerceAtLeast(32)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val c = size / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = AndroidColor.argb(56, 0, 122, 255)
    canvas.drawCircle(c, c, size * 0.48f, paint)

    paint.color = AndroidColor.argb(46, 0, 0, 0)
    canvas.drawCircle(c, c + 1.5f * density, size * 0.28f, paint)

    paint.color = AndroidColor.WHITE
    canvas.drawCircle(c, c, size * 0.27f, paint)

    paint.color = AndroidColor.rgb(0, 122, 255)
    canvas.drawCircle(c, c, size * 0.20f, paint)

    return BitmapDescriptorFactory.fromBitmap(bmp)
}

private fun createIosNavChevronIcon(density: Float): BitmapDescriptor {
    val size = (54 * density).roundToInt().coerceAtLeast(42)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val c = size / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = AndroidColor.argb(45, 0, 122, 255)
    canvas.drawCircle(c, c, size * 0.46f, paint)

    paint.color = AndroidColor.WHITE
    canvas.drawCircle(c, c, size * 0.34f, paint)

    paint.color = AndroidColor.rgb(0, 122, 255)
    canvas.drawCircle(c, c, size * 0.28f, paint)

    val s = size * 0.16f
    val path = AndroidPath().apply {
        moveTo(c, c - s * 1.15f)
        lineTo(c + s * 0.88f, c + s * 0.78f)
        lineTo(c, c + s * 0.28f)
        lineTo(c - s * 0.88f, c + s * 0.78f)
        close()
    }
    paint.color = AndroidColor.WHITE
    canvas.drawPath(path, paint)

    return BitmapDescriptorFactory.fromBitmap(bmp)
}

private fun createCircleBadgeIcon(label: String, fillColor: Int, density: Float): BitmapDescriptor {
    val size = (34 * density).roundToInt().coerceAtLeast(28)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val c = size / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = AndroidColor.argb(55, 0, 0, 0)
    canvas.drawCircle(c, c + 1.5f * density, size * 0.42f, paint)

    paint.color = AndroidColor.WHITE
    canvas.drawCircle(c, c, size * 0.44f, paint)

    paint.color = fillColor
    canvas.drawCircle(c, c, size * 0.36f, paint)

    paint.color = AndroidColor.WHITE
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = 11.5f * density
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    val textY = c - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(label, c, textY, paint)

    return BitmapDescriptorFactory.fromBitmap(bmp)
}
