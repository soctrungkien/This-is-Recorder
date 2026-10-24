package com.hihi.recorder.net

import android.content.Context
import com.hihi.recorder.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Metadata for a newer GitHub release. */
data class UpdateInfo(
    val versionName: String,
    val tagName: String,
    val releaseUrl: String,
    val notes: String,
)

enum class UpdateFailure {
    NO_RELEASES,
    NETWORK,
    RATE_LIMIT,
    SERVER,
    INVALID_RESPONSE,
}

sealed interface UpdateResult {
    data object UpToDate : UpdateResult
    data class Available(val info: UpdateInfo) : UpdateResult
    data class Failed(val reason: UpdateFailure) : UpdateResult
}

/**
 * Checks the official GitHub release feed through gh-proxy. No analytics or
 * identifiers are sent; this is a single anonymous GET. The proxy exists
 * because api.github.com and release assets are often unreachable directly in
 * the regions where this app is used.
 */
object UpdateChecker {

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/soctrungkien/This-is-Recorder/releases/latest"
    private const val GITHUB_PROXY = "https://gh-proxy.com/"
    private const val RELEASE_PAGE =
        "https://github.com/soctrungkien/This-is-Recorder/releases"

    /** Automatic checks run on every app start for official (non-debug) builds. */
    fun shouldCheckAutomatically(context: Context): Boolean {
        // Touch applicationContext so callers cannot accidentally leak an Activity.
        context.applicationContext
        return !BuildConfig.DEBUG
    }

    private fun proxied(url: String): String =
        if (url.startsWith(GITHUB_PROXY)) url else GITHUB_PROXY + url

    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        val throughProxy = request(proxied(LATEST_RELEASE_URL))
        if (throughProxy is UpdateResult.Failed &&
            throughProxy.reason in setOf(UpdateFailure.NETWORK, UpdateFailure.SERVER)
        ) {
            // If the mirror is down, keep update checks useful by trying the
            // official endpoint as a last resort.
            request(LATEST_RELEASE_URL)
        } else {
            throughProxy
        }
    }

    private fun request(url: String): UpdateResult {
        val connection = runCatching {
            (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "ThisIsRecorder/${BuildConfig.VERSION_NAME}")
            }
        }.getOrElse {
            return UpdateResult.Failed(UpdateFailure.NETWORK)
        }

        return try {
            val code = runCatching { connection.responseCode }.getOrElse {
                return UpdateResult.Failed(UpdateFailure.NETWORK)
            }
            when {
                code == HttpURLConnection.HTTP_NOT_FOUND ->
                    return UpdateResult.Failed(UpdateFailure.NO_RELEASES)
                code == 403 || code == 429 ->
                    return UpdateResult.Failed(UpdateFailure.RATE_LIMIT)
                code !in 200..299 ->
                    return UpdateResult.Failed(UpdateFailure.SERVER)
            }

            val body = runCatching {
                connection.inputStream.bufferedReader().use { it.readText() }
            }.getOrElse {
                return UpdateResult.Failed(UpdateFailure.NETWORK)
            }
            parse(body)
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    internal fun parse(body: String, currentVersion: String = BuildConfig.VERSION_NAME): UpdateResult {
        return runCatching {
            val json = JSONObject(body)
            if (json.optBoolean("draft", false) || json.optBoolean("prerelease", false)) {
                return UpdateResult.UpToDate
            }
            val tag = json.optString("tag_name").trim().removePrefix("v")
            if (tag.isBlank()) return UpdateResult.Failed(UpdateFailure.INVALID_RESPONSE)
            if (!isVersionNewer(tag, currentVersion)) return UpdateResult.UpToDate

            val releaseUrl = json.optString("html_url").takeIf { it.isNotBlank() }
                ?: RELEASE_PAGE
            var downloadUrl = proxied(releaseUrl)
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true) && !name.contains("debug", true)) {
                        val candidate = asset.optString("browser_download_url")
                        if (candidate.isNotBlank()) {
                            downloadUrl = proxied(candidate)
                            break
                        }
                    }
                }
            }

            UpdateResult.Available(
                UpdateInfo(
                    versionName = tag,
                    tagName = json.optString("tag_name"),
                    releaseUrl = downloadUrl,
                    notes = json.optString("body").trim(),
                ),
            )
        }.getOrElse {
            UpdateResult.Failed(UpdateFailure.INVALID_RESPONSE)
        }
    }

    /** Compares dotted numeric versions. Pre-release suffixes are ignored. */
    fun isVersionNewer(candidate: String, current: String): Boolean {
        fun parts(value: String): List<Int> = value
            .trim()
            .removePrefix("v")
            .substringBefore('-')
            .substringBefore('+')
            .split('.')
            .map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

        val remote = parts(candidate)
        val local = parts(current)
        val size = maxOf(remote.size, local.size)
        for (index in 0 until size) {
            val remotePart = remote.getOrElse(index) { 0 }
            val localPart = local.getOrElse(index) { 0 }
            if (remotePart != localPart) return remotePart > localPart
        }
        return false
    }
}
