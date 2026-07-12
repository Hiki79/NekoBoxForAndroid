package io.nekohasekai.sagernet.backup

import android.os.Parcel
import android.os.Parcelable
import io.nekohasekai.sagernet.database.ParcelizeBridge
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.database.RuleEntity
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.database.preference.KeyValuePair
import io.nekohasekai.sagernet.database.preference.PublicDatabase
import io.nekohasekai.sagernet.ktx.toStringPretty
import moe.matsuri.nb4a.utils.Util
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

object BackupManager {

    private const val VERSION = 2
    private const val MAX_BACKUP_BYTES = 16 * 1024 * 1024
    private const val MAX_ENTITIES = 10_000
    private const val MAX_ENTITY_CHARS = 4 * 1024 * 1024

    private data class DecodedBackup(
        val profiles: List<ProxyEntity>?,
        val groups: List<ProxyGroup>?,
        val rules: List<RuleEntity>?,
        val settings: List<KeyValuePair>?,
    )

    fun createBackup(profile: Boolean, rule: Boolean, setting: Boolean): String {
        val out = JSONObject().apply {
            put("version", VERSION)
            put("createdAt", System.currentTimeMillis())
        }
        if (profile || rule) {
            SagerDatabase.instance.runInTransaction {
                if (profile) {
                    out.put("profiles", JSONArray().apply {
                        SagerDatabase.proxyDao.getAll().forEach { put(it.toBase64String()) }
                    })
                    out.put("groups", JSONArray().apply {
                        SagerDatabase.groupDao.allGroups().forEach { put(it.toBase64String()) }
                    })
                }
                if (rule) {
                    out.put("rules", JSONArray().apply {
                        SagerDatabase.rulesDao.allRules().forEach { put(it.toBase64String()) }
                    })
                }
            }
        }
        if (setting) {
            PublicDatabase.instance.runInTransaction {
                out.put("settings", JSONArray().apply {
                    PublicDatabase.kvPairDao.all().forEach { put(it.toBase64String()) }
                })
            }
        }
        return out.toStringPretty().also(::requireValidSize)
    }

    fun parse(content: String): JSONObject {
        requireValidSize(content)
        return parseJson(content)
    }

    fun parse(input: InputStream): JSONObject {
        val output = ByteArrayOutputStream(DEFAULT_BUFFER_SIZE)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val maxRead = minOf(buffer.size, MAX_BACKUP_BYTES + 1 - total)
            val count = input.read(buffer, 0, maxRead)
            if (count < 0) break
            total += count
            require(total <= MAX_BACKUP_BYTES) { "Backup file is too large" }
            output.write(buffer, 0, count)
        }
        return parseJson(output.toString(Charsets.UTF_8.name()))
    }

    private fun parseJson(content: String): JSONObject {
        val json = JSONObject(content)
        val version = json.optInt("version", 0)
        require(version in 1..VERSION) { "Unsupported backup version: $version" }
        return json
    }

    fun restore(
        content: JSONObject,
        profile: Boolean,
        rule: Boolean,
        setting: Boolean,
    ) {
        val target = decode(content, profile, rule, setting)
        val rollback = decode(
            parse(createBackup(profile, rule, setting)),
            profile,
            rule,
            setting,
        )
        try {
            apply(target, profile, rule, setting)
        } catch (error: Exception) {
            try {
                apply(rollback, profile, rule, setting)
            } catch (rollbackError: Exception) {
                error.addSuppressed(rollbackError)
            }
            throw error
        }
    }

    private fun decode(
        content: JSONObject,
        profile: Boolean,
        rule: Boolean,
        setting: Boolean,
    ): DecodedBackup {
        val profiles = if (profile && content.has("profiles")) {
            content.getJSONArray("profiles").decode { ProxyEntity.CREATOR.createFromParcel(it) }
        } else null
        val groups = if (profile && content.has("groups")) {
            content.getJSONArray("groups").decode { ProxyGroup.CREATOR.createFromParcel(it) }
        } else null
        require(!profile || (profiles == null) == (groups == null)) {
            "Backup must contain profiles and groups together"
        }
        val rules = if (rule && content.has("rules")) {
            content.getJSONArray("rules").decode { ParcelizeBridge.createRule(it) }
        } else null
        val settings = if (setting && content.has("settings")) {
            content.getJSONArray("settings").decode { KeyValuePair.CREATOR.createFromParcel(it) }
        } else null
        profiles?.requireUnique("profile") { it.id }
        groups?.requireUnique("group") { it.id }
        rules?.requireUnique("rule") { it.id }
        settings?.requireUnique("setting") { it.key }
        return DecodedBackup(profiles, groups, rules, settings)
    }

    private fun apply(
        backup: DecodedBackup,
        profile: Boolean,
        rule: Boolean,
        setting: Boolean,
    ) {
        SagerDatabase.instance.runInTransaction {
            if (profile && backup.profiles != null && backup.groups != null) {
                SagerDatabase.proxyDao.reset()
                SagerDatabase.groupDao.reset()
                SagerDatabase.groupDao.insert(backup.groups)
                SagerDatabase.proxyDao.insert(backup.profiles)
            }
            if (rule && backup.rules != null) {
                SagerDatabase.rulesDao.reset()
                SagerDatabase.rulesDao.insert(backup.rules)
            }
        }
        if (setting && backup.settings != null) {
            PublicDatabase.instance.runInTransaction {
                PublicDatabase.kvPairDao.reset()
                PublicDatabase.kvPairDao.insert(backup.settings)
            }
        }
    }

    private fun Parcelable.toBase64String(): String {
        val parcel = Parcel.obtain()
        return try {
            writeToParcel(parcel, 0)
            val data = parcel.marshall()
            try {
                Util.b64EncodeUrlSafe(data).also {
                    require(it.length <= MAX_ENTITY_CHARS) { "Backup entry is too large" }
                }
            } finally {
                data.fill(0)
            }
        } finally {
            parcel.recycle()
        }
    }

    private fun requireValidSize(content: String) {
        require(content.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) {
            "Backup file is too large"
        }
    }

    private inline fun <T, K> List<T>.requireUnique(
        type: String,
        key: (T) -> K,
    ) {
        require(mapTo(HashSet<K>(size), key).size == size) {
            "Backup contains duplicate $type entries"
        }
    }

    private inline fun <T> JSONArray.decode(create: (Parcel) -> T): List<T> {
        require(length() <= MAX_ENTITIES) { "Backup contains too many entries" }
        return List(length()) { index ->
            val encoded = getString(index)
            require(encoded.length <= MAX_ENTITY_CHARS) { "Backup entry is too large" }
            val data = Util.b64Decode(encoded)
            val parcel = Parcel.obtain()
            try {
                parcel.unmarshall(data, 0, data.size)
                parcel.setDataPosition(0)
                create(parcel)
            } finally {
                parcel.recycle()
                data.fill(0)
            }
        }
    }
}
