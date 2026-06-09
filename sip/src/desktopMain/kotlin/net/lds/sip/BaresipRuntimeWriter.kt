package net.lds.sip

import java.nio.file.Files
import java.nio.file.Path

/** AOR как в `telephony.c` (`cmd_audioCall`). */
internal object BaresipRuntimeWriter {
    fun writeRuntimeDir(params: SipCallParams): Path {
        val dir = Files.createTempDirectory("lds-baresip-")
        dir.toFile().deleteOnExit()

        val config = buildConfig(isWindows())
        val account = buildAccountLine(params)

        Files.writeString(dir.resolve("config"), config)
        Files.writeString(dir.resolve("accounts"), account + System.lineSeparator())
        Files.writeString(dir.resolve("contacts"), "")
        return dir
    }

    private fun buildAccountLine(params: SipCallParams): String {
        val accountLabel = params.account.orEmpty()
        return "\"$accountLabel\" <sip:${params.login}@${params.host}:${params.port};transport=${params.transport}>;" +
            "auth_pass=${params.password};"
    }

    private fun buildConfig(windows: Boolean): String = if (windows) {
        """
            poll_method          select
            sip_transports       udp,tcp,tls

            module account.dll
            module menu.dll
            module g711.dll
            module wasapi.dll
            module srtp.dll
            module stun.dll
            module turn.dll
            module ice.dll
            module uuid.dll

            audio_player wasapi,default
            audio_source wasapi,default
            audio_alert wasapi,default

            call_local_timeout 120
            call_max_calls 1
            rtcp_enable yes
        """.trimIndent() + System.lineSeparator()
    } else {
        """
            poll_method          select
            sip_transports       udp,tcp,tls

            module account.so
            module menu.so
            module stdio.so
            module g711.so
            module alsa.so
            module srtp.so
            module stun.so
            module turn.so
            module ice.so
            module uuid.so

            audio_player alsa,default
            audio_source alsa,default
            audio_alert alsa,default

            call_local_timeout 120
            call_max_calls 1
            rtcp_enable yes
        """.trimIndent() + System.lineSeparator()
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").contains("windows", ignoreCase = true)
}
