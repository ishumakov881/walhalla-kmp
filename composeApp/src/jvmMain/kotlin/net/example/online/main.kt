package net.example.online

import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.multiplatform.webview.util.addTempDirectoryRemovalHook
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import multiplatform.network.cmptoast.ToastHost
import java.io.File
import kotlin.math.max

/**
 * Должно быть до первого обращения Compose/Skiko к GPU.
 * На Windows без нормального D3D12 (RDP, VM, старый драйвер) иначе:
 * `RenderException: Failed to create DirectX12 device` и пустое окно.
 *
 * Переопределение: `-Dskiko.renderApi=OPENGL` или `SOFTWARE` в JVM args (`compose.desktop` в Gradle).
 */
private fun configureSkikoRenderApi() {
    if (System.getProperty("skiko.renderApi") == null) {
        System.setProperty("skiko.renderApi", "OPENGL")
    }
}

private fun appDataDir(): File =
    File(System.getProperty("user.home"), ".lds-online").apply { mkdirs() }

//fun main() {
//    configureSkikoRenderApi()
//    application {
//        Window(
//            onCloseRequest = ::exitApplication,
//            title = "WebView_iOS",
//        ) {
//            NotifierManager.initialize(
//                configuration = NotificationPlatformConfiguration.Desktop(
//                    showPushNotification = true
//                )
//            )
//            App()
//        }
//    }
//}
fun main() {
    configureSkikoRenderApi()

    application {
        addTempDirectoryRemovalHook()
        Window(
            onCloseRequest = ::exitApplication,
            title = "Title",
        ) {
            var restartRequired by remember { mutableStateOf(false) }
            var downloading by remember { mutableStateOf(0F) }
            var initialized by remember { mutableStateOf(false) }
            //val download: KCEFBuilder.Download = remember { Builder().github().build() }

            NotifierManager.initialize(
                configuration = NotificationPlatformConfiguration.Desktop(
                    showPushNotification = true
                )
            )

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    KCEF.init(builder = {
                        installDir(File(appDataDir(), "kcef-bundle"))

                        /*
                      Add this code when using JDK 17.
                      Builder().github {
                          release("jbr-release-17.0.10b1087.23")
                      }.buffer(download.bufferSize).build()
                         */
                        progress {
                            onDownloading {
                                downloading = max(it, 0F)
                            }
                            onInitialized {
                                initialized = true
                            }
                        }
                        settings {
                            cachePath = File(appDataDir(), "kcef-cache").absolutePath
                            persistSessionCookies = true
                        }
                    }, onError = {
                        it?.printStackTrace()
                    }, onRestartRequired = {
                        restartRequired = true
                    })
                }
            }

            if (restartRequired) {
                Text(text = "Restart required.")
            } else {
                if (initialized) {
                    App()
                } else {
                    Text(text = "Downloading $downloading%")
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    KCEF.disposeBlocking()
                }
            }

            ToastHost()
        }
    }
}