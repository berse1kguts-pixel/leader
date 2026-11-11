package com.example.data

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double
)

/**
 * 坐标系转换与空间几何工具类：
 * - 腾讯地图 WebService API 使用 GCJ-02（国测局火星坐标系）
 * - Android 硬件 GPS 与 OpenStreetMap / OSRM 使用 WGS-84 坐标系
 * 提供零依赖、高精度的双向纠偏与腾讯地图 polyline 差分数组解压算法。
 */
object CoordinateUtils {
    private const val PI = 3.1415926535897932384626
    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323
    private const val EARTH_RADIUS_METERS = 6371000.0

    fun isInsideChina(lat: Double, lon: Double): Boolean {
        return lon in 72.004..137.8347 && lat in 0.8293..55.8271
    }

    /**
     * WGS-84 (GPS / OSM) -> GCJ-02 (腾讯地图 / 高德地图)
     */
    fun wgs84ToGcj02(lat: Double, lon: Double): LatLngPoint {
        if (!isInsideChina(lat, lon)) return LatLngPoint(lat, lon)
        var dLat = transformLat(lon - 105.0, lat - 35.0)
        var dLon = transformLon(lon - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI)
        dLon = (dLon * 180.0) / (A / sqrtMagic * cos(radLat) * PI)
        return LatLngPoint(lat + dLat, lon + dLon)
    }

    /**
     * GCJ-02 (腾讯地图) -> WGS-84 (GPS / OSM)
     */
    fun gcj02ToWgs84(lat: Double, lon: Double): LatLngPoint {
        if (!isInsideChina(lat, lon)) return LatLngPoint(lat, lon)
        val gcj = wgs84ToGcj02(lat, lon)
        val dLat = gcj.latitude - lat
        val dLon = gcj.longitude - lon
        return LatLngPoint(lat - dLat, lon - dLon)
    }

    /**
     * 腾讯位置服务 Direction v1 接口返回的 polyline 前向差分数组解压算法：
     * for (var i = 2; i < coors.length ; i++) { coors[i] = coors[i-2] + Number(coors[i])/1000000 }
     */
    fun decompressTencentPolyline(rawCoors: List<Double>): List<LatLngPoint> {
        if (rawCoors.size < 2) return emptyList()
        val copy = rawCoors.toMutableList()
        for (i in 2 until copy.size) {
            copy[i] = copy[i - 2] + copy[i] / 1_000_000.0
        }
        val points = ArrayList<LatLngPoint>(copy.size / 2)
        var i = 0
        while (i + 1 < copy.size) {
            val lat = copy[i]
            val lng = copy[i + 1]
            if (abs(lat) <= 90.0 && abs(lng) <= 180.0) {
                points.add(LatLngPoint(lat, lng))
            }
            i += 2
        }
        return points
    }

    /**
     * 计算两点间球面距离（单位：米）
     */
    fun haversineDistanceMeters(p1: LatLngPoint, p2: LatLngPoint): Double {
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)
        val a = sin(dLat / 2).pow(2.0) + sin(dLon / 2).pow(2.0) * cos(lat1) * cos(lat2)
        val c = 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * 计算从 p1 指向 p2 的正北航向角（0°~360°）
     */
    fun bearingDegrees(p1: LatLngPoint, p2: LatLngPoint): Float {
        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val brng = Math.toDegrees(atan2(y, x))
        return ((brng + 360.0) % 360.0).toFloat()
    }

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLon(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * y + 0.1 * x * x + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }
}
