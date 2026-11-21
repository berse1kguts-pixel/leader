package com.example.security

import android.content.Context
import android.util.Base64
import com.example.data.LatLngPoint
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class DecryptedLocationSample(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val accuracyMeters: Float,
    val batteryPercent: Int,
    val recordedAtMs: Long,
    val travelMode: String
) {
    val wgs84: LatLngPoint get() = LatLngPoint(latitude, longitude)
}

/**
 * 端到端位置载荷加密模块（AES-256-GCM）：
 * - 明文经纬度仅在发送端使用本地 256 位随机密钥加密后上传 Firestore，服务器仅保存 ivBase64:cipherBase64 密文。
 * - 密钥托管文档（key_escrows/{ownerUid}）受 Firestore 安全规则严格锁死，仅在「非本人发起 + 通知冷却结束 + 同意大于反对 + 本人未一票否决」解锁后才允许授权群友读取解密。
 */
class GuardianCrypto(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "purenav_guardian_keystore",
        Context.MODE_PRIVATE
    )

    fun getOrCreateUserKeyB64(uid: String): String {
        val prefKey = "aes256_$uid"
        val existing = prefs.getString(prefKey, null)
        if (!existing.isNullOrBlank()) return existing

        val generator = KeyGenerator.getInstance("AES")
        generator.init(256, SecureRandom())
        val encoded = Base64.encodeToString(generator.generateKey().encoded, Base64.NO_WRAP)
        prefs.edit().putString(prefKey, encoded).apply()
        return encoded
    }

    fun rotateUserKeyB64(uid: String): String {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(256, SecureRandom())
        val encoded = Base64.encodeToString(generator.generateKey().encoded, Base64.NO_WRAP)
        prefs.edit().putString("aes256_$uid", encoded).apply()
        return encoded
    }

    fun encryptLocationPayload(
        keyB64: String,
        point: LatLngPoint,
        speedKmh: Float,
        accuracyMeters: Float,
        batteryPercent: Int,
        recordedAtMs: Long,
        travelMode: String
    ): String {
        val json = JSONObject().apply {
            put("lat", point.latitude)
            put("lng", point.longitude)
            put("spd", speedKmh.toDouble())
            put("acc", accuracyMeters.toDouble())
            put("bat", batteryPercent)
            put("ts", recordedAtMs)
            put("mode", travelMode)
        }.toString()

        val secretKey = decodeKey(keyB64)
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(json.toByteArray(Charsets.UTF_8))

        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val ctB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        return "$ivB64:$ctB64"
    }

    fun decryptLocationPayload(keyB64: String, encryptedPayload: String): DecryptedLocationSample? {
        return try {
            val parts = encryptedPayload.split(":", limit = 2)
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            val secretKey = decodeKey(keyB64)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            val plain = String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
            val obj = JSONObject(plain)
            DecryptedLocationSample(
                latitude = obj.getDouble("lat"),
                longitude = obj.getDouble("lng"),
                speedKmh = obj.optDouble("spd", 0.0).toFloat(),
                accuracyMeters = obj.optDouble("acc", 10.0).toFloat(),
                batteryPercent = obj.optInt("bat", 100),
                recordedAtMs = obj.optLong("ts", 0L),
                travelMode = obj.optString("mode", "DRIVING")
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeKey(keyB64: String): SecretKey {
        val raw = Base64.decode(keyB64.trim(), Base64.NO_WRAP)
        require(raw.size == 16 || raw.size == 24 || raw.size == 32) {
            "Invalid AES key length: ${raw.size}"
        }
        return SecretKeySpec(raw, "AES")
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
