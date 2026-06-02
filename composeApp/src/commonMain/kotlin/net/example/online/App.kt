package net.example.online

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import com.multiplatform.webview.jsbridge.WebViewJsBridge
import com.multiplatform.webview.request.RequestInterceptor
import com.multiplatform.webview.request.WebRequest
import com.multiplatform.webview.request.WebRequestInterceptResult
import net.example.online.permissions.BindBlobSaveStoragePermission
import net.example.online.permissions.PushNotificationPermissionOnResume
import net.example.online.push.PushInbox
import net.example.online.push.PushWebViewBridge
import net.example.online.push.ParsedPush
import net.example.online.push.PushTokenSender
import net.example.online.push.ui.PushInboxTopBarAction
import net.example.online.push.ui.PushMessagesInboxScreen
import net.example.online.ui.webview.ErrorContent
import net.example.online.webview.WebViewUrlHandler
import net.example.online.webview.WebAppAuthStorage
import net.example.online.webview.configureBlankTargetHandling

import net.example.online.webview.registerLdsWebAppJsHandlers

import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewError
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import dev.walhalla.kmp.device.Installations
import dev.walhalla.kmp.device.provideLdsWebAppDeviceInfo
import kotlinx.coroutines.launch

@Composable
fun App() {


    MaterialTheme {
//        var showContent by remember { mutableStateOf(false) }
//        Column(
//            modifier = Modifier
//                .background(MaterialTheme.colorScheme.primaryContainer)
//                .safeContentPadding()
//                .fillMaxSize(),
//            horizontalAlignment = Alignment.CenterHorizontally,
//        ) {
//            Button(onClick = { showContent = !showContent }) {
//                Text("Click me!")
//            }
//            AnimatedVisibility(showContent) {
//                val greeting = remember { Greeting().greet() }
//                Column(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                ) {
//                    Image(painterResource(Res.drawable.compose_multiplatform), null)
//                    Text("Compose: $greeting")
//                }
//            }
//        }

        WebViewScreen()

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen() {
    PushNotificationPermissionOnResume()
    BindBlobSaveStoragePermission()

    //val pushNotifier = NotifierManager.getPushNotifier()
    //pushNotifier.getToken() //Get current user push notification token

    var tokenValue by remember { mutableStateOf("NONE") }
    var pendingPush by remember { mutableStateOf<ParsedPush?>(null) }
    var showPushInbox by remember { mutableStateOf(false) }
    var webLoadError by remember { mutableStateOf<WebLoadErrorUiState?>(null) }
    var webErrorRetryTick by remember { mutableStateOf(0) }
    var showIdsDialog by remember { mutableStateOf(false) }

    val pushCounts by PushInbox.counts.collectAsState()
    val pendingOpenInbox by PushInbox.pendingOpenInbox.collectAsState()

    LaunchedEffect(pendingOpenInbox) {
        if (pendingOpenInbox) {
            PushInbox.consumeOpenInboxRequest()
            showPushInbox = true
        }
    }

    val state = rememberWebViewState("https://google.com")

    val urlHandler = remember {
        WebViewUrlHandler(
            downloadFileTypes = arrayOf(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".zip", ".apk"),
            linksOpenedInExternalBrowser = arrayOf("vkontakte", "vk.com"),
            isCheckSameDomainEnabled = true,
            homeDomain9 = "google.com",
        )
    }

    val navigator = rememberWebViewNavigator(
        requestInterceptor = object : RequestInterceptor {
            override fun onInterceptUrlRequest(request: WebRequest, navigator: WebViewNavigator)
            : WebRequestInterceptResult {
                // Редирект/подмена URL до загрузки в WebView (опционально, до handler)
//                if (request.url.contains("vkontakte")) {
//                    return WebRequestInterceptResult.Modify(
//                        WebRequest(
//                            url = "https://kotlinlang.org/docs/multiplatform.html",
//                            headers = request.headers,
//                        ),
//                    )
//                }
                if (request.url.startsWith("blob:")) {
                    multiplatform.network.cmptoast.showToast("Загрузка...")
                    val fileName = "download_${kotlin.time.Clock.System.now().toEpochMilliseconds()}"
                    navigator.evaluateJavaScript(
                        net.example.online.webview.BlobDownloadScript.build(
                            blobUrl = request.url,
                            mimeType = "application/octet-stream",
                            fileName = fileName,
                        ),
                    )
                    return WebRequestInterceptResult.Reject
                }
                val x = urlHandler.interceptUrlRequest(request)
                //showToast("Link intercepted: $x")
                return x
            }
        },
    )

    //val jsBridge = rememberWebViewJsBridge()
    val jsBridge = remember { WebViewJsBridge(/*jsBridgeName = "Android"*/jsBridgeName = "kmpJsBridge") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        tokenValue = NotifierManager.getPushNotifier().getToken() ?: ""
        println("Token: $tokenValue")
        PushTokenSender.sendCurrentTokenIfNeeded(tokenValue)
    }

    LaunchedEffect(state.loadingState, pushCounts.unread) {
        if (state.loadingState is LoadingState.Finished) {
            //navigator.evaluateJavaScript("alert()")
            PushWebViewBridge.syncPushMessagesButton(navigator, pushCounts.unread)
        }
    }

    LaunchedEffect(state.errorsForCurrentRequest.toList(), state.loadingState, webErrorRetryTick) {
        val loadingState = state.loadingState

        val errors = state.errorsForCurrentRequest.toList()
        //val selected = errors.lastOrNull { it.isFromMainFrame } ?: errors.lastOrNull()
        // parity with old appWebView: show error UI only for main page failures,
        // ignore secondary resource failures (images, scripts, etc.)
        val selected = errors.lastOrNull { it.isFromMainFrame }

        if (selected != null && shouldShowWebError(selected.code)) {
            webLoadError = toWebLoadErrorUi(selected)
        } else if (loadingState is LoadingState.Finished) {
            webLoadError = null
        }
    }

    //other s*t state.cookieManager



    val webAppDeviceInfo = remember { provideLdsWebAppDeviceInfo() }
    val currentPushToken by rememberUpdatedState(tokenValue)
    val openPushInbox by rememberUpdatedState({ showPushInbox = true })


    state.webSettings.apply {

        desktopWebSettings.apply {
            isJavaScriptEnabled = true
            allowFileAccessFromFileURLs = true
        }

        iOSWebSettings.apply {
            isJavaScriptEnabled = true
            allowFileAccessFromFileURLs = true
        }

        isJavaScriptEnabled = true
        //customUserAgentString = "@@@"
        androidWebSettings.apply {
            // Grants RESOURCE_PROTECTED_MEDIA_ID permission, default false
            allowProtectedMedia = true
            // Grants RESOURCE_MIDI_SYSEX permission, default false
            allowMidiSysexMessages = true


            // Основные настройки
            supportZoom = false
            // Загрузка и кэширование
            loadsImagesAutomatically = true

            // JavaScript и DOM
            isJavaScriptEnabled = true
            domStorageEnabled = true

            // Доступ к файлам
            allowFileAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true

            // User Agent
            //Mozilla/5.0 (Linux; Android 9; SM-G9880 Build/PQ3B.190801.10101846; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.82 Mobile Safari/537.36
            //"Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:134.0) Gecko/20100101 Firefox/134.0"//


//            val originalUA = customUserAgentString
//            userAgentString = fixUserAgent(originalUA).replace("; wv)", ")")
//            println("Original UA: $originalUA")
//            println("   Fixed UA: $userAgentString")
        }
        // ...
    }


    LaunchedEffect(Unit) {
        PushInbox.refreshCounts()
        pendingPush = PushInbox.consumePendingDialog()

        NotifierManager.addListener(object : NotifierManager.Listener {
            override fun onNewToken(token: String) {
                scope.launch {
                    tokenValue = token
                    println("Firebase Push Token: $token")
                    PushTokenSender.sendCurrentTokenIfNeeded(token)
                }
            }

            override fun onPushNotificationWithPayloadData(
                title: String?,
                body: String?,
                data: PayloadData,
            ) {
                scope.launch {
                    pendingPush = PushInbox.consumePendingDialog()
                }
            }

            override fun onNotificationClicked(data: PayloadData) {
                super.onNotificationClicked(data)
                scope.launch {
                    pendingPush = PushInbox.consumePendingDialog()
                    PushInbox.requestOpenInbox()
                }
            }
        })
    }



    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(
            title = {
                val title = state.pageTitle?.takeIf { it.isNotBlank() } ?: "Title"
                Text(text = title, overflow = TextOverflow.Ellipsis, maxLines = 1)
            },
            navigationIcon = {
                if (navigator.canGoBack) {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            },
            actions = {
                PushInboxTopBarAction(
                    unreadCount = pushCounts.unread,
                    totalCount = pushCounts.total,
                    onOpenInbox = { showPushInbox = true },
                )

                Button(onClick = {
                    PushInbox.test()
//                    navigator.evaluateJavaScript(
//                        //"typeof window.Android !== 'undefined' && typeof window.showPushMessagesButton !== 'undefined'",
//            "typeof window.showPushMessagesButton !== 'undefined'",
//            {
//                showToast("is jsBridge attach success? : $it")

//                navigator.evaluateJavaScript("showPushMessagesButton(${pushCounts.unread});", {
//                    //showToast(it)
//                })
//            })

//                        PushWebViewBridge.syncPushMessagesButton(navigator, pushCounts.unread)
                        //navigator.loadHtml("javascript:showPushMessagesButton(0);")
//                        navigator.evaluateJavaScript("showPushMessagesButton(0);",{
//                            navigator.evaluateJavaScript("alert($it);",{
//                                println("@@@ $it")
//                            })
//                        })
                }) {
                    Text("test msg")
                }


                Button(onClick = { showIdsDialog = true }) {
                    Text("IDs")
                }
            },
        )
    }) { paddingValues ->
        Box(
            modifier = Modifier
                //.background(MaterialTheme.colorScheme.primaryContainer)
                //.safeContentPadding()
                .fillMaxSize().padding(paddingValues)
        ) {
            Column {


                //Text(text = "${state.pageTitle}")

                val loadingState = state.loadingState
                if (loadingState is LoadingState.Loading) {
                    LinearProgressIndicator(
                        progress = { loadingState.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = ProgressIndicatorDefaults.linearColor,
                        trackColor = ProgressIndicatorDefaults.linearTrackColor,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )
                }
                WebView(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    webViewJsBridge = jsBridge,
                    navigator = navigator,
                    onCreated = {
                        native ->
                        configureBlankTargetHandling(native)
                    },
                )
            }


            LaunchedEffect(Unit) {
                registerLdsWebAppJsHandlers(
                    jsBridge = jsBridge,
                    deviceInfo = webAppDeviceInfo,
                    pushTokenProvider = {
                        currentPushToken.takeIf {
                            it.isNotBlank() && it != "NONE" && !it.startsWith(
                                "##"
                            )
                        }
                    },
                    onOpenInbox = { openPushInbox() },
                )
            }

            //Text(text = formatToken(tokenValue)?:"---", color = Color.Red)

            webLoadError?.let { webError ->
                ErrorContent(
                    title = webError.title,
                    description = webError.description,
                    isLoading = state.loadingState is LoadingState.Loading,
                    onReload = {
                        navigator.reload()
                    },
                )
            }

            //Text(text = "${webLoadError?.description}", color = Color.Red)




            //DIALOGS

//    pendingPush?.let { push ->
//        AlertDialog(
//            onDismissRequest = {
//                PushInbox.markRead(push.letter.id, push.userName)
//                pendingPush = null
//            },
//            title = { Text(push.letter.title) },
//            text = { Text(push.letter.text) },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        PushInbox.markRead(push.letter.id, push.userName)
//                        pendingPush = null
//                    },
//                ) {
//                    Text("OK")
//                }
//            },
//            dismissButton = {
//                TextButton(
//                    onClick = {
//                        pendingPush = null
//                        showPushInbox = true
//                    },
//                ) {
//                    Text("Все сообщения")
//                }
//            },
//        )
//    }


            if (showIdsDialog) {
                val info = webAppDeviceInfo
                val installId = Installations.installId()
                val authSession = WebAppAuthStorage.load()
                val authToken = authSession?.sessionToken
                val fcmToken = currentPushToken.takeIf { it.isNotBlank() && it != "NONE" }

                Dialog(
                    onDismissRequest = { showIdsDialog = false },
                    properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().zIndex(100f),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .heightIn(max = 480.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text("LdsWebAppDeviceInfo")
                            Text("appName: ${info.appName}")
                            Text("appVersionName: ${info.appVersionName}")
                            Text("appVersionCode: ${info.appVersionCode}")
                            Text("installSource: ${info.installSource}")
                            Text("deviceModel: ${info.deviceModel}")
                            Text("deviceManufacturer: ${info.deviceManufacturer}")
                            Text("osVersion: ${info.osVersion}")
                            Text("deviceId: ${info.deviceId}")
                            Text("locale: ${info.locale}")
                            Text("timeZone: ${info.timeZone}")
                            Text("")
                            Text("Дополнительно")
                            Text("installId: ${installId ?: "<null>"}")
                            Text("authToken: ${authToken ?: "<null>"}")
                            Text("fcmToken: ${fcmToken ?: "<null>"}")
                            TextButton(onClick = { showIdsDialog = false }) { Text("OK") }
                        }
                    }
                }
            }

            if (showPushInbox) {
                Dialog(
                    onDismissRequest = { showPushInbox = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        PushMessagesInboxScreen(onDismiss = { showPushInbox = false })
                    }
                }
            }
            //END_DIALOGS
        }
    }






}

private data class WebLoadErrorUiState(
    val title: String,
    val description: String,
)

private fun toWebLoadErrorUi(error: WebViewError): WebLoadErrorUiState {
    val (title, defaultDescription) = when (error.code) {
        WEBVIEW_ERROR_HOST_LOOKUP ->
            "Нет подключения к интернету" to "Проверьте интернет и повторите попытку."
        WEBVIEW_ERROR_CONNECT ->
            "Сервер недоступен" to "Не удалось подключиться к серверу."
        WEBVIEW_ERROR_TIMEOUT ->
            "Превышено время ожидания" to "Сервер отвечает слишком долго. Попробуйте еще раз."
        WEBVIEW_ERROR_PROXY_AUTHENTICATION ->
            "Ошибка прокси" to "Проверьте настройки сети и прокси."
        else ->
            "Ошибка загрузки страницы" to "Не удалось загрузить страницу."
    }

    val description = error.description.takeIf { it.isNotBlank() } ?: defaultDescription
    return WebLoadErrorUiState(title = title, description = description)
}

private fun shouldShowWebError(code: Int): Boolean {
    // Аналог старого appWebView: пропускаем file-not-found как второстепенный кейс.
    return code != WEBVIEW_ERROR_FILE_NOT_FOUND
}

private const val WEBVIEW_ERROR_HOST_LOOKUP = -2
private const val WEBVIEW_ERROR_PROXY_AUTHENTICATION = -5
private const val WEBVIEW_ERROR_CONNECT = -6
private const val WEBVIEW_ERROR_TIMEOUT = -8
private const val WEBVIEW_ERROR_FILE_NOT_FOUND = -14