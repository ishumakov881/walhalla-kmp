package net.example.online.utils

expect object ActivityUtils {
    fun openBrowser(data: String)
    fun startEmailActivity(email: String, subject: String?, text: String?)
    fun startCallActivity(url: String)
    fun startSmsActivity(url: String)
    fun startMapSearchActivity(url: String)
    fun startMapYandex(url: String)
    fun startyandexnavi(url: String)
    fun starDefault(url: String)
    fun startShareActivity(subject: String?, text: String)
    fun starttg(url: String)

    fun startViber(url: String)
    fun sendWhatsappText(url: String)
    fun sendWhatsappPhone(url: String)
    fun mailTo(url: String)


}
