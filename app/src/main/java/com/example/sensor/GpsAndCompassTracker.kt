package com.example.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import com.example.data.LatLngPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class TelemetryState(
    val currentWgs84: LatLngPoint = LatLngPoint(39.9087, 116.3975), // 默认北京天安门北侧起点
    val speedKmh: Float = 0f,
    val bearingDegrees: Float = 0f,
    val accuracyMeters: Float = 12f,
    val hasRealGpsFix: Boolean = false,
    val isVoiceEnabled: Boolean = true
)

/**
 * 轻量级硬件传感器与语音播报控制器：
 * - 使用系统原生 LocationManager 与 SensorManager（TYPE_ROTATION_VECTOR），
 *   不依赖臃肿后台推送服务，杜绝后台唤醒耗电与内存泄漏。
 * - 使用 Android 原生 TextToSpeech 实现离线零延迟转向语音播报。
 */
class GpsAndCompassTracker(context: Context) : SensorEventListener {

    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val _telemetry = MutableStateFlow(TelemetryState())
    val telemetry: StateFlow<TelemetryState> = _telemetry.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isTrackingLocation = false
    private var isTrackingCompass = false

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // 过滤 Android 模拟器出厂默认的美国加州 Mountain View (37.422, -122.084) 虚拟坐标，避免国内导航起点跳到海外
            val isDefaultUsEmulatorCoord =
                location.latitude in 37.35..37.48 && location.longitude in -122.15..-122.00
            if (isDefaultUsEmulatorCoord) return

            val speedKmh = if (location.hasSpeed()) (location.speed * 3.6f).coerceAtLeast(0f) else 0f
            val newBearing = if (location.hasBearing() && speedKmh > 3f) {
                location.bearing
            } else {
                _telemetry.value.bearingDegrees
            }
            _telemetry.value = _telemetry.value.copy(
                currentWgs84 = LatLngPoint(location.latitude, location.longitude),
                speedKmh = speedKmh,
                bearingDegrees = newBearing,
                accuracyMeters = if (location.hasAccuracy()) location.accuracy else 10f,
                hasRealGpsFix = true
            )
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    init {
        try {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val res = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                    isTtsReady = res != TextToSpeech.LANG_MISSING_DATA &&
                        res != TextToSpeech.LANG_NOT_SUPPORTED
                    if (!isTtsReady) {
                        tts?.setLanguage(Locale.US)
                        isTtsReady = true
                    }
                }
            }
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startHardwareTracking(hasLocationPermission: Boolean) {
        if (!isTrackingCompass) {
            val rotSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            if (rotSensor != null) {
                sensorManager?.registerListener(
                    this,
                    rotSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
                isTrackingCompass = true
            }
        }

        if (hasLocationPermission && !isTrackingLocation && locationManager != null) {
            try {
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER
                )
                for (provider in providers) {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.requestLocationUpdates(
                            provider,
                            1500L,
                            2f,
                            locationListener
                        )
                        val last = locationManager.getLastKnownLocation(provider)
                        if (last != null) {
                            locationListener.onLocationChanged(last)
                        }
                    }
                }
                isTrackingLocation = true
            } catch (_: SecurityException) {
                isTrackingLocation = false
            } catch (_: Exception) {
                isTrackingLocation = false
            }
        }
    }

    fun stopHardwareTracking() {
        if (isTrackingLocation) {
            try {
                locationManager?.removeUpdates(locationListener)
            } catch (_: Exception) {}
            isTrackingLocation = false
        }
        if (isTrackingCompass) {
            try {
                sensorManager?.unregisterListener(this)
            } catch (_: Exception) {}
            isTrackingCompass = false
        }
    }

    fun updateSimulatedPosition(point: LatLngPoint, speedKmh: Float, bearing: Float) {
        _telemetry.value = _telemetry.value.copy(
            currentWgs84 = point,
            speedKmh = speedKmh,
            bearingDegrees = bearing
        )
    }

    fun toggleVoice(): Boolean {
        val next = !_telemetry.value.isVoiceEnabled
        _telemetry.value = _telemetry.value.copy(isVoiceEnabled = next)
        return next
    }

    fun speakInstruction(text: String) {
        if (!_telemetry.value.isVoiceEnabled || !isTtsReady) return
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nav_step_${System.currentTimeMillis()}")
        } catch (_: Exception) {}
    }

    fun release() {
        stopHardwareTracking()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthDeg = ((Math.toDegrees(orientationAngles[0].toDouble()) + 360.0) % 360.0).toFloat()
            // 仅在静止或低速时使用电子罗盘航向，高速行驶时优先使用 GPS 轨迹航向
            if (_telemetry.value.speedKmh < 5f) {
                _telemetry.value = _telemetry.value.copy(bearingDegrees = azimuthDeg)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
