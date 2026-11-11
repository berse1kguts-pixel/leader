package com.example.data.remote

import com.example.data.LatLngPoint
import com.squareup.moshi.Json

enum class NavEngineType(val displayName: String, val coordBadge: String) {
    AMAP_LBS("高德开放平台 API", "GCJ-02 高德官方路况"),
    TENCENT_LBS("腾讯位置服务 API", "GCJ-02 腾讯官方路况"),
    OPEN_OSRM("开源 OSRM + OSM", "WGS-84 国际开源路网")
}

enum class TravelMode(
    val label: String,
    val tencentPath: String,
    val osrmProfile: String,
    val avgSpeedKmh: Double
) {
    DRIVING("汽车", "driving", "driving", 42.0),
    TRANSIT("公交地铁", "transit", "driving", 28.0),
    CYCLING("自行车", "bicycling", "bike", 16.5),
    WALKING("步行", "walking", "foot", 5.0)
}

data class SearchPoiResult(
    val id: String,
    val title: String,
    val address: String,
    val category: String,
    val wgs84Point: LatLngPoint,
    val gcj02Point: LatLngPoint,
    val sourceEngine: String
)

enum class TrafficCondition(val label: String, val colorHex: Long) {
    SMOOTH("畅通", 0xFF10B981),
    SLOW("缓行", 0xFFF59E0B),
    CONGESTED("拥堵", 0xFFEF4444)
}

data class NavStep(
    val index: Int,
    val instruction: String,
    val roadName: String,
    val distanceMeters: Int,
    val actionType: String,
    val startPoint: LatLngPoint,
    val trafficCondition: TrafficCondition = TrafficCondition.SMOOTH
)

data class TransitSegment(
    val type: String, // "WALK", "SUBWAY", "BUS"
    val lineName: String,
    val lineBadgeColorHex: Long,
    val departureStop: String,
    val arrivalStop: String,
    val stationCount: Int,
    val durationMinutes: Int,
    val distanceMeters: Int,
    val exitInfo: String = ""
)

data class TransitPlanOption(
    val id: String,
    val tagLabel: String,
    val totalDurationMinutes: Int,
    val totalDistanceMeters: Int,
    val walkingDistanceMeters: Int,
    val costYuan: Int,
    val frequencyText: String,
    val segments: List<TransitSegment>
)

data class TransitLineArrival(
    val lineName: String,          // 例如："地铁1号线八通线", "地铁8号线", "1路", "52路", "120路"
    val shortBadge: String,        // 例如："1号线", "8号线", "1路", "52路"
    val badgeColorHex: Long,       // 例如：0xFFC23A30 (北京1号线红), 0xFF009B6B (8号线绿), 0xFF1677FF (公交蓝)
    val isSubway: Boolean,
    val directionTerminal: String, // 例如："开往 环球度假区", "开往 古城", "开往 四惠枢纽站"
    val nextStopName: String,      // 例如："下一站 王府井"
    val nextArrivalMinutes: Int,   // 例如：1 (1分钟 / 即将进站)
    val secondArrivalMinutes: Int, // 例如：6 (下一班 6分钟)
    val crowdStatus: String,       // 例如："余座充足", "舒适"
    val firstLastTime: String,     // 例如："首 05:10 · 末 23:35"
    val stopsSequence: List<String>, // 站点序列：例如 ["复兴门", "西单", "天安门西", "天安门东", "王府井", "东单", "建国门"]
    val currentStopIndex: Int = 3
)

data class TransitExitGuide(
    val exitCode: String,          // 例如："B东北口"
    val landmarkDesc: String,      // 例如："南池子大街 · 故宫东华门 · 设有无障碍直梯"
    val distanceMeters: Int
)

data class TransitStationInfo(
    val id: String,
    val name: String,              // 例如："天安门东", "南池子南口", "东华门", "王府井"
    val stationType: String,       // "SUBWAY" 或 "BUS"
    val nearestExit: String,       // 例如："B东北口 (无障碍直梯)"
    val distanceMeters: Int,
    val walkingMinutes: Int,
    val wgs84Point: LatLngPoint,
    val gcj02Point: LatLngPoint,
    val lines: List<TransitLineArrival>,
    val exits: List<TransitExitGuide>
)

data class CalculatedRoute(
    val originName: String,
    val destinationName: String,
    val destinationAddress: String,
    val originWgs84: LatLngPoint,
    val destinationWgs84: LatLngPoint,
    val polylineWgs84: List<LatLngPoint>,
    val totalDistanceMeters: Int,
    val totalDurationSeconds: Int,
    val steps: List<NavStep>,
    val travelMode: TravelMode,
    val trafficLightsCount: Int = 6,
    val elevationGainMeters: Int = 14,
    val caloriesBurnedKcal: Int = 120,
    val smoothPercent: Int = 82,
    val slowPercent: Int = 13,
    val congestedPercent: Int = 5,
    val transitOptions: List<TransitPlanOption> = emptyList(),
    val engineUsed: NavEngineType,
    val engineNotice: String
)

// ==================== 高德开放平台 Web服务 API (v3/v4) 响应模型 ====================

data class AmapPoiSearchResponse(
    val status: String = "0",
    val info: String = "",
    val pois: List<AmapPoiItem>? = null
)

data class AmapPoiItem(
    val id: String = "",
    val name: String = "",
    val address: Any? = null,
    val type: String = "",
    val location: String = "" // "lng,lat"
)

data class AmapDirectionV3Response(
    val status: String = "0",
    val info: String = "",
    val route: AmapRouteContainer? = null
)

data class AmapRouteContainer(
    val paths: List<AmapPathItem>? = null
)

data class AmapPathItem(
    val distance: String = "0",
    val duration: String = "0",
    @Json(name = "traffic_lights") val trafficLights: String? = null,
    val steps: List<AmapStepItem>? = null
)

data class AmapStepItem(
    val instruction: String? = null,
    val road: Any? = null,
    val distance: String = "0",
    val duration: String = "0",
    val action: Any? = null,
    val polyline: String? = null, // "lng1,lat1;lng2,lat2"
    val tmcs: List<AmapTmcItem>? = null
)

data class AmapTmcItem(
    val status: String? = null, // "畅通", "缓行", "拥堵", "严重拥堵"
    val distance: String? = null
)

// ==================== 腾讯位置服务 WebService API 响应模型 ====================

data class TencentSearchResponse(
    val status: Int = -1,
    val message: String = "",
    val data: List<TencentPoiItem>? = null
)

data class TencentPoiItem(
    val id: String = "",
    val title: String = "",
    val address: String = "",
    val category: String = "",
    val location: TencentLatLng? = null
)

data class TencentLatLng(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class TencentDirectionResponse(
    val status: Int = -1,
    val message: String = "",
    val result: TencentDirectionResult? = null
)

data class TencentDirectionResult(
    val routes: List<TencentRouteItem>? = null
)

data class TencentRouteItem(
    val distance: Int = 0,
    val duration: Int = 0,
    val polyline: List<Double>? = null,
    val steps: List<TencentRouteStep>? = null
)

data class TencentRouteStep(
    val mode: String? = null, // 公交接口中："WALKING" 或 "TRANSIT"
    val instruction: String? = null,
    @Json(name = "road_name") val roadName: String? = null,
    val distance: Int = 0,
    val duration: Int = 0,
    @Json(name = "act_desc") val actDesc: String? = null,
    @Json(name = "polyline_idx") val polylineIdx: List<Int>? = null,
    val polyline: List<Double>? = null,
    val lines: List<TencentTransitLineItem>? = null
)

data class TencentTransitLineItem(
    val vehicle: String? = null, // "SUBWAY", "BUS", "RAIL"
    val title: String? = null,
    @Json(name = "station_count") val stationCount: Int = 0,
    val price: Double = 0.0,
    val distance: Int = 0,
    val duration: Int = 0,
    val destination: TencentStopNode? = null,
    val geton: TencentStopNode? = null,
    val getoff: TencentStopNode? = null,
    val polyline: List<Double>? = null
)

data class TencentStopNode(
    val title: String? = null,
    @Json(name = "exit_title") val exitTitle: String? = null
)

// ==================== OpenStreetMap (Nominatim & OSRM) 响应模型 ====================

data class NominatimPlaceItem(
    @Json(name = "place_id") val placeId: Long = 0L,
    @Json(name = "display_name") val displayName: String = "",
    val name: String? = null,
    val type: String? = null,
    val lat: String = "0.0",
    val lon: String = "0.0"
)

data class OsrmRouteResponse(
    val code: String = "",
    val routes: List<OsrmRouteItem>? = null
)

data class OsrmRouteItem(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val geometry: OsrmGeometry? = null,
    val legs: List<OsrmLeg>? = null
)

data class OsrmGeometry(
    val coordinates: List<List<Double>>? = null
)

data class OsrmLeg(
    val steps: List<OsrmStep>? = null
)

data class OsrmStep(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val name: String = "",
    val maneuver: OsrmManeuver? = null
)

data class OsrmManeuver(
    val type: String? = null,
    val modifier: String? = null,
    val location: List<Double>? = null
)
