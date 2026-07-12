package io.nekohasekai.sagernet.backup

import android.util.Base64
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {

    private const val FORMAT = "nekobox-webdav-backup"
    private const val VERSION = 1
    private const val ITERATIONS = 150_000
    private const val MAX_PLAIN_BYTES = 16 * 1024 * 1024
    private const val MAX_CIPHER_BYTES = MAX_PLAIN_BYTES + 16
    private const val MAX_ENCRYPTED_BYTES = 24 * 1024 * 1024
    private val associatedData = "NekoBox WebDAV Backup v1".toByteArray(Charsets.UTF_8)
    private val secureRandom = SecureRandom()

    fun encrypt(plainText: String, password: String): String {
        require(password.length >= 8) { "Backup password must contain at least 8 characters" }
        val plainBytes = plainText.toByteArray(Charsets.UTF_8)
        val salt = ByteArray(16).also(secureRandom::nextBytes)
        val iv = ByteArray(12).also(secureRandom::nextBytes)
        val payload = try {
            require(plainBytes.size <= MAX_PLAIN_BYTES) { "Backup file is too large" }
            val key = deriveKey(password, salt, ITERATIONS)
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.ENCRYPT_MODE,
                    SecretKeySpec(key, "AES"),
                    GCMParameterSpec(128, iv),
                )
                cipher.updateAAD(associatedData)
                cipher.doFinal(plainBytes)
            } finally {
                key.fill(0)
            }
        } finally {
            plainBytes.fill(0)
        }

        return JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("kdf", "PBKDF2-HMAC-SHA256")
            put("iterations", ITERATIONS)
            put("salt", salt.base64())
            put("cipher", "AES-256-GCM")
            put("iv", iv.base64())
            put("payload", payload.base64())
        }.toString()
    }

    fun decrypt(encrypted: String, password: String): String {
        require(password.length >= 8) { "Backup password must contain at least 8 characters" }
        require(encrypted.toByteArray(Charsets.UTF_8).size <= MAX_ENCRYPTED_BYTES) {
            "Encrypted backup is too large"
        }
        val envelope = JSONObject(encrypted)
        require(envelope.optString("format") == FORMAT) { "Invalid encrypted backup" }
        require(envelope.optInt("version") == VERSION) { "Unsupported encrypted backup version" }
        require(envelope.optString("kdf") == "PBKDF2-HMAC-SHA256") { "Unsupported backup KDF" }
        require(envelope.optString("cipher") == "AES-256-GCM") { "Unsupported backup cipher" }
        val iterations = envelope.getInt("iterations")
        require(iterations in 100_000..1_000_000) { "Invalid backup KDF cost" }
        val salt = envelope.getString("salt").base64Bytes()
        val iv = envelope.getString("iv").base64Bytes()
        val payload = envelope.getString("payload").base64Bytes()
        require(salt.size == 16 && iv.size == 12) { "Invalid encrypted backup parameters" }
        require(payload.size in 16..MAX_CIPHER_BYTES) { "Invalid encrypted backup payload" }

        val key = deriveKey(password, salt, iterations)
        val plainBytes = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(128, iv),
            )
            cipher.updateAAD(associatedData)
            cipher.doFinal(payload)
        } finally {
            key.fill(0)
        }
        return try {
            require(plainBytes.size <= MAX_PLAIN_BYTES) { "Backup file is too large" }
            plainBytes.toString(Charsets.UTF_8)
        } finally {
            plainBytes.fill(0)
        }
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val passwordBytes = password.toByteArray(Charsets.UTF_8)
        try {
            mac.init(SecretKeySpec(passwordBytes, "HmacSHA256"))
        } finally {
            passwordBytes.fill(0)
        }

        val block = ByteBuffer.allocate(salt.size + 4)
            .put(salt)
            .putInt(1)
            .array()
        var current = try {
            mac.doFinal(block)
        } finally {
            block.fill(0)
        }
        val result = current.copyOf()
        var complete = false
        try {
            repeat(iterations - 1) {
                val next = mac.doFinal(current)
                current.fill(0)
                current = next
                for (index in result.indices) {
                    result[index] = (result[index].toInt() xor current[index].toInt()).toByte()
                }
            }
            complete = true
            return result
        } finally {
            current.fill(0)
            if (!complete) result.fill(0)
        }
    }

    private fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.base64Bytes(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
}
