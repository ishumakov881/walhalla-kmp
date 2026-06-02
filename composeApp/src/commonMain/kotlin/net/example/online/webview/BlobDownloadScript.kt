package net.example.online.webview

/**
 * Скачивание `blob:` URL через JS → [SaveBlobJsHandler] (как ConfigureWebView в appWebView).
 */
object BlobDownloadScript {

    fun build(blobUrl: String, mimeType: String, fileName: String): String {
        val safeUrl = blobUrl.replace("\\", "\\\\").replace("'", "\\'")
        val safeMime = mimeType.replace("\\", "\\\\").replace("'", "\\'")
        val safeName = fileName.replace("\\", "\\\\").replace("'", "\\'")
        return """
            (function() {
                var blobUrl = '$safeUrl';
                alert(blobUrl);
                var xhr = new XMLHttpRequest();
                xhr.open('GET', blobUrl, true);
                xhr.responseType = 'blob';
                xhr.onload = function() {
                    if (this.status !== 200) return;
                    var reader = new FileReader();
                    reader.onloadend = function() {
                        //if (!window.Android || typeof window.Android.callNative !== 'function') return;
                        Android.callNative(
                            'saveBlob',
                            JSON.stringify({
                                base64Data: reader.result,
                                mimeType: '$safeMime',
                                fileName: '$safeName'
                            }),
                            function() {}
                        );
                    };
                    reader.readAsDataURL(this.response);
                };
                xhr.send();
            })();
        """.trimIndent()
    }
}
