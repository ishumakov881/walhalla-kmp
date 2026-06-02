package net.example.online.push

import dev.walhalla.kmp.device.Installations
import dev.walhalla.kmp.device.AppDeviceInfo
import dev.walhalla.kmp.device.provideLdsWebAppDeviceInfo
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay



object PushTokenSender {
    private const val DEVICE_TOKEN_URL = "https://.../app/1.0/device/token"
    private const val MAX_ATTEMPTS = 5
    private const val RETRY_DELAY_MS = 3_000L

    private val client = HttpClient()

    suspend fun sendCurrentTokenIfNeeded(fcmToken: String): Boolean {
        val token = fcmToken.trim()
        if (token.isEmpty()) return false

        val deviceId = Installations.deviceId().trim()
        if (deviceId.isEmpty()) return false

        val sentKey = "$deviceId:$token"
        if (PushSettingsStorage.getLastSentPushTokenKey() == sentKey) {
            return true
        }

        repeat(MAX_ATTEMPTS) { attempt ->
            val success = runCatching {
                sendToken(provideLdsWebAppDeviceInfo(), token)
            }.getOrElse { error ->
                println("FCM token send attempt ${attempt + 1}/$MAX_ATTEMPTS error: ${error.message}")
                false
            }

            if (success) {
                PushSettingsStorage.setLastSentPushTokenKey(sentKey)
                return true
            }

            if (attempt < MAX_ATTEMPTS - 1) {
                delay(RETRY_DELAY_MS)
            }
        }

        println("FCM token send failed after $MAX_ATTEMPTS attempts")
        return false
    }

    private suspend fun sendToken(
        deviceInfo: AppDeviceInfo,
        fcmToken: String,
    ): Boolean {
        val response = client.submitForm(
            url = DEVICE_TOKEN_URL,
            formParameters = Parameters.build {
                append("device_id", Installations.deviceId())
                append("fcm_token", fcmToken)
            },
        ) {
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.UserAgent, "MobileCabinet/${deviceInfo.appVersionName}")
        }

        val body = response.bodyAsText()
        val success = response.status.isSuccess() && body.contains("\"status\":\"success\"")
        println("FCM token sent to server: $success ${response.status.value}")
        return success
    }
}
