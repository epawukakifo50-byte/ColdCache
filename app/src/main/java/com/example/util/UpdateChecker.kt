package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val releasePageUrl: String,
    val apkDownloadUrl: String?,
    val isUpdateAvailable: Boolean
)

object UpdateChecker {
    private const val GITHUB_REPO = "epawukakifo50-byte/ColdCache"
    private const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(RELEASES_API_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "ColdCache-Android")
                connectTimeout = 6000
                readTimeout = 6000
            }

            if (conn.responseCode != 200) return@withContext null

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            val tagName = json.optString("tag_name", "").trim()
            val cleanLatest = tagName.removePrefix("v").trim()
            val cleanCurrent = BuildConfig.VERSION_NAME.removePrefix("v").trim()
            val name = json.optString("name", tagName)
            val body = json.optString("body", "")
            val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO/releases")

            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                        break
                    }
                }
            }

            val isAvailable = isNewerVersion(cleanLatest, cleanCurrent)

            AppUpdateInfo(
                latestVersion = tagName,
                currentVersion = "v$cleanCurrent",
                releaseTitle = name,
                releaseNotes = body,
                releasePageUrl = htmlUrl,
                apkDownloadUrl = apkUrl ?: htmlUrl,
                isUpdateAvailable = isAvailable
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        if (latest.isBlank() || current.isBlank()) return false
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(latestParts.size, currentParts.size)

        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun downloadUpdate(context: Context, url: String, fileName: String = "ColdCache-Update.apk") {
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager
            if (dm != null && url.startsWith("http")) {
                val request = android.app.DownloadManager.Request(Uri.parse(url)).apply {
                    setTitle(fileName)
                    setDescription("Загрузка обновления ColdCache...")
                    setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                    setMimeType("application/vnd.android.package-archive")
                }
                dm.enqueue(request)
                android.widget.Toast.makeText(
                    context,
                    "Загрузка началась. Проверьте шторку уведомлений!",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            }
        } catch (_: Exception) {}

        // Fallback to browser
        openDownload(context, url)
    }

    fun openDownload(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
