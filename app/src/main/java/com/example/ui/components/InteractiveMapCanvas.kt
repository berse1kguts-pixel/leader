package com.example.ui.components

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CoordinateUtils
import com.example.data.LatLngPoint
import com.example.data.remote.CalculatedRoute
import com.example.data.remote.NavStep
import com.example.data.remote.TravelMode
import com.example.ui.SelectedPlaceDetail
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldStatus
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosRed
import com.example.ui.theme.IosSystemGray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan

enum class MapTileStyle(
    val label: String,
    val subtitle: String,
    val shortBadge: String,
    val usesGcj02Projection: Boolean,
    val bgFallbackColor: Color,
    val horizonSkyTopColor: Color,
    val routePrimaryColor: Color,
    val routeHaloColor: Color,
    val isDarkStyle: Boolean
) {
    CN_STREETS(
        label = "高德3D实景路网",
        subtitle = "真实高德中文瓦片 + 3D车道级路线与建筑",
        shortBadge = "高德3D",
        usesGcj02Projection = true,
        bgFallbackColor = Color(0xFFEAEEF3),
        horizonSkyTopColor = Color(0xFFBDE0FE),
        routePrimaryColor = Color(0xFF1677FF),
        routeHaloColor = Color(0xFF0045B5),
        isDarkStyle = false
    ),
    CARTO_VOYAGER(
        label = "清爽日间",
        subtitle = "突出城市主干道、立体高架与路名",
        shortBadge = "日间",
        usesGcj02Projection = false,
        bgFallbackColor = Color(0xFFEDF1F5),
        horizonSkyTopColor = Color(0xFFC8E4FF),
        routePrimaryColor = IosBlue,
        routeHaloColor = Color(0xFF003B99),
        isDarkStyle = false
    ),
    CARTO_DARK(
        label = "夜间护眼",
        subtitle = "低眩光深空夜航、发光车道与流光建筑",
        shortBadge = "夜间",
        usesGcj02Projection = false,
        bgFallbackColor = Color(0xFF141C2B),
        horizonSkyTopColor = Color(0xFF09101D),
        routePrimaryColor = Color(0xFF2E9BFF),
        routeHaloColor = Color(0xFF004099),
        isDarkStyle = true
    ),
    OSM_STANDARD(
        label = "开源全览",
        subtitle = "OpenStreetMap 详尽社区路网要素图层",
        shortBadge = "OSM",
        usesGcj02Projection = false,
        bgFallbackColor = Color(0xFFECEFF4),
        horizonSkyTopColor = Color(0xFFD0E6FA),
        routePrimaryColor = IosBlue,
        routeHaloColor = Color(0xFF003899),
        isDarkStyle = false
    )
}

private data class TileRequestKey(
    val style: MapTileStyle,
    val z: Int,
    val x: Int,
    val y: Int
) {
    val cacheKey: String get() = "${style.name}/$z/$x/$y"
}

private object MapTileLoader {
    private val cache = LruCache<String, ImageBitmap>(140)
    private val inFlight = HashSet<String>()
    private val loaderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _tileVersion = MutableStateFlow(0)
    val tileVersion: StateFlow<Int> = _tileVersion.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    @Synchronized
    fun get(key: String): ImageBitmap? = cache.get(key)

    @Synchronized
    private fun markInFlight(key: String): Boolean {
        if (cache.get(key) != null || inFlight.contains(key)) return false
        inFlight.add(key)
        return true
    }

    @Synchronized
    private fun put(key: String, bitmap: ImageBitmap) {
        cache.put(key, bitmap)
        inFlight.remove(key)
    }

    @Synchronized
    private fun clearInFlight(key: String) {
        inFlight.remove(key)
    }

    fun requestTileAsync(req: TileRequestKey) {
        val key = req.cacheKey
        if (!markInFlight(key)) return
        loaderScope.launch {
            try {
                val bmp = downloadTile(req)
                if (bmp != null) {
                    put(key, bmp)
                    _tileVersion.value += 1
                }
            } finally {
                clearInFlight(key)
            }
        }
    }

    private fun downloadTile(req: TileRequestKey): ImageBitmap? {
        val urls = buildCandidateUrls(req.style, req.z, req.x, req.y)
        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) PureNav/6.0")
                    .header("Accept", "image/webp,image/apng,image/png,image/*,*/*;q=0.8")
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bmp != null) {
                                return bmp.asImageBitmap()
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Try next mirror
            }
        }
        return null
    }

    private fun buildCandidateUrls(style: MapTileStyle, z: Int, x: Int, y: Int): List<String> {
        val sub = ((x + y) % 4 + 4) % 4 + 1
        val abc = charArrayOf('a', 'b', 'c')[((x + y) % 3 + 3) % 3]
        return when (style) {
            MapTileStyle.CN_STREETS -> listOf(
                "https://webrd0$sub.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=2&style=8&x=$x&y=$y&z=$z",
                "https://wprd0$sub.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x=$x&y=$y&z=$z",
                "https://$abc.basemaps.cartocdn.com/rastertiles/voyager/$z/$x/$y@2x.png"
            )
            MapTileStyle.CARTO_VOYAGER -> listOf(
                "https://$abc.basemaps.cartocdn.com/rastertiles/voyager/$z/$x/$y@2x.png",
                "https://webrd0$sub.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=2&style=8&x=$x&y=$y&z=$z"
            )
            MapTileStyle.OSM_STANDARD -> listOf(
                "https://tile.openstreetmap.org/$z/$x/$y.png",
                "https://$abc.basemaps.cartocdn.com/rastertiles/voyager/$z/$x/$y@2x.png"
            )
            MapTileStyle.CARTO_DARK -> listOf(
                "https://$abc.basemaps.cartocdn.com/dark_all/$z/$x/$y@2x.png",
                "https://$abc.basemaps.cartocdn.com/rastertiles/dark_all/$z/$x/$y.png"
            )
        }
    }
}

@Composable
fun InteractiveMapCanvas(
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
    onDropPinOnMap: (LatLngPoint) -> Unit,
    currentStepIndex: Int = 0,
    distanceToNextTurnMeters: Int = 129,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val baseTileSizePx = with(density) { 256.dp.toPx() }
    val tileVersion by MapTileLoader.tileVersion.collectAsState()

    var continuousZoom by remember { mutableFloatStateOf(16.2f) }
    var cameraCenterWgs84 by remember { mutableStateOf(currentLocationWgs84) }
    var trackingState by remember { mutableIntStateOf(1) }
    var viewportSize by remember { mutableStateOf(IntSize(1080, 1920)) }
    var isTrafficOverlayEnabled by remember { mutableStateOf(true) }

    val effectiveMode = activeRoute?.travelMode ?: travelMode

    // 根据出行方式调整最佳 3D 俯仰角：驾车 48°、骑行 44°、步行 40°（看清周边人行步道与街角店铺）、公交地铁 32°
    val targetPitchX = when {
        !is3dPitchEnabled -> 0f
        effectiveMode == TravelMode.WALKING -> 40f
        effectiveMode == TravelMode.CYCLING -> 44f
        effectiveMode == TravelMode.TRANSIT -> 30f
        isNavigating -> 50f
        else -> 46f
    }
    val animatedPitchX by animateFloatAsState(
        targetValue = targetPitchX,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessLow),
        label = "pitchXAnim"
    )
    val pitchFactor = (animatedPitchX / 48f).coerceIn(0f, 1.1f)

    val infiniteTransition = rememberInfiniteTransition(label = "realRouteFlow")
    val laneFlowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laneFlowPhase"
    )

    fun toLayerCoord(wgs: LatLngPoint): LatLngPoint {
        return if (mapStyle.usesGcj02Projection) {
            CoordinateUtils.wgs84ToGcj02(wgs.latitude, wgs.longitude)
        } else {
            wgs
        }
    }

    LaunchedEffect(selectedPlace, activeRoute, isNavigating, effectiveMode) {
        if (isNavigating) {
            trackingState = 2
            cameraCenterWgs84 = currentLocationWgs84
            continuousZoom = when (effectiveMode) {
                TravelMode.WALKING -> 17.2f
                TravelMode.CYCLING -> 16.8f
                TravelMode.TRANSIT -> 15.2f
                TravelMode.DRIVING -> 16.5f
            }
        } else if (activeRoute != null) {
            trackingState = 0
            if (is3dPitchEnabled && activeRoute.totalDistanceMeters > 12_000) {
                cameraCenterWgs84 = activeRoute.originWgs84
                continuousZoom = 16.0f
            } else {
                val midLat = (activeRoute.originWgs84.latitude + activeRoute.destinationWgs84.latitude) / 2.0
                val midLon = (activeRoute.originWgs84.longitude + activeRoute.destinationWgs84.longitude) / 2.0
                cameraCenterWgs84 = LatLngPoint(midLat, midLon)
                val dist = activeRoute.totalDistanceMeters
                continuousZoom = when {
                    dist > 80_000 -> 9.8f
                    dist > 30_000 -> 11.4f
                    dist > 12_000 -> 13.2f
                    dist > 4_000 -> 14.6f
                    dist > 1_500 -> 15.8f
                    else -> 16.6f
                }
            }
        } else if (selectedPlace != null) {
            trackingState = 0
            cameraCenterWgs84 = selectedPlace.wgs84
            continuousZoom = 16.2f
        }
    }

    LaunchedEffect(currentLocationWgs84, isNavigating, trackingState) {
        if (isNavigating || trackingState >= 1) {
            cameraCenterWgs84 = currentLocationWgs84
        }
    }

    val tileZ = floor(continuousZoom).toInt().coerceIn(3, 18)
    val zoomScale = 2f.pow(continuousZoom - tileZ)
    val scaledTilePx = baseTileSizePx * zoomScale
    val n = 2.0.pow(tileZ.toDouble())

    val cameraLayerCoord = toLayerCoord(cameraCenterWgs84)
    val centerTileX = lonToTileX(cameraLayerCoord.longitude, n)
    val centerTileY = latToTileY(cameraLayerCoord.latitude, n)

    val visibleTiles = remember(
        mapStyle,
        tileZ,
        centerTileX.roundToInt(),
        centerTileY.roundToInt(),
        viewportSize,
        scaledTilePx.roundToInt(),
        is3dPitchEnabled
    ) {
        val w = max(1, viewportSize.width).toFloat()
        val h = max(1, viewportSize.height).toFloat()
        val extraSpan = if (is3dPitchEnabled) 2.8f else 1.4f
        val halfCols = (w / (2f * scaledTilePx)) + extraSpan
        val halfRows = (h / (2f * scaledTilePx)) + extraSpan
        val minX = floor(centerTileX - halfCols).toInt()
        val maxX = floor(centerTileX + halfCols).toInt()
        val minY = floor(centerTileY - halfRows).toInt()
        val maxY = floor(centerTileY + halfRows).toInt()
        val maxIdx = n.toInt() - 1

        val list = ArrayList<TileRequestKey>()
        for (ty in minY..maxY) {
            if (ty in 0..maxIdx) {
                for (tx in minX..maxX) {
                    val wrappedX = ((tx % n.toInt()) + n.toInt()) % n.toInt()
                    list.add(TileRequestKey(mapStyle, tileZ, wrappedX, ty))
                }
            }
        }
        list
    }

    LaunchedEffect(visibleTiles) {
        visibleTiles.forEach { req ->
            MapTileLoader.requestTileAsync(req)
        }
    }

    fun projectWgs84ToScreen(wgsPt: LatLngPoint, widthPx: Float, heightPx: Float): Offset {
        val layerPt = toLayerCoord(wgsPt)
        val ptx = lonToTileX(layerPt.longitude, n)
        val pty = latToTileY(layerPt.latitude, n)
        val anchorY = heightPx * (0.50f + 0.16f * pitchFactor)
        val sx = (widthPx / 2f) + ((ptx - centerTileX) * scaledTilePx).toFloat()
        val sy = anchorY + ((pty - centerTileY) * scaledTilePx).toFloat()
        return Offset(sx, sy)
    }

    val cardinalPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(mapStyle.bgFallbackColor)
            .onSizeChanged { viewportSize = it }
            .pointerInput(mapStyle, continuousZoom, cameraCenterWgs84, pitchFactor) {
                detectTapGestures(
                    onDoubleTap = {
                        continuousZoom = (continuousZoom + 1.0f).coerceAtMost(18.0f)
                    },
                    onLongPress = { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val currentZ = floor(continuousZoom).toInt().coerceIn(3, 18)
                        val curScale = 2f.pow(continuousZoom - currentZ)
                        val curTilePx = baseTileSizePx * curScale
                        val curN = 2.0.pow(currentZ.toDouble())
                        val curLayerPt = toLayerCoord(cameraCenterWgs84)
                        val cTx = lonToTileX(curLayerPt.longitude, curN)
                        val cTy = latToTileY(curLayerPt.latitude, curN)
                        val anchorY = h * (0.50f + 0.16f * pitchFactor)

                        val tapTileX = cTx + (tapOffset.x - w / 2f) / curTilePx
                        val tapTileY = (cTy + (tapOffset.y - anchorY) / curTilePx)
                            .coerceIn(0.05, curN - 0.05)

                        val tapLon = tileXToLon(tapTileX, curN)
                        val tapLat = tileYToLat(tapTileY, curN)
                        val wgsPoint = if (mapStyle.usesGcj02Projection) {
                            CoordinateUtils.gcj02ToWgs84(tapLat, tapLon)
                        } else {
                            LatLngPoint(tapLat, tapLon)
                        }
                        onDropPinOnMap(wgsPoint)
                    }
                )
            }
            .pointerInput(mapStyle) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    if (pan.x != 0f || pan.y != 0f) {
                        trackingState = 0
                        val currentZ = floor(continuousZoom).toInt().coerceIn(3, 18)
                        val curScale = 2f.pow(continuousZoom - currentZ)
                        val curTilePx = baseTileSizePx * curScale
                        val curN = 2.0.pow(currentZ.toDouble())

                        val curLayerPt = toLayerCoord(cameraCenterWgs84)
                        val newTileX = lonToTileX(curLayerPt.longitude, curN) - (pan.x / curTilePx)
                        val newTileY = (latToTileY(curLayerPt.latitude, curN) - (pan.y / curTilePx))
                            .coerceIn(0.05, curN - 0.05)

                        val newLon = tileXToLon(newTileX, curN)
                        val newLat = tileYToLat(newTileY, curN)
                        cameraCenterWgs84 = if (mapStyle.usesGcj02Projection) {
                            CoordinateUtils.gcj02ToWgs84(newLat, newLon)
                        } else {
                            LatLngPoint(newLat, newLon)
                        }
                    }
                    if (zoomChange != 1f && zoomChange > 0.2f) {
                        val zoomDelta = log2(zoomChange)
                        continuousZoom = (continuousZoom + zoomDelta).coerceIn(4.0f, 18.0f)
                    }
                }
            }
    ) {
        // ==================== 1. 真实高德/OSM瓦片底图 + 模式专属 3D 路线渲染层 ====================
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0.66f)
                    rotationX = animatedPitchX
                    val overscanScale = 1f + pitchFactor * 0.62f
                    scaleX = overscanScale
                    scaleY = overscanScale * (1f + pitchFactor * 0.18f)
                    cameraDistance = 11f * density.density
                }
                .testTag("interactive_map_canvas")
        ) {
            val currentVersion = tileVersion
            val w = size.width
            val h = size.height
            val anchorY = h * (0.50f + 0.16f * pitchFactor)

            draw2dProceduralCityVectorMap(
                w = w,
                h = h,
                anchorY = anchorY,
                centerLat = cameraLayerCoord.latitude,
                centerLon = cameraLayerCoord.longitude,
                n = n,
                centerTileX = centerTileX,
                centerTileY = centerTileY,
                scaledTilePx = scaledTilePx,
                isDarkStyle = mapStyle.isDarkStyle
            )

            // 绘制真实高德中文地图瓦片 / OSM 瓦片
            val extraSpan = if (animatedPitchX > 5f) 2.8f else 1.4f
            val halfCols = (w / (2f * scaledTilePx)) + extraSpan
            val halfRows = (h / (2f * scaledTilePx)) + extraSpan
            val minTileX = floor(centerTileX - halfCols).toInt()
            val maxTileX = floor(centerTileX + halfCols).toInt()
            val minTileY = floor(centerTileY - halfRows).toInt()
            val maxTileY = floor(centerTileY + halfRows).toInt()
            val maxTileIndex = n.toInt() - 1

            for (tx in minTileX..maxTileX) {
                for (ty in minTileY..maxTileY) {
                    if (ty in 0..maxTileIndex) {
                        val wrappedX = ((tx % n.toInt()) + n.toInt()) % n.toInt()
                        val key = "${mapStyle.name}/$tileZ/$wrappedX/$ty"

                        val leftPx = (w / 2f) + ((tx - centerTileX) * scaledTilePx).toFloat()
                        val topPx = anchorY + ((ty - centerTileY) * scaledTilePx).toFloat()
                        val rightPx = (w / 2f) + ((tx + 1 - centerTileX) * scaledTilePx).toFloat()
                        val bottomPx = anchorY + ((ty + 1 - centerTileY) * scaledTilePx).toFloat()

                        val dstX = leftPx.roundToInt()
                        val dstY = topPx.roundToInt()
                        val dstW = max(1, rightPx.roundToInt() - dstX)
                        val dstH = max(1, bottomPx.roundToInt() - dstY)

                        val bmp = MapTileLoader.get(key)
                        if (bmp != null) {
                            drawImage(
                                image = bmp,
                                dstOffset = IntOffset(dstX, dstY),
                                dstSize = IntSize(dstW, dstH),
                                alpha = 1.0f,
                                filterQuality = FilterQuality.Medium
                            )
                        } else {
                            val parentKey = "${mapStyle.name}/${tileZ - 1}/${wrappedX / 2}/${ty / 2}"
                            val parentBmp = if (tileZ > 3) MapTileLoader.get(parentKey) else null
                            if (parentBmp != null) {
                                val subHalfW = parentBmp.width / 2
                                val subHalfH = parentBmp.height / 2
                                val srcX = (wrappedX % 2) * subHalfW
                                val srcY = (ty % 2) * subHalfH
                                drawImage(
                                    image = parentBmp,
                                    srcOffset = IntOffset(srcX, srcY),
                                    srcSize = IntSize(subHalfW, subHalfH),
                                    dstOffset = IntOffset(dstX, dstY),
                                    dstSize = IntSize(dstW, dstH),
                                    alpha = 1.0f,
                                    filterQuality = FilterQuality.Low
                                )
                            }
                        }
                    }
                }
            }

            if (animatedPitchX > 8f && continuousZoom >= 14.2f) {
                drawRealCoord3dBuildings(
                    w = w,
                    h = h,
                    anchorY = anchorY,
                    centerLat = cameraLayerCoord.latitude,
                    centerLon = cameraLayerCoord.longitude,
                    n = n,
                    centerTileX = centerTileX,
                    centerTileY = centerTileY,
                    scaledTilePx = scaledTilePx,
                    pitchFactor = pitchFactor,
                    isDarkStyle = mapStyle.isDarkStyle
                )
            }

            // ==================== 1d. 四大出行模式专属真实路线绘制 ====================
            if (activeRoute != null && activeRoute.polylineWgs84.size >= 2) {
                val screenPts = activeRoute.polylineWgs84.map { pt ->
                    projectWgs84ToScreen(pt, w, h)
                }

                val routePath = Path()
                screenPts.forEachIndexed { index, pos ->
                    if (index == 0) routePath.moveTo(pos.x, pos.y) else routePath.lineTo(pos.x, pos.y)
                }

                when (effectiveMode) {
                    TravelMode.WALKING -> {
                        // 【高德/iOS 步行专属视觉】：绝不画汽车黑柏油马路！而是绘制「翡翠青人行步道光晕 + 动态向前流动的白色与碧绿步点圆珠胶囊」
                        drawPath(
                            path = routePath,
                            color = Color(0xFF10B981).copy(alpha = 0.28f),
                            style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = routePath,
                            color = Color(0xFF059669),
                            style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        // 动态步点圆珠脉冲线
                        drawPath(
                            path = routePath,
                            color = Color(0xFFA7F3D0),
                            style = Stroke(
                                width = 6.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(2.dp.toPx(), 13.dp.toPx()),
                                    -laneFlowPhase * 15.dp.toPx()
                                )
                            )
                        )
                        drawPath(
                            path = routePath,
                            color = Color.White,
                            style = Stroke(
                                width = 4.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(1.5.dp.toPx(), 13.5.dp.toPx()),
                                    -laneFlowPhase * 15.dp.toPx()
                                )
                            )
                        )
                    }

                    TravelMode.CYCLING -> {
                        // 【高德/iOS 自行车专属视觉】：清爽薄荷绿非机动车间道 + 破风白色流光箭头，不含四车道机动柏油路
                        drawPath(
                            path = routePath,
                            color = Color(0xFF047857),
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = routePath,
                            color = Color(0xFF10B981),
                            style = Stroke(width = 11.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = routePath,
                            color = Color(0xFF6EE7B7).copy(alpha = 0.65f),
                            style = Stroke(
                                width = 5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(14.dp.toPx(), 12.dp.toPx()),
                                    -laneFlowPhase * 26.dp.toPx()
                                )
                            )
                        )
                    }

                    TravelMode.TRANSIT -> {
                        // 【公交地铁专属视觉】：城市轨道交通双轨线 + 白色枕木刻度 + 换乘站点圆环节点
                        drawPath(
                            path = routePath,
                            color = Color(0xFF0E7490),
                            style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = routePath,
                            color = Color(0xFF06B6D4),
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = routePath,
                            color = Color.White.copy(alpha = 0.80f),
                            style = Stroke(
                                width = 6.dp.toPx(),
                                cap = StrokeCap.Butt,
                                join = StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(3.dp.toPx(), 11.dp.toPx()),
                                    0f
                                )
                            )
                        )
                        // 绘制沿途地铁站点白色圆环
                        val stationStride = max(1, screenPts.size / 6)
                        for (i in stationStride until screenPts.size - 1 step stationStride) {
                            val stPt = screenPts[i]
                            drawCircle(color = Color(0xFF0E7490), radius = 7.dp.toPx(), center = stPt)
                            drawCircle(color = Color.White, radius = 4.5.dp.toPx(), center = stPt)
                        }
                    }

                    TravelMode.DRIVING -> {
                        // 【高德 3D 车道级驾车专属视觉】：三车道深色柏油路基 + 白色虚线分道线 + 双色高德蓝光带 + 实时路况彩虹叠层
                        val isLaneLevelZoom = continuousZoom >= 14.5f && animatedPitchX > 8f
                        if (isLaneLevelZoom) {
                            drawPath(
                                path = routePath,
                                color = Color.White.copy(alpha = 0.92f),
                                style = Stroke(width = 38.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                path = routePath,
                                color = Color(0xFF1D2939).copy(alpha = 0.94f),
                                style = Stroke(width = 33.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                path = routePath,
                                color = Color.White.copy(alpha = 0.45f),
                                style = Stroke(
                                    width = 19.dp.toPx(),
                                    cap = StrokeCap.Butt,
                                    join = StrokeJoin.Round,
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(18.dp.toPx(), 16.dp.toPx()),
                                        -laneFlowPhase * 34.dp.toPx()
                                    )
                                )
                            )
                            drawPath(
                                path = routePath,
                                color = Color(0xFF1D2939),
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        drawPath(
                            path = routePath,
                            color = mapStyle.routeHaloColor,
                            style = Stroke(
                                width = if (isLaneLevelZoom) 15.dp.toPx() else 13.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        drawPath(
                            path = routePath,
                            color = mapStyle.routePrimaryColor,
                            style = Stroke(
                                width = if (isLaneLevelZoom) 11.5.dp.toPx() else 9.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // 仅驾车模式叠加实时机动车红黄绿拥堵路况
                        if (isTrafficOverlayEnabled && screenPts.size >= 4) {
                            val totalSegs = screenPts.size - 1
                            for (i in 0 until totalSegs) {
                                val stepIdx = (i * activeRoute.steps.size / max(1, totalSegs))
                                    .coerceIn(0, (activeRoute.steps.size - 1).coerceAtLeast(0))
                                val cond = activeRoute.steps.getOrNull(stepIdx)?.trafficCondition
                                val trafficColor = when (cond?.name) {
                                    "CONGESTED" -> Color(0xFFEF4444)
                                    "SLOW" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF10B981)
                                }
                                if (cond?.name == "CONGESTED" || cond?.name == "SLOW" || !isLaneLevelZoom) {
                                    drawLine(
                                        color = trafficColor,
                                        start = screenPts[i],
                                        end = screenPts[i + 1],
                                        strokeWidth = if (isLaneLevelZoom) 8.5.dp.toPx() else 7.5.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                }

                // 驾车与骑行模式绘制流动导流箭头
                if (effectiveMode == TravelMode.DRIVING || effectiveMode == TravelMode.CYCLING) {
                    val stepStride = max(1, screenPts.size / 14)
                    for (i in 0 until screenPts.size - 1 step stepStride) {
                        val p1 = screenPts[i]
                        val p2 = screenPts[i + 1]
                        val dx = p2.x - p1.x
                        val dy = p2.y - p1.y
                        val segLen = sqrt(dx * dx + dy * dy)
                        if (segLen > 14f) {
                            val cx = p1.x + dx * laneFlowPhase
                            val cy = p1.y + dy * laneFlowPhase
                            val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                            rotate(degrees = angleDeg, pivot = Offset(cx, cy)) {
                                val chev = 4.2.dp.toPx()
                                val chevPath = Path().apply {
                                    moveTo(cx - chev, cy + chev * 0.6f)
                                    lineTo(cx, cy - chev * 0.6f)
                                    lineTo(cx + chev, cy + chev * 0.6f)
                                }
                                drawPath(
                                    path = chevPath,
                                    color = Color.White.copy(alpha = 0.92f),
                                    style = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }
                    }
                }

                // 真实路线下一路口 3D 转向大箭头（根据模式自动切换蓝边或翠绿边）
                val upcomingStep = activeRoute.steps.getOrNull(currentStepIndex + 1)
                    ?: activeRoute.steps.getOrNull(currentStepIndex)
                if (upcomingStep != null && effectiveMode != TravelMode.TRANSIT) {
                    val turnPtScreen = projectWgs84ToScreen(upcomingStep.startPoint, w, h)
                    val borderColor = if (effectiveMode == TravelMode.WALKING || effectiveMode == TravelMode.CYCLING) {
                        Color(0xFF059669)
                    } else {
                        Color(0xFF0088FF)
                    }
                    drawRealTurn3dArrowOnMap(
                        turnCenter = turnPtScreen,
                        actionType = upcomingStep.actionType,
                        roadBearingDeg = bearingDegrees,
                        borderTint = borderColor
                    )
                }
            }

            // 1e. 终点大头针
            val pinTarget = activeRoute?.destinationWgs84 ?: selectedPlace?.wgs84
            if (pinTarget != null) {
                val destScreen = projectWgs84ToScreen(pinTarget, w, h)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.28f),
                    radius = 12.dp.toPx(),
                    center = Offset(destScreen.x, destScreen.y + 3.dp.toPx())
                )
                drawCircle(color = Color.White, radius = 14.dp.toPx(), center = destScreen)
                drawCircle(color = IosRed, radius = 11.dp.toPx(), center = destScreen)
                drawCircle(color = Color.White, radius = 4.5.dp.toPx(), center = destScreen)
            }
        }

        // ==================== 2. 顶部 3D 天际线柔和天空雾化过渡带 ====================
        if (animatedPitchX > 8f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                mapStyle.horizonSkyTopColor.copy(alpha = 0.88f),
                                mapStyle.horizonSkyTopColor.copy(alpha = 0.40f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // ==================== 3. 模式专属 3D 定位模型层（汽车=3D珍珠白轿跑 / 步行=3D步行视野扇形+行人罗盘 / 自行车=3D骑行标 / 公交=轨道脉冲标） ====================
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rawUserPos = projectWgs84ToScreen(currentLocationWgs84, w, h)
            if (rawUserPos.x in -120f..(w + 120f) && rawUserPos.y in -120f..(h + 120f)) {
                if (animatedPitchX > 8f) {
                    when (effectiveMode) {
                        TravelMode.WALKING -> {
                            drawGaode3dWalkingCompassAndPedestrian(
                                center = rawUserPos,
                                bearingDegrees = bearingDegrees,
                                pulsePhase = laneFlowPhase,
                                cardinalPaint = cardinalPaint
                            )
                        }
                        TravelMode.CYCLING -> {
                            drawGaode3dCyclingCompassAndBike(
                                center = rawUserPos,
                                bearingDegrees = bearingDegrees,
                                cardinalPaint = cardinalPaint
                            )
                        }
                        TravelMode.TRANSIT -> {
                            drawGaode3dTransitBeacon(
                                center = rawUserPos,
                                pulsePhase = laneFlowPhase
                            )
                        }
                        TravelMode.DRIVING -> {
                            drawGaode3dCompassAndWhiteCar(
                                center = rawUserPos,
                                bearingDegrees = bearingDegrees,
                                cardinalPaint = cardinalPaint
                            )
                        }
                    }
                } else {
                    val dotColor = when (effectiveMode) {
                        TravelMode.WALKING, TravelMode.CYCLING -> Color(0xFF10B981)
                        TravelMode.TRANSIT -> Color(0xFF06B6D4)
                        TravelMode.DRIVING -> IosBlue
                    }
                    drawCircle(
                        color = dotColor.copy(alpha = 0.18f),
                        radius = 24.dp.toPx(),
                        center = rawUserPos
                    )
                    drawCircle(color = Color.White, radius = 12.dp.toPx(), center = rawUserPos)
                    drawCircle(color = dotColor, radius = 8.5.dp.toPx(), center = rawUserPos)
                }
            }
        }

        // ==================== 4. 左上角：模式专属仪表盘（汽车=车速+路况光柱；步行=计步器+卡路里；自行车=骑行表+海拔爬升；地铁=轨道准点圆章） ====================
        val hasTopTurnBanner = is3dPitchEnabled || isNavigating
        val speedoTopPadding = if (hasTopTurnBanner) 154.dp else 14.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = speedoTopPadding, start = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (effectiveMode) {
                TravelMode.WALKING -> {
                    val estSteps = ((activeRoute?.totalDistanceMeters ?: 860) * 1.35).roundToInt()
                    val estKcal = activeRoute?.caloriesBurnedKcal ?: 45
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(74.dp)
                            .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.20f))
                            .border(1.5.dp, Color(0xFFA7F3D0), CircleShape)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = "步行导航",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "$estSteps",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "预计步数",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFFD1FAE5), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = IosOrange,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${estKcal}千卡",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }

                TravelMode.CYCLING -> {
                    val climbM = activeRoute?.elevationGainMeters ?: 12
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.20f))
                            .border(1.5.dp, Color(0xFF6EE7B7), CircleShape)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                contentDescription = "骑行速度",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (speedKmh >= 1f) "${speedKmh.roundToInt()}" else "16",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF059669)
                            )
                            Text(
                                text = "km/h",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFFD1FAE5), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terrain,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "爬升${climbM}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }

                TravelMode.TRANSIT -> {
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.20f))
                            .border(1.5.dp, Color(0xFFA5F3FC), CircleShape)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsSubway,
                                contentDescription = "公交地铁",
                                tint = Color(0xFF0891B2),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "地铁公交",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0E7490)
                            )
                        }
                    }
                }

                TravelMode.DRIVING -> {
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.22f))
                            .border(1.dp, Color(0xFFE6ECF5), CircleShape)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (speedKmh >= 1f) {
                                Text(
                                    text = "${speedKmh.roundToInt()}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1677FF),
                                    letterSpacing = (-0.5).sp
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(14.dp)
                                            .height(4.dp)
                                            .background(Color(0xFF1677FF), RoundedCornerShape(1.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(14.dp)
                                            .height(4.dp)
                                            .background(Color(0xFF1677FF), RoundedCornerShape(1.dp))
                                    )
                                }
                            }
                            Text(
                                text = "km/h",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF3B78D8)
                            )
                        }
                    }

                    // 仅在汽车驾车模式下展示「全程实时路况彩虹光柱条」
                    if (isTrafficOverlayEnabled && activeRoute != null) {
                        Surface(
                            color = Color.White.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(24.dp)
                                .height(144.dp)
                                .shadow(8.dp, RoundedCornerShape(14.dp))
                                .border(1.5.dp, Color.White, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val smoothWeight = activeRoute.smoothPercent.coerceAtLeast(10).toFloat()
                                val slowWeight = activeRoute.slowPercent.coerceAtLeast(5).toFloat()
                                val congWeight = activeRoute.congestedPercent.coerceAtLeast(3).toFloat()

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(smoothWeight * 0.5f)
                                        .background(
                                            Color(0xFF10B981),
                                            RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(congWeight)
                                        .background(Color(0xFFEF4444))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(slowWeight)
                                        .background(Color(0xFFF59E0B))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(smoothWeight * 0.5f)
                                        .background(
                                            Color(0xFF10B981),
                                            RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==================== 5. 右上角：模式状态徽章 + 实时路况开关 + 3D/2D 工具栏 ====================
        val rightTopPadding = if (hasTopTurnBanner) 154.dp else 14.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = rightTopPadding, end = 12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val modeBadgeText = when (effectiveMode) {
                TravelMode.WALKING -> "步行罗盘·人行步道"
                TravelMode.CYCLING -> "骑行绿道·坡度辅助"
                TravelMode.TRANSIT -> "地铁公交·实时换乘"
                TravelMode.DRIVING -> if (isTrafficOverlayEnabled) "实时路况·已开启" else "高德瓦片·3D"
            }
            val modeBadgeTint = when (effectiveMode) {
                TravelMode.WALKING, TravelMode.CYCLING -> Color(0xFF059669)
                TravelMode.TRANSIT -> Color(0xFF0891B2)
                TravelMode.DRIVING -> if (isTrafficOverlayEnabled) Color(0xFF10B981) else Color(0xFF1677FF)
            }

            Surface(
                color = Color.White.copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clickable { onToggle3dPitch() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = modeBadgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(modeBadgeTint, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            val glassSurfaceColor = if (mapStyle.isDarkStyle) {
                Color(0xFF242426).copy(alpha = 0.90f)
            } else {
                Color.White.copy(alpha = 0.94f)
            }
            val primaryIconTint = if (mapStyle.isDarkStyle) Color.White else Color(0xFF1C1C1E)

            Surface(
                color = glassSurfaceColor,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .width(42.dp)
                    .shadow(7.dp, RoundedCornerShape(12.dp))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { onOpenMapSettings() }
                            .testTag("switch_map_style_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = "选择地图模式",
                            tint = primaryIconTint.copy(alpha = 0.85f),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    HorizontalDivider(
                        color = IosSystemGray.copy(alpha = 0.2f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
                                trackingState = 1
                                cameraCenterWgs84 = currentLocationWgs84
                            }
                            .testTag("recenter_map_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (trackingState >= 1) Icons.Default.NearMe else Icons.Outlined.NearMe,
                            contentDescription = "定位回正",
                            tint = Color(0xFF1677FF),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            if (effectiveMode == TravelMode.DRIVING) {
                Surface(
                    onClick = { isTrafficOverlayEnabled = !isTrafficOverlayEnabled },
                    color = if (isTrafficOverlayEnabled) Color(0xFF10B981) else glassSurfaceColor,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(7.dp, CircleShape)
                        .testTag("toggle_traffic_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "路况",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTrafficOverlayEnabled) Color.White else primaryIconTint
                        )
                    }
                }
            }

            Surface(
                onClick = onToggle3dPitch,
                color = if (is3dPitchEnabled) Color(0xFF1677FF) else glassSurfaceColor,
                shape = CircleShape,
                modifier = Modifier
                    .size(42.dp)
                    .shadow(7.dp, CircleShape)
                    .testTag("toggle_3d_pitch_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (is3dPitchEnabled) "3D" else "2D",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (is3dPitchEnabled) Color.White else primaryIconTint
                    )
                }
            }
        }
    }
}

/**
 * 步行模式专属：「3D 步行视野扇形光锥 + 东南西北随身罗盘 + 翡翠绿立体行人导航光标」
 */
private fun DrawScope.drawGaode3dWalkingCompassAndPedestrian(
    center: Offset,
    bearingDegrees: Float,
    pulsePhase: Float,
    cardinalPaint: Paint
) {
    val rx = 56.dp.toPx()
    val ry = 28.dp.toPx()

    // 1. 前方 65° 翡翠青步行朝向视野扇形光束
    val conePath = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x - 44.dp.toPx(), center.y - 78.dp.toPx())
        lineTo(center.x + 44.dp.toPx(), center.y - 78.dp.toPx())
        close()
    }
    drawPath(
        path = conePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF10B981).copy(alpha = 0.16f),
                Color(0xFF059669).copy(alpha = 0.42f)
            ),
            startY = center.y - 78.dp.toPx(),
            endY = center.y
        )
    )

    // 2. 3D 椭圆步行随身罗盘底盘
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFD1FAE5).copy(alpha = 0.55f),
                Color(0xFF6EE7B7).copy(alpha = 0.30f),
                Color(0xFF10B981).copy(alpha = 0.08f)
            ),
            center = center,
            radius = rx
        ),
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(rx * 2f, ry * 2f)
    )
    drawOval(
        color = Color.White.copy(alpha = 0.82f),
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(rx * 2f, ry * 2f),
        style = Stroke(width = 1.6.dp.toPx())
    )

    // 动态扩散步频脉冲圈
    val pulseRx = rx * (0.45f + pulsePhase * 0.52f)
    val pulseRy = ry * (0.45f + pulsePhase * 0.52f)
    drawOval(
        color = Color(0xFF10B981).copy(alpha = (1f - pulsePhase) * 0.55f),
        topLeft = Offset(center.x - pulseRx, center.y - pulseRy),
        size = Size(pulseRx * 2f, pulseRy * 2f),
        style = Stroke(width = 2.dp.toPx())
    )

    // 东南西北方位文字
    val effectiveBearing = if (bearingDegrees == 0f) 0f else bearingDegrees
    val cardinals = listOf(
        Triple("北", 0f, true),
        Triple("东", 90f, false),
        Triple("南", 180f, false),
        Triple("西", 270f, false)
    )
    val labelRx = rx * 0.82f
    val labelRy = ry * 0.84f
    cardinals.forEach { (label, worldDeg, isNorth) ->
        val relRad = Math.toRadians((worldDeg - effectiveBearing).toDouble())
        val lx = center.x + sin(relRad).toFloat() * labelRx
        val ly = center.y - cos(relRad).toFloat() * labelRy + 4.dp.toPx()

        cardinalPaint.textSize = if (isNorth) 14.sp.toPx() else 12.sp.toPx()
        cardinalPaint.style = Paint.Style.STROKE
        cardinalPaint.strokeWidth = 3.5.dp.toPx()
        cardinalPaint.color = android.graphics.Color.WHITE
        drawContext.canvas.nativeCanvas.drawText(label, lx, ly, cardinalPaint)

        cardinalPaint.style = Paint.Style.FILL
        cardinalPaint.color = if (isNorth) {
            android.graphics.Color.rgb(220, 38, 38)
        } else {
            android.graphics.Color.rgb(6, 95, 70)
        }
        drawContext.canvas.nativeCanvas.drawText(label, lx, ly, cardinalPaint)
    }

    // 3. 中央高德步行专属「立体白边翠绿行人导航箭头圆盘 + 3D 行人徽标」
    drawCircle(
        color = Color.Black.copy(alpha = 0.25f),
        radius = 19.dp.toPx(),
        center = Offset(center.x, center.y + 3.dp.toPx())
    )
    drawCircle(
        color = Color.White,
        radius = 18.dp.toPx(),
        center = center
    )
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF10B981), Color(0xFF059669))
        ),
        radius = 14.5.dp.toPx(),
        center = center
    )

    // 白色前向导航尖角
    val arrowSize = 8.dp.toPx()
    val navArrow = Path().apply {
        moveTo(center.x, center.y - arrowSize * 1.15f)
        lineTo(center.x + arrowSize * 0.85f, center.y + arrowSize * 0.75f)
        lineTo(center.x, center.y + arrowSize * 0.30f)
        lineTo(center.x - arrowSize * 0.85f, center.y + arrowSize * 0.75f)
        close()
    }
    drawPath(path = navArrow, color = Color.White)
}

/**
 * 自行车模式专属：「3D 骑行破风绿道罗盘 + 立体骑行领航标」
 */
private fun DrawScope.drawGaode3dCyclingCompassAndBike(
    center: Offset,
    bearingDegrees: Float,
    cardinalPaint: Paint
) {
    val rx = 58.dp.toPx()
    val ry = 29.dp.toPx()

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFA7F3D0).copy(alpha = 0.50f),
                Color(0xFF34D399).copy(alpha = 0.25f),
                Color.Transparent
            ),
            center = center,
            radius = rx
        ),
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(rx * 2f, ry * 2f)
    )
    drawOval(
        color = Color.White.copy(alpha = 0.78f),
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(rx * 2f, ry * 2f),
        style = Stroke(width = 1.5.dp.toPx())
    )

    val cardinals = listOf(
        Triple("北", 0f, true),
        Triple("东", 90f, false),
        Triple("南", 180f, false),
        Triple("西", 270f, false)
    )
    val labelRx = rx * 0.82f
    val labelRy = ry * 0.84f
    cardinals.forEach { (label, worldDeg, isNorth) ->
        val relRad = Math.toRadians((worldDeg - bearingDegrees).toDouble())
        val lx = center.x + sin(relRad).toFloat() * labelRx
        val ly = center.y - cos(relRad).toFloat() * labelRy + 4.dp.toPx()

        cardinalPaint.textSize = if (isNorth) 14.sp.toPx() else 12.sp.toPx()
        cardinalPaint.style = Paint.Style.STROKE
        cardinalPaint.strokeWidth = 3.5.dp.toPx()
        cardinalPaint.color = android.graphics.Color.WHITE
        drawContext.canvas.nativeCanvas.drawText(label, lx, ly, cardinalPaint)

        cardinalPaint.style = Paint.Style.FILL
        cardinalPaint.color = if (isNorth) {
            android.graphics.Color.rgb(220, 38, 38)
        } else {
            android.graphics.Color.rgb(4, 120, 87)
        }
        drawContext.canvas.nativeCanvas.drawText(label, lx, ly, cardinalPaint)
    }

    // 3D 破风骑行流线型胶囊车标
    drawCircle(
        color = Color.Black.copy(alpha = 0.28f),
        radius = 20.dp.toPx(),
        center = Offset(center.x, center.y + 3.dp.toPx())
    )
    drawCircle(color = Color.White, radius = 19.dp.toPx(), center = center)
    drawCircle(color = Color(0xFF059669), radius = 15.dp.toPx(), center = center)

    val s = 9.dp.toPx()
    val chevron = Path().apply {
        moveTo(center.x, center.y - s * 1.1f)
        lineTo(center.x + s * 0.9f, center.y + s * 0.7f)
        lineTo(center.x, center.y + s * 0.2f)
        lineTo(center.x - s * 0.9f, center.y + s * 0.7f)
        close()
    }
    drawPath(path = chevron, color = Color.White)
}

/**
 * 公交地铁模式专属：「轨道交通脉冲定位标」
 */
private fun DrawScope.drawGaode3dTransitBeacon(
    center: Offset,
    pulsePhase: Float
) {
    val outerR = (24 + pulsePhase * 18).dp.toPx()
    drawCircle(
        color = Color(0xFF06B6D4).copy(alpha = (1f - pulsePhase) * 0.45f),
        radius = outerR,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 16.dp.toPx(),
        center = center
    )
    drawCircle(
        color = Color(0xFF0891B2),
        radius = 12.dp.toPx(),
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 5.dp.toPx(),
        center = center
    )
}

private fun DrawScope.drawRealTurn3dArrowOnMap(
    turnCenter: Offset,
    actionType: String,
    roadBearingDeg: Float,
    borderTint: Color = Color(0xFF0088FF)
) {
    val isRight = actionType.contains("右")
    val dir = if (isRight) 1f else -1f
    val s = 18.dp.toPx()

    rotate(degrees = roadBearingDeg, pivot = turnCenter) {
        val arrowPath = Path().apply {
            moveTo(turnCenter.x - s * 0.24f, turnCenter.y + s * 1.2f)
            lineTo(turnCenter.x - s * 0.24f, turnCenter.y - s * 0.22f)
            lineTo(turnCenter.x + dir * s * 0.85f, turnCenter.y - s * 0.22f)
            lineTo(turnCenter.x + dir * s * 0.85f, turnCenter.y - s * 0.55f)
            lineTo(turnCenter.x + dir * s * 1.45f, turnCenter.y)
            lineTo(turnCenter.x + dir * s * 0.85f, turnCenter.y + s * 0.55f)
            lineTo(turnCenter.x + dir * s * 0.85f, turnCenter.y + s * 0.22f)
            lineTo(turnCenter.x + s * 0.24f, turnCenter.y + s * 0.22f)
            lineTo(turnCenter.x + s * 0.24f, turnCenter.y + s * 1.2f)
            close()
        }
        drawPath(
            path = arrowPath,
            color = Color.Black.copy(alpha = 0.28f),
            style = Stroke(width = 6.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        )
        drawPath(
            path = arrowPath,
            color = borderTint,
            style = Stroke(width = 4.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        )
        drawPath(path = arrowPath, color = Color.White)
    }
}

private fun DrawScope.drawRealCoord3dBuildings(
    w: Float,
    h: Float,
    anchorY: Float,
    centerLat: Double,
    centerLon: Double,
    n: Double,
    centerTileX: Double,
    centerTileY: Double,
    scaledTilePx: Float,
    pitchFactor: Float,
    isDarkStyle: Boolean
) {
    val stepDeg = 0.0028
    val startLatIdx = floor((centerLat - 0.014) / stepDeg).toInt()
    val endLatIdx = floor((centerLat + 0.014) / stepDeg).toInt()
    val startLonIdx = floor((centerLon - 0.016) / stepDeg).toInt()
    val endLonIdx = floor((centerLon + 0.016) / stepDeg).toInt()

    val roofColor = if (isDarkStyle) {
        Color(0xFF2B3C54).copy(alpha = 0.72f)
    } else {
        Color.White.copy(alpha = 0.78f)
    }
    val frontWallColor = if (isDarkStyle) {
        Color(0xFF1F2D40).copy(alpha = 0.75f)
    } else {
        Color(0xFFE3ECF6).copy(alpha = 0.78f)
    }
    val sideWallColor = if (isDarkStyle) {
        Color(0xFF162130).copy(alpha = 0.78f)
    } else {
        Color(0xFFCFDCEB).copy(alpha = 0.78f)
    }
    val strokeColor = if (isDarkStyle) {
        Color(0xFF4B6587).copy(alpha = 0.55f)
    } else {
        Color(0xFFBAC8DA).copy(alpha = 0.70f)
    }

    for (latIdx in endLatIdx downTo startLatIdx) {
        for (lonIdx in startLonIdx..endLonIdx) {
            val hash = abs((latIdx * 73856093) xor (lonIdx * 19349663))
            if (hash % 10 < 5) continue

            val bLat = (latIdx + 0.35) * stepDeg
            val bLon = (lonIdx + 0.35) * stepDeg

            val ptx = lonToTileX(bLon, n)
            val pty = latToTileY(bLat, n)
            val sx = (w / 2f) + ((ptx - centerTileX) * scaledTilePx).toFloat()
            val sy = anchorY + ((pty - centerTileY) * scaledTilePx).toFloat()

            if (sx < -60f || sx > w + 60f || sy < -60f || sy > h + 60f) continue
            if (abs(sx - w / 2f) < 72.dp.toPx()) continue

            val halfW = (18 + (hash % 16)).dp.toPx()
            val halfD = (14 + ((hash / 3) % 14)).dp.toPx()
            val heightPx = (24 + (hash % 36)).dp.toPx() * pitchFactor

            val extrudeX = if (sx < w / 2f) -heightPx * 0.14f else heightPx * 0.14f
            val extrudeY = -heightPx

            val bL = sx - halfW
            val bR = sx + halfW
            val bT = sy - halfD
            val bB = sy + halfD

            val rL = bL + extrudeX
            val rR = bR + extrudeX
            val rT = bT + extrudeY
            val rB = bB + extrudeY

            val frontWall = Path().apply {
                moveTo(bL, bB)
                lineTo(bR, bB)
                lineTo(rR, rB)
                lineTo(rL, rB)
                close()
            }
            drawPath(path = frontWall, color = frontWallColor)

            val sideWall = Path().apply {
                if (sx < w / 2f) {
                    moveTo(bR, bT)
                    lineTo(bR, bB)
                    lineTo(rR, rB)
                    lineTo(rR, rT)
                } else {
                    moveTo(bL, bT)
                    lineTo(bL, bB)
                    lineTo(rL, rB)
                    lineTo(rL, rT)
                }
                close()
            }
            drawPath(path = sideWall, color = sideWallColor)

            val roof = Path().apply {
                moveTo(rL, rT)
                lineTo(rR, rT)
                lineTo(rR, rB)
                lineTo(rL, rB)
                close()
            }
            drawPath(path = roof, color = roofColor)
            drawPath(path = roof, color = strokeColor, style = Stroke(width = 1.dp.toPx()))
        }
    }
}

/**
 * 驾车模式专属：「3D 东南西北透视罗盘光环 + 3D 立体白色轿跑车模」
 */
private fun DrawScope.drawGaode3dCompassAndWhiteCar(
    center: Offset,
    bearingDegrees: Float,
    cardinalPaint: Paint
) {
    val rx = 62.dp.toPx()
    val ry = 31.dp.toPx()

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE0F0FF).copy(alpha = 0.45f),
                Color(0xFFB5D6FF).copy(alpha = 0.30f),
                Color(0xFF8AB8FF).copy(alpha = 0.08f)
            ),
            center = center,
            radius = rx
        ),
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(rx * 2f, ry * 2f)
    )
    drawOval(
        color = Color.White.copy(alpha = 0.72f),
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(rx * 2f, ry * 2f),
        style = Stroke(width = 1.5.dp.toPx())
    )
    val innerRx = rx * 0.65f
    val innerRy = ry * 0.65f
    drawOval(
        color = Color.White.copy(alpha = 0.48f),
        topLeft = Offset(center.x - innerRx, center.y - innerRy),
        size = Size(innerRx * 2f, innerRy * 2f),
        style = Stroke(width = 1.dp.toPx())
    )

    for (deg in 0 until 360 step 15) {
        val rad = Math.toRadians((deg - bearingDegrees).toDouble())
        val sinA = sin(rad).toFloat()
        val cosA = cos(rad).toFloat()
        val rOutX = rx * 0.96f
        val rOutY = ry * 0.96f
        val rInX = if (deg % 90 == 0) rx * 0.80f else rx * 0.88f
        val rInY = if (deg % 90 == 0) ry * 0.80f else ry * 0.88f
        drawLine(
            color = Color.White.copy(alpha = if (deg % 90 == 0) 0.9f else 0.45f),
            start = Offset(center.x + sinA * rInX, center.y - cosA * rInY),
            end = Offset(center.x + sinA * rOutX, center.y - cosA * rOutY),
            strokeWidth = if (deg % 90 == 0) 2.dp.toPx() else 1.dp.toPx()
        )
    }

    val effectiveBearing = if (bearingDegrees == 0f) 180f else bearingDegrees
    val cardinals = listOf(
        Triple("北", 0f, true),
        Triple("东", 90f, false),
        Triple("南", 180f, false),
        Triple("西", 270f, false)
    )
    val labelRx = rx * 0.84f
    val labelRy = ry * 0.86f

    cardinals.forEach { (label, worldDeg, isNorth) ->
        val relRad = Math.toRadians((worldDeg - effectiveBearing).toDouble())
        val lx = center.x + sin(relRad).toFloat() * labelRx
        val ly = center.y - cos(relRad).toFloat() * labelRy + 5.dp.toPx()

        if (isNorth) {
            cardinalPaint.textSize = 15.sp.toPx()
            cardinalPaint.style = Paint.Style.STROKE
            cardinalPaint.strokeWidth = 4.5.dp.toPx()
            cardinalPaint.color = android.graphics.Color.WHITE
            drawContext.canvas.nativeCanvas.drawText(label, lx, ly, cardinalPaint)

            cardinalPaint.style = Paint.Style.FILL
            cardinalPaint.color = android.graphics.Color.rgb(220, 38, 38)
            drawContext.canvas.nativeCanvas.drawText(label, lx, ly, cardinalPaint)
        } else {
            cardinalPaint.textSize = 13.sp.toPx()
            cardinalPaint.style = Paint.Style.STROKE
            cardinalPaint.strokeWidth = 3.5.dp.toPx()
            cardinalPaint.color = android.graphics.Color.WHITE
            drawContext.canvas.nativeCanvas.drawText(label, lx, ly, cardinalPaint)

            cardinalPaint.style = Paint.Style.FILL
            cardinalPaint.color = android.graphics.Color.rgb(30, 41, 59)
            drawContext.canvas.nativeCanvas.drawText(label, lx, ly, cardinalPaint)
        }
    }

    val carW = 38.dp.toPx()
    val carH = 52.dp.toPx()
    val cx = center.x
    val cy = center.y - 4.dp.toPx()

    drawOval(
        color = Color.Black.copy(alpha = 0.50f),
        topLeft = Offset(cx - carW * 0.58f, cy - carH * 0.42f),
        size = Size(carW * 1.16f, carH * 0.96f)
    )
    drawOval(
        color = Color(0xFF22C55E).copy(alpha = 0.38f),
        topLeft = Offset(cx - carW * 0.25f, cy + carH * 0.28f),
        size = Size(carW * 0.50f, carH * 0.22f)
    )

    drawRoundRect(
        color = Color(0xFF111827),
        topLeft = Offset(cx - carW * 0.52f, cy + carH * 0.08f),
        size = Size(7.dp.toPx(), 15.dp.toPx()),
        cornerRadius = CornerRadius(3.dp.toPx())
    )
    drawRoundRect(
        color = Color(0xFF111827),
        topLeft = Offset(cx + carW * 0.52f - 7.dp.toPx(), cy + carH * 0.08f),
        size = Size(7.dp.toPx(), 15.dp.toPx()),
        cornerRadius = CornerRadius(3.dp.toPx())
    )

    drawOval(
        color = Color(0xFFF1F5F9),
        topLeft = Offset(cx - carW * 0.58f, cy - carH * 0.24f),
        size = Size(7.dp.toPx(), 5.dp.toPx())
    )
    drawOval(
        color = Color(0xFFF1F5F9),
        topLeft = Offset(cx + carW * 0.58f - 7.dp.toPx(), cy - carH * 0.24f),
        size = Size(7.dp.toPx(), 5.dp.toPx())
    )

    val bodyPath = Path().apply {
        moveTo(cx - carW * 0.34f, cy - carH * 0.48f)
        lineTo(cx + carW * 0.34f, cy - carH * 0.48f)
        lineTo(cx + carW * 0.48f, cy - carH * 0.12f)
        lineTo(cx + carW * 0.50f, cy + carH * 0.32f)
        lineTo(cx + carW * 0.40f, cy + carH * 0.45f)
        lineTo(cx - carW * 0.40f, cy + carH * 0.45f)
        lineTo(cx - carW * 0.50f, cy + carH * 0.32f)
        lineTo(cx - carW * 0.48f, cy - carH * 0.12f)
        close()
    }
    drawPath(
        path = bodyPath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFD8E1EC),
                Color(0xFFFFFFFF),
                Color(0xFFF8FAFC),
                Color(0xFFD0DBE8)
            ),
            startX = cx - carW * 0.5f,
            endX = cx + carW * 0.5f
        )
    )
    drawPath(
        path = bodyPath,
        color = Color(0xFF94A3B8),
        style = Stroke(width = 1.dp.toPx())
    )

    val canopyPath = Path().apply {
        moveTo(cx - carW * 0.28f, cy - carH * 0.30f)
        lineTo(cx + carW * 0.28f, cy - carH * 0.30f)
        lineTo(cx + carW * 0.34f, cy + carH * 0.16f)
        lineTo(cx - carW * 0.34f, cy + carH * 0.16f)
        close()
    }
    drawPath(
        path = canopyPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)),
            startY = cy - carH * 0.30f,
            endY = cy + carH * 0.16f
        )
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.92f),
        topLeft = Offset(cx - carW * 0.27f, cy - carH * 0.12f),
        size = Size(carW * 0.54f, 6.dp.toPx()),
        cornerRadius = CornerRadius(3.dp.toPx())
    )

    drawRoundRect(
        color = Color(0xFFEF4444),
        topLeft = Offset(cx - carW * 0.42f, cy + carH * 0.24f),
        size = Size(carW * 0.84f, 4.5.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
    drawRoundRect(
        color = Color(0xFFFF1E27),
        topLeft = Offset(cx - carW * 0.44f, cy + carH * 0.22f),
        size = Size(8.dp.toPx(), 6.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
    drawRoundRect(
        color = Color(0xFFFF1E27),
        topLeft = Offset(cx + carW * 0.44f - 8.dp.toPx(), cy + carH * 0.22f),
        size = Size(8.dp.toPx(), 6.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx())
    )

    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(cx - carW * 0.36f, cy + carH * 0.35f),
        size = Size(carW * 0.72f, 5.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
    drawRoundRect(
        color = Color(0xFF22C55E),
        topLeft = Offset(cx - 6.dp.toPx(), cy + carH * 0.34f),
        size = Size(12.dp.toPx(), 4.dp.toPx()),
        cornerRadius = CornerRadius(1.dp.toPx())
    )
}

private fun DrawScope.draw2dProceduralCityVectorMap(
    w: Float,
    h: Float,
    anchorY: Float,
    centerLat: Double,
    centerLon: Double,
    n: Double,
    centerTileX: Double,
    centerTileY: Double,
    scaledTilePx: Float,
    isDarkStyle: Boolean
) {
    drawRect(color = if (isDarkStyle) Color(0xFF141C2B) else Color(0xFFF1F4F8))

    val stepDeg = 0.0045
    val startLatIdx = floor((centerLat - 0.04) / stepDeg).toInt()
    val endLatIdx = floor((centerLat + 0.04) / stepDeg).toInt()
    val startLonIdx = floor((centerLon - 0.04) / stepDeg).toInt()
    val endLonIdx = floor((centerLon + 0.04) / stepDeg).toInt()

    val parkColor = if (isDarkStyle) Color(0xFF1B332B) else Color(0xFFD4EEDB)
    val waterColor = if (isDarkStyle) Color(0xFF172C46) else Color(0xFFBFE0FF)
    val blockColor = if (isDarkStyle) Color(0xFF1C2638) else Color(0xFFE5EAF0)
    val roadColor = if (isDarkStyle) Color(0xFF28364D) else Color.White
    val highwayColor = if (isDarkStyle) Color(0xFF3B4F70) else Color(0xFFFDE68A)

    for (latIdx in startLatIdx..endLatIdx) {
        for (lonIdx in startLonIdx..endLonIdx) {
            val lat = latIdx * stepDeg
            val lon = lonIdx * stepDeg
            val ptx = lonToTileX(lon, n)
            val pty = latToTileY(lat, n)
            val sx = (w / 2f) + ((ptx - centerTileX) * scaledTilePx).toFloat()
            val sy = anchorY + ((pty - centerTileY) * scaledTilePx).toFloat()

            val hash = abs((latIdx * 73856093) xor (lonIdx * 19349663))
            val cellSize = 48.dp.toPx()
            val fill = when {
                hash % 13 == 0 -> waterColor
                hash % 9 == 0 -> parkColor
                else -> blockColor
            }
            drawRoundRect(
                color = fill,
                topLeft = Offset(sx + 4.dp.toPx(), sy + 4.dp.toPx()),
                size = Size(cellSize, cellSize),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
        }
    }

    for (latIdx in startLatIdx..endLatIdx) {
        val lat = latIdx * stepDeg
        val pty = latToTileY(lat, n)
        val sy = anchorY + ((pty - centerTileY) * scaledTilePx).toFloat()
        val isArterial = latIdx % 3 == 0
        drawLine(
            color = if (isArterial) highwayColor else roadColor,
            start = Offset(0f, sy),
            end = Offset(w, sy),
            strokeWidth = if (isArterial) 6.dp.toPx() else 3.dp.toPx()
        )
    }
    for (lonIdx in startLonIdx..endLonIdx) {
        val lon = lonIdx * stepDeg
        val ptx = lonToTileX(lon, n)
        val sx = (w / 2f) + ((ptx - centerTileX) * scaledTilePx).toFloat()
        val isArterial = lonIdx % 3 == 0
        drawLine(
            color = if (isArterial) highwayColor else roadColor,
            start = Offset(sx, 0f),
            end = Offset(sx, h),
            strokeWidth = if (isArterial) 6.dp.toPx() else 3.dp.toPx()
        )
    }
}

@Composable
fun HudCockpitModal(
    speedKmh: Float,
    bearingDegrees: Float,
    currentStep: NavStep?,
    distanceToNextTurnMeters: Int,
    remainingDistanceMeters: Int,
    remainingDurationSeconds: Int,
    isMirrored: Boolean,
    onToggleMirror: () -> Unit,
    onExitHud: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = if (isMirrored) -1f else 1f
                },
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HUD 车载前挡投影模式",
                    color = CyanAccent.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "航向 ${bearingDegrees.roundToInt()}°",
                    color = EmeraldStatus,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${speedKmh.roundToInt()}",
                    fontSize = 88.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = IosGreen
                )
                Text(
                    text = "KM/H 实时车速",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${distanceToNextTurnMeters}米 · ${currentStep?.actionType ?: "直行"}",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentStep?.instruction ?: "沿当前道路继续行驶",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("剩余里程", color = Color.White.copy(alpha = 0.6f))
                    Text(
                        text = if (remainingDistanceMeters >= 1000) {
                            String.format("%.1f km", remainingDistanceMeters / 1000.0)
                        } else {
                            "$remainingDistanceMeters m"
                        },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldStatus
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("预计剩余时间", color = Color.White.copy(alpha = 0.6f))
                    Text(
                        text = "${(remainingDurationSeconds / 60).coerceAtLeast(1)} 分钟",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = IosBlue
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                onClick = onToggleMirror,
                color = Color(0xFF2C2C2E),
                shape = CircleShape,
                modifier = Modifier.testTag("hud_mirror_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Flip, contentDescription = "挡风玻璃镜像翻转", tint = IosBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMirrored) "恢复正向显示" else "前挡风玻璃镜像投影",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                onClick = onExitHud,
                color = IosRed,
                shape = CircleShape,
                modifier = Modifier.testTag("exit_hud_button")
            ) {
                Text(
                    text = "退出 HUD",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

private fun lonToTileX(lon: Double, n: Double): Double {
    return ((lon + 180.0) / 360.0) * n
}

private fun latToTileY(lat: Double, n: Double): Double {
    val clampedLat = lat.coerceIn(-85.0511, 85.0511)
    val latRad = clampedLat * PI / 180.0
    return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
}

private fun tileXToLon(tileX: Double, n: Double): Double {
    val wrapped = ((tileX % n) + n) % n
    return (wrapped / n) * 360.0 - 180.0
}

private fun tileYToLat(tileY: Double, n: Double): Double {
    val a = PI * (1.0 - 2.0 * tileY / n)
    return Math.toDegrees(atan(sinh(a))).coerceIn(-85.0511, 85.0511)
}
