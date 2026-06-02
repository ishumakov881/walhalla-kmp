package net.example.online.webview


import com.chrynan.uri.core.Uri
import com.chrynan.uri.core.fromString

import com.multiplatform.webview.request.WebRequest
import com.multiplatform.webview.request.WebRequestInterceptResult
import net.example.online.utils.ActivityUtils
import multiplatform.network.cmptoast.showToast
import net.example.online.utils.DownloadUtility
import net.example.online.utils.getFileName
import net.example.online.utils.isLinkInternal


class WebViewUrlHandler(
    val downloadFileTypes: Array<String>, val linksOpenedInExternalBrowser: Array<String>,
    var isCheckSameDomainEnabled: Boolean, var homeDomain9: String? = null
) {
    val TAG = "@@@"


    private fun isSameDomain(url: String, baseDomain: String?): Boolean {
        val uri = Uri.fromString(uriString = url)
        val domain = uri.host

        val result =
            if (domain != null && (domain.endsWith(".$baseDomain") || domain == baseDomain)) {
                true
            } else {
                false
            }


        println("isSameDomain: $domain $baseDomain $result")

        //o.php?
        return result
    }

    fun isLinkExternal(url: String): Boolean {
        for (rule in linksOpenedInExternalBrowser) {
            if (url.contains(rule)) return true
        }
        return false
    }

    fun isDownloadableFile(url: String): Boolean {
        //if(url.startsWith("data:")) return true
        var url = url
        val index = url.indexOf("?")
        if (index > -1) {
            url = url.substring(0, index)
        }
        url = url.lowercase()
        for (type in downloadFileTypes) {
            if (url.endsWith(type)) return true
        }
        return false
    }

    fun handleUrl(url: String): Boolean {
        val var0 = isDownloadableFile(url)
        //Toast.makeText(view."@@@@")
        if (var0) {
            multiplatform.network.cmptoast.showToast("Загрузка...")
            DownloadUtility.downloadFile(url, getFileName(url))
            return true
        } else if (url.startsWith("https://telegram.me") || url.startsWith("tg:") || url.startsWith(
                "https://t.me/"
            )
        ) {
            ActivityUtils.starttg(url)
            return true //handle itself
        } else if (url.startsWith("file:///android_asset")) {
            showToast(message = "@@@@@@@@@")
            return false
        } else if ((url.startsWith("http://") || url.startsWith("https://"))) {
//            if (url.startsWith("https://accounts.google.com/o/oauth2") || url.contains("redirect_uri=")) {
//                // Новое значение для параметра redirect_uri
//                String encodedRedirectUri = "";
//                try {
//                    String newRedirectUri = context.getPackageName() + "://app/";
//                    encodedRedirectUri = URLEncoder.encode(newRedirectUri, "UTF-8");
//                } catch (UnsupportedEncodingException ignored) {
//                }
//                // Разбиваем строку по символу '&' для получения параметров
//                String[] parts = url.split("&");
//
//                // Проходим по каждому параметру и заменяем redirect_uri на новое значение
//                StringBuilder modifiedUrlBuilder = new StringBuilder();
//                for (String part : parts) {
//                    if (part.startsWith("redirect_uri=")) {
//                        // Заменяем старое значение на новое
//                        modifiedUrlBuilder.append("redirect_uri=").append(encodedRedirectUri);
//                    } else {
//                        // Сохраняем остальные параметры без изменений
//                        modifiedUrlBuilder.append(part);
//                    }
//                    // Добавляем '&' после каждого параметра, кроме последнего
//                    modifiedUrlBuilder.append("&");
//                }
//
//                // Удаляем лишний '&' в конце строки
//                String modifiedUrl = modifiedUrlBuilder.toString().substring(0, modifiedUrlBuilder.length() - 1);
//                println(TAG + "@" + modifiedUrl);
//                chromeView.openOauth2(modifiedUrl);
//                return true;
//            }


            //println(TAG + "@c@");
            // determine for opening the link externally or internally

            var openInExternalApp = isLinkExternal(url) //openInExternalApp app
            val internal = isLinkInternal(url) //internal webView
            if (!openInExternalApp && !internal) {
                openInExternalApp = WebViewAppConfig.OPEN_LINKS_IN_EXTERNAL_BROWSER
            }
            //My new Code
            if (url.endsWith(".apk")) {
                ActivityUtils.openBrowser(url)
                return true
            }

            // open the link
            if (openInExternalApp) {
                println("$TAG@@@ $")
                ActivityUtils.openBrowser(url)
                return true
            } else {
                if (isCheckSameDomainEnabled) {
                    if (isSameDomain(url, homeDomain9)) {
                        println(
                            TAG + "NOT_OVERRIDE:isSameDomain: ${homeDomain9} :: $url"
                        )
                        return false
                    } else {
                        println(
                            TAG + "blocked: $url, ${homeDomain9}"
                        )

                        //var 1
                        //url blocked
                        ActivityUtils.openBrowser(url)
                        return true
                    }
                } else {
                    //@@@ showActionBarProgress(true);
                    println(TAG + "NOT_OVERRIDE: ... $url")
                    return false
                }
            }
        } else if (url.startsWith("mailto:")) {
            ActivityUtils.mailTo(url)
            return true
        } else if (url.startsWith("whatsapp://send?phone=")) {
            val url2 =
                "https://api.whatsapp.com/send?phone=" + url.replace("whatsapp://send?phone=", "")
            ActivityUtils.sendWhatsappPhone(url2)
            return true
        } else if (url.startsWith("https://api.whatsapp.com/send?phone=") || url.startsWith("https://api.whatsapp.com/send/?phone=")) {
            ActivityUtils.sendWhatsappPhone(url)
            return true //bs
        } else if (url.startsWith("whatsapp://send?text=")) {
            ActivityUtils.sendWhatsappText(url)
            return true //bs
        } else if (url.startsWith("viber:")) {
            ActivityUtils.startViber(url)
            return true //bs
        } else if (url.startsWith("tel:")) {
            ActivityUtils.startCallActivity( url)
            return true
        } else if (url.startsWith("sms:")) {
            ActivityUtils.startSmsActivity( url)
            return true
        } else if (url.startsWith("geo:")) {
            ActivityUtils.startMapSearchActivity( url)
            return true
        } else if (url.startsWith("yandexnavi:")) {
            ActivityUtils.startyandexnavi( url)
            return true
        } else if (url.startsWith("intent://")) {
            if (url.startsWith("intent://maps.yandex")) {
                ActivityUtils.startMapYandex( url.replace("intent://", "https://"))
                return true
            }
            //bnk            else if (InAppBrowserUtils.isNspb(url)) {
//bnk                return InAppBrowserUtils.handleNspb(view, url);
//bnk            } else if (url.startsWith("intent://pay.mironline.ru")) {
//bnk                InAppBrowserUtils.paymironlineru( url);
//bnk                return true;
//bnk            }
            return false
        } else {
//            if (isConnected) {
//                // return false to let the WebView handle the URL
//                return false
//            } else {
//                // show the proper "not connected" message
//                view.loadData(offlineMessageHtml, "text/html", "utf-8")
//                // return true if the host application wants to leave the current
//                // WebView and handle the url itself
//                return true
//            }
        }
        return true
    }

    /**
     * Мост к [RequestInterceptor]: `handleUrl == true` → навигация в WebView отменяется.
     */
    fun interceptUrlRequest(request: WebRequest): WebRequestInterceptResult {
        return if (handleUrl(request.url)) {
            WebRequestInterceptResult.Reject
        } else {
            WebRequestInterceptResult.Allow
        }
    }
}