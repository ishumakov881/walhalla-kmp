package net.example.online.utils

import multiplatform.network.cmptoast.showToast
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

internal object JvmDesktopUtils {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    fun openUrl(url: String): Boolean {
        if (!Desktop.isDesktopSupported()) {
            showToast("Открытие ссылок не поддерживается на этой платформе")
            return false
        }
        return try {
            Desktop.getDesktop().browse(URI(url))
            true
        } catch (e: Exception) {
            showToast("Не удалось открыть ссылку: ${e.message ?: url}")
            false
        }
    }

    fun openMailto(email: String, subject: String?, body: String?) {
        val uri = buildMailtoUri(email, subject, body)
        if (!Desktop.isDesktopSupported()) {
            showToast("Почтовый клиент недоступен")
            return
        }
        try {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.MAIL)) {
                desktop.mail(uri)
            } else {
                desktop.browse(uri)
            }
        } catch (e: Exception) {
            if (!openUrl(uri.toString())) {
                showToast("Не удалось открыть почту: ${e.message ?: email}")
            }
        }
    }

    fun mailTo(url: String) {
        openUrl(url)
    }

    fun shareText(subject: String?, text: String) {
        if (!Desktop.isDesktopSupported()) {
            copyToClipboard(text)
            return
        }
        try {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.MAIL)) {
                desktop.mail(buildMailtoUri("", subject, text))
                return
            }
        } catch (_: Exception) {
            // fallback to clipboard
        }
        copyToClipboard(buildString {
            subject?.let {
                append(it)
                append("\n\n")
            }
            append(text)
        })
        showToast("Текст скопирован в буфер обмена")
    }

    fun downloadFile(url: String, fileName: String) {
        Thread {
            runCatching {
                doDownload(url, sanitizeFileName(fileName))
            }.onFailure { error ->
                showToast("Ошибка загрузки: ${error.message ?: error::class.simpleName}")
            }
        }.start()
    }

    private fun doDownload(url: String, fileName: String) {
        val request = HttpRequest.newBuilder()
            .uri(URI(url))
            .timeout(Duration.ofMinutes(5))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() !in 200..299) {
            showToast("Ошибка загрузки: HTTP ${response.statusCode()}")
            return
        }

        val downloadsDir = Paths.get(System.getProperty("user.home"), "Downloads")
        Files.createDirectories(downloadsDir)
        val target = resolveUniquePath(downloadsDir, fileName)
        Files.write(target, response.body())

        showToast("Файл сохранён: ${target.fileName}")

        if (Desktop.isDesktopSupported()) {
            runCatching {
                Desktop.getDesktop().open(target.parent.toFile())
            }
        }
    }

    private fun resolveUniquePath(directory: Path, fileName: String): Path {
        var candidate = directory.resolve(fileName)
        if (!Files.exists(candidate)) return candidate

        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        var index = 1
        while (Files.exists(candidate)) {
            candidate = directory.resolve("$base ($index)$ext")
            index++
        }
        return candidate
    }

    private fun sanitizeFileName(fileName: String): String {
        val cleaned = fileName
            .replace('\\', '_')
            .replace('/', '_')
            .replace(':', '_')
            .trim()
            .take(200)
        return cleaned.ifEmpty { "download" }
    }

    private fun buildMailtoUri(email: String, subject: String?, body: String?): URI {
        val query = buildList {
            subject?.takeIf { it.isNotEmpty() }?.let {
                add("subject=${encodeMailto(it)}")
            }
            body?.takeIf { it.isNotEmpty() }?.let {
                add("body=${encodeMailto(it)}")
            }
        }
        val base = if (email.isEmpty()) "mailto:" else "mailto:$email"
        val uriString = if (query.isEmpty()) base else "$base?${query.joinToString("&")}"
        return URI(uriString)
    }

    private fun encodeMailto(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }

    fun extractQueryParam(url: String, param: String): String? {
        val queryStart = url.indexOf('?')
        if (queryStart < 0) return null
        return url.substring(queryStart + 1)
            .split('&')
            .firstNotNullOfOrNull { part ->
                val eq = part.indexOf('=')
                if (eq > 0 && part.substring(0, eq) == param) {
                    part.substring(eq + 1)
                } else {
                    null
                }
            }
    }
}
