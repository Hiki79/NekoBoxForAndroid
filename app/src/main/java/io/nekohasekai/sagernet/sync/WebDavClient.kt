package io.nekohasekai.sagernet.sync

import io.nekohasekai.sagernet.BuildConfig
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class WebDavException(
    message: String,
    val statusCode: Int? = null,
) : IOException(message)

class WebDavClient(private val config: WebDavConfig) {

    private val client get() = HTTP_CLIENT

    private val baseUrl: HttpUrl by lazy {
        val url = config.serverUrl.trim().toHttpUrlOrNull()
            ?: throw WebDavException("Invalid WebDAV server URL")
        if (url.scheme != "https") throw WebDavException("WebDAV requires HTTPS")
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) {
            throw WebDavException("WebDAV server URL must not contain credentials")
        }
        if (url.query != null || url.fragment != null) {
            throw WebDavException("WebDAV server URL must not contain a query or fragment")
        }
        if (url.encodedPath.endsWith('/')) url else url.newBuilder().addPathSegment("").build()
    }

    private val pathSegments: List<String> by lazy {
        val segments = config.remotePath
            .trim()
            .trim('/')
            .split('/')
            .filter { it.isNotBlank() }
        if (segments.isEmpty() || segments.any { it == "." || it == ".." }) {
            throw WebDavException("Invalid remote backup path")
        }
        segments
    }

    private val authorization: String by lazy {
        if (!config.isComplete) throw WebDavException("WebDAV configuration is incomplete")
        Credentials.basic(config.username, config.appPassword, Charsets.UTF_8)
    }

    fun testConnection() {
        execute(
            Request.Builder()
                .url(baseUrl)
                .method("PROPFIND", EMPTY_BODY)
                .header("Depth", "0")
                .authenticated()
                .build()
        ).use { }
    }

    fun upload(content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_TRANSFER_BYTES) {
            throw WebDavException("Backup is too large to upload")
        }
        try {
            ensureDirectories()
            val target = targetUrl()
            val temporary = temporaryUrl()
            deleteTemporary(temporary)
            try {
                execute(
                    Request.Builder()
                        .url(temporary)
                        .put(bytes.toRequestBody(BACKUP_MEDIA_TYPE))
                        .authenticated()
                        .build()
                ).use { }
                execute(
                    Request.Builder()
                        .url(temporary)
                        .method("MOVE", EMPTY_BODY)
                        .header("Destination", target.toString())
                        .header("Overwrite", "T")
                        .authenticated()
                        .build()
                ).use { }
            } finally {
                runCatching { deleteTemporary(temporary) }
            }
        } finally {
            bytes.fill(0)
        }
    }

    fun download(): String {
        val response = execute(
            Request.Builder()
                .url(targetUrl())
                .get()
                .authenticated()
                .build()
        )
        response.use {
            val body = it.body ?: throw WebDavException("WebDAV returned an empty backup")
            if (body.contentLength() > MAX_TRANSFER_BYTES) {
                throw WebDavException("Remote backup is too large")
            }
            return body.readUtf8Limited()
        }
    }

    private fun ensureDirectories() {
        if (pathSegments.size <= 1) return
        val builder = baseUrl.newBuilder()
        pathSegments.dropLast(1).forEach { segment ->
            builder.addPathSegment(segment)
            val request = Request.Builder()
                .url(builder.build())
                .method("MKCOL", EMPTY_BODY)
                .authenticated()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 405) {
                    throw response.toException("Unable to create WebDAV directory")
                }
            }
        }
    }

    private fun targetUrl(): HttpUrl = baseUrl.newBuilder().apply {
        pathSegments.forEach(::addPathSegment)
    }.build()

    private fun temporaryUrl(): HttpUrl = baseUrl.newBuilder().apply {
        pathSegments.dropLast(1).forEach(::addPathSegment)
        addPathSegment(pathSegments.last() + ".uploading")
    }.build()

    private fun deleteTemporary(url: HttpUrl) {
        client.newCall(
            Request.Builder()
                .url(url)
                .delete()
                .authenticated()
                .build()
        ).execute().use { response ->
            if (!response.isSuccessful && response.code != 404) {
                throw response.toException("Unable to clean up temporary WebDAV backup")
            }
        }
    }

    private fun Request.Builder.authenticated(): Request.Builder = apply {
        header("Authorization", authorization)
        header("User-Agent", "NekoBox/${BuildConfig.VERSION_NAME}")
    }

    private fun execute(request: Request) = client.newCall(request).execute().also { response ->
        if (!response.isSuccessful) {
            val exception = response.toException("WebDAV request failed")
            response.close()
            throw exception
        }
    }

    private fun okhttp3.Response.toException(prefix: String): WebDavException {
        val detail = runCatching {
            val buffer = ByteArray(MAX_ERROR_BYTES)
            val count = body?.byteStream()?.read(buffer) ?: -1
            if (count > 0) String(buffer, 0, count, Charsets.UTF_8).trim().take(200)
            else null
        }.getOrNull()
        return WebDavException(
            buildString {
                append(prefix)
                append(" (HTTP ")
                append(code)
                append(')')
                if (!detail.isNullOrBlank()) {
                    append(": ")
                    append(detail)
                }
            },
            code,
        )
    }

    private fun ResponseBody.readUtf8Limited(): String {
        val initialSize = contentLength()
            .takeIf { it in 1..MAX_TRANSFER_BYTES.toLong() }
            ?.toInt()
            ?: DEFAULT_BUFFER_SIZE
        val output = ByteArrayOutputStream(initialSize)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        byteStream().use { input ->
            var total = 0L
            while (true) {
                val maxRead = minOf(
                    buffer.size.toLong(),
                    MAX_TRANSFER_BYTES.toLong() + 1L - total,
                ).toInt()
                val count = input.read(buffer, 0, maxRead)
                if (count < 0) break
                total += count
                if (total > MAX_TRANSFER_BYTES) {
                    throw WebDavException("Remote backup is too large")
                }
                output.write(buffer, 0, count)
            }
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private companion object {
        const val MAX_TRANSFER_BYTES = 24 * 1024 * 1024
        const val MAX_ERROR_BYTES = 512
        val BACKUP_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val EMPTY_BODY = ByteArray(0).toRequestBody(null)
        val HTTP_CLIENT = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}
