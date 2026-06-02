package net.example.online.webview

/**
 * Platform hook for handling links opened via target="_blank" / window.open.
 */
expect fun configureBlankTargetHandling(nativeWebView: Any)

