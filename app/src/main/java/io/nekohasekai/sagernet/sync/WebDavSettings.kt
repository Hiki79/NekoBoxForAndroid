package io.nekohasekai.sagernet.sync

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import io.nekohasekai.sagernet.SagerNet
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.RandomAccessFile
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.util.Calendar
import javax.crypto.Cipher
import javax.security.auth.x500.X500Principal

data class WebDavConfig(
    val serverUrl: String = WebDavSettings.DEFAULT_SERVER,
    val username: String = "",
    val appPassword: String = "",
    val backupPassword: String = "",
    val remotePath: String = WebDavSettings.DEFAULT_REMOTE_PATH,
    val autoBackup: Boolean = false,
    val wifiOnly: Boolean = true,
) {
    val isComplete: Boolean
        get() = serverUrl.isNotBlank() &&
            username.isNotBlank() &&
            appPassword.isNotBlank() &&
            backupPassword.length >= 8 &&
            remotePath.isNotBlank()

    override fun toString(): String =
        "WebDavConfig(serverUrl=$serverUrl, usernameSet=${username.isNotEmpty()}, " +
            "appPasswordSet=${appPassword.isNotEmpty()}, " +
            "backupPasswordSet=${backupPassword.isNotEmpty()}, remotePath=$remotePath, " +
            "autoBackup=$autoBackup, wifiOnly=$wifiOnly)"
}

object WebDavSettings {

    const val DEFAULT_SERVER = "https://dav.jianguoyun.com/dav/"
    const val DEFAULT_REMOTE_PATH = "NekoBox/backup.nbk"

    private const val KEY_ALIAS = "nekobox_webdav_credentials"
    private const val KEY_SERVER = "server"
    private const val KEY_USERNAME = "username"
    private const val KEY_APP_PASSWORD = "app_password"
    private const val KEY_BACKUP_PASSWORD = "backup_password"
    private const val KEY_REMOTE_PATH = "remote_path"
    private const val KEY_AUTO_BACKUP = "auto_backup"
    private const val KEY_WIFI_ONLY = "wifi_only"
    private const val KEY_LAST_SYNC = "last_sync"
    private const val KEY_RESTORE_PENDING = "restore_pending"
    private const val KEY_RESTORE_STARTED_AT = "restore_started_at"

    private val context: Context get() = SagerNet.application
    private val settingsFile: AtomicFile
        get() = AtomicFile(File(context.noBackupFilesDir, "webdav_sync.json"))
    private val lockFile: File
        get() = File(context.noBackupFilesDir, "webdav_sync.lock")

    @Synchronized
    fun load(): WebDavConfig = runCatching {
        withFileLock {
            val stored = readSettings()
            WebDavConfig(
                serverUrl = stored.optString(KEY_SERVER, DEFAULT_SERVER),
                username = stored.optString(KEY_USERNAME),
                appPassword = decryptPreference(stored.optString(KEY_APP_PASSWORD)),
                backupPassword = decryptPreference(stored.optString(KEY_BACKUP_PASSWORD)),
                remotePath = stored.optString(KEY_REMOTE_PATH, DEFAULT_REMOTE_PATH),
                autoBackup = stored.optBoolean(KEY_AUTO_BACKUP, false),
                wifiOnly = stored.optBoolean(KEY_WIFI_ONLY, true),
            )
        }
    }.getOrElse { WebDavConfig() }

    @Synchronized
    fun save(config: WebDavConfig) = withFileLock {
        val previous = runCatching(::readSettings).getOrElse { JSONObject() }
        val stored = JSONObject().apply {
            put(KEY_SERVER, config.serverUrl.trim())
            put(KEY_USERNAME, config.username.trim())
            put(KEY_APP_PASSWORD, encryptPreference(config.appPassword))
            put(KEY_BACKUP_PASSWORD, encryptPreference(config.backupPassword))
            put(KEY_REMOTE_PATH, config.remotePath.trim())
            put(KEY_AUTO_BACKUP, config.autoBackup)
            put(KEY_WIFI_ONLY, config.wifiOnly)
            put(KEY_LAST_SYNC, previous.optLong(KEY_LAST_SYNC, 0L))
            put(KEY_RESTORE_PENDING, previous.optBoolean(KEY_RESTORE_PENDING, false))
            put(KEY_RESTORE_STARTED_AT, previous.optLong(KEY_RESTORE_STARTED_AT, 0L))
        }
        writeSettings(stored)
    }

    @Synchronized
    fun lastSync(): Long = runCatching {
        withFileLock { readSettings().optLong(KEY_LAST_SYNC, 0L) }
    }.getOrDefault(0L)

    @Synchronized
    fun markSynced(time: Long = System.currentTimeMillis()) = withFileLock {
        val stored = readSettings().apply { put(KEY_LAST_SYNC, time) }
        writeSettings(stored)
    }

    @Synchronized
    fun beginManualRestore() = withFileLock {
        val stored = readSettings().apply {
            put(KEY_RESTORE_PENDING, true)
            put(KEY_RESTORE_STARTED_AT, System.currentTimeMillis())
        }
        writeSettings(stored)
    }

    @Synchronized
    fun endManualRestore() = withFileLock {
        val stored = readSettings().apply {
            put(KEY_RESTORE_PENDING, false)
            put(KEY_RESTORE_STARTED_AT, 0L)
        }
        writeSettings(stored)
    }

    @Synchronized
    fun isManualRestorePending(): Boolean = runCatching {
        withFileLock {
            val stored = readSettings()
            if (!stored.optBoolean(KEY_RESTORE_PENDING, false)) return@withFileLock false
            val startedAt = stored.optLong(KEY_RESTORE_STARTED_AT, 0L)
            val age = System.currentTimeMillis() - startedAt
            if (startedAt > 0L && age in 0L..MAX_RESTORE_PAUSE_MILLIS) {
                true
            } else {
                stored.put(KEY_RESTORE_PENDING, false)
                stored.put(KEY_RESTORE_STARTED_AT, 0L)
                writeSettings(stored)
                false
            }
        }
    }.getOrDefault(false)

    @Synchronized
    fun recoverInterruptedRestore() = withFileLock {
        val stored = readSettings()
        if (stored.optBoolean(KEY_RESTORE_PENDING, false)) {
            stored.put(KEY_RESTORE_PENDING, false)
            stored.put(KEY_RESTORE_STARTED_AT, 0L)
            writeSettings(stored)
        }
    }

    @Synchronized
    fun clear() = withFileLock {
        settingsFile.delete()
        runCatching {
            val keyStore = keyStore()
            if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
        }
        Unit
    }

    private fun readSettings(): JSONObject = try {
        settingsFile.openRead().use { input -> JSONObject(input.readUtf8Limited()) }
    } catch (_: FileNotFoundException) {
        JSONObject()
    }

    private fun InputStream.readUtf8Limited(): String {
        val output = ByteArrayOutputStream(DEFAULT_BUFFER_SIZE)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val maxRead = minOf(buffer.size, MAX_SETTINGS_BYTES + 1 - total)
            val count = read(buffer, 0, maxRead)
            if (count < 0) break
            total += count
            require(total <= MAX_SETTINGS_BYTES) { "WebDAV settings file is too large" }
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private fun writeSettings(settings: JSONObject) {
        val bytes = settings.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_SETTINGS_BYTES) { "WebDAV settings file is too large" }
        val output = settingsFile.startWrite()
        try {
            output.write(bytes)
            settingsFile.finishWrite(output)
        } catch (error: Throwable) {
            settingsFile.failWrite(output)
            throw error
        } finally {
            bytes.fill(0)
        }
    }

    private inline fun <T> withFileLock(block: () -> T): T {
        lockFile.parentFile?.mkdirs()
        RandomAccessFile(lockFile, "rw").use { file ->
            file.channel.use { channel ->
                val lock = channel.lock()
                try {
                    return block()
                } finally {
                    lock.release()
                }
            }
        }
    }

    private fun encryptPreference(value: String): String {
        if (value.isEmpty()) return ""
        val bytes = value.toByteArray(Charsets.UTF_8)
        return try {
            require(bytes.size <= MAX_CREDENTIAL_BYTES) { "Credential is too long" }
            val keyStore = keyStore()
            ensureKey(keyStore)
            val certificate = checkNotNull(keyStore.getCertificate(KEY_ALIAS)) {
                "WebDAV credential key is unavailable"
            }
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, certificate.publicKey)
            Base64.encodeToString(cipher.doFinal(bytes), Base64.NO_WRAP)
        } finally {
            bytes.fill(0)
        }
    }

    private fun decryptPreference(encrypted: String): String {
        if (encrypted.isEmpty()) return ""
        return runCatching {
            val keyStore = keyStore()
            ensureKey(keyStore)
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, keyStore.getKey(KEY_ALIAS, null) as PrivateKey)
            val decrypted = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP))
            try {
                decrypted.toString(Charsets.UTF_8)
            } finally {
                decrypted.fill(0)
            }
        }.getOrDefault("")
    }

    @Synchronized
    private fun ensureKey(keyStore: KeyStore) {
        if (keyStore.containsAlias(KEY_ALIAS) && keyStore.getCertificate(KEY_ALIAS) != null) return
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
        val generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            generator.initialize(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setKeySize(2048)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .build()
            )
        } else {
            @Suppress("DEPRECATION")
            val start = Calendar.getInstance()
            val end = Calendar.getInstance().apply { add(Calendar.YEAR, 30) }
            @Suppress("DEPRECATION")
            val spec = KeyPairGeneratorSpec.Builder(context)
                .setAlias(KEY_ALIAS)
                .setSubject(X500Principal("CN=$KEY_ALIAS"))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start.time)
                .setEndDate(end.time)
                .setKeySize(2048)
                .build()
            generator.initialize(spec)
        }
        generator.generateKeyPair()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private const val MAX_SETTINGS_BYTES = 64 * 1024
    private const val MAX_CREDENTIAL_BYTES = 190
    private const val MAX_RESTORE_PAUSE_MILLIS = 60 * 60 * 1000L
}
