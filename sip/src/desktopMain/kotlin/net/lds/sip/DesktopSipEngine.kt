package net.lds.sip

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Desktop SIP через внешний `baresip` (MSYS2 / PATH).
 *
 * Эталон поведения — Android [net.lds.online.sip.SipService] +
 * `accesspoint/src/main/cpp/telephony/telephony.c` (`ua_register` → `ua_connect(peer)`).
 * В APK встроен свой baresip; здесь — отдельный exe, версия может отличаться.
 *
 * `LDS_SIP_SIMULATE=1` → [SimulatedSipEngine] (только UI).
 * `BARESIP_EXE` / `BARESIP_HOME` / `C:/msys64/ucrt64/bin/baresip.exe`.
 */
class DesktopSipEngine : SipEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _events = MutableSharedFlow<SipEngineEvent>(extraBufferCapacity = 32)
    override val events: Flow<SipEngineEvent> = _events.asSharedFlow()

    private var process: Process? = null
    private var stdin: BufferedWriter? = null
    private var stdoutJob: kotlinx.coroutines.Job? = null

    private val baresipExe = BaresipLocator.resolve()

    override suspend fun start() {
        _events.emit(SipEngineEvent.InitResult(success = baresipExe != null))
    }

    override fun shutdown() {
        stopProcess()
        scope.cancel()
    }

    override fun placeCall(params: SipCallParams) {
        val exe = baresipExe ?: return
        if (!isPlaceCallParamsValid(params)) {
            if (LOG_ENABLED) {
                println("$TAG SIP_CALL_PARAMS invalid: $params")
            }
            _events.tryEmit(SipEngineEvent.CallClosed(statusCode = 503))
            return
        }
        if (LOG_ENABLED) {
            println(
                "$TAG SIP_CALL_PARAMS host=${params.host} port=${params.port} " +
                    "transport=${params.transport} account=${params.account} login=${params.login} " +
                    "peer=${params.operator}",
            )
        }

        stopProcess()

        val configDir = BaresipRuntimeWriter.writeRuntimeDir(params)
        val binDir = exe.parent
        val dialUri = buildDialUri(params)

        stdoutJob = scope.launch {
            runCatching {
                // Windows MSYS2: stdio.dll нет — stdin-команды не работают; dial через -e (menu).
                val pb = ProcessBuilder(
                    exe.toString(),
                    "-f",
                    configDir.toString(),
                    "-e",
                    "/dial $dialUri",
                )
                    .directory(binDir?.toFile())
                    .redirectErrorStream(true)

                val path = System.getenv("PATH").orEmpty()
                if (binDir != null) {
                    pb.environment()["PATH"] = "${binDir}${java.io.File.pathSeparator}$path"
                }

                val proc = pb.start()
                process = proc
                stdin = BufferedWriter(OutputStreamWriter(proc.outputStream, StandardCharsets.UTF_8))

                BufferedReader(InputStreamReader(proc.inputStream, StandardCharsets.UTF_8)).use { reader ->
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        onBaresipLine(line)
                    }
                }

                val exited = proc.waitFor(2, TimeUnit.SECONDS)
                if (!exited) {
                    proc.destroyForcibly()
                }
            }.onFailure { error ->
                _events.tryEmit(SipEngineEvent.CallClosed(statusCode = 503))
                error.printStackTrace()
            }
        }
    }

    override fun hangup() {
        sendCommand("/hangup")
        scope.launch {
            delay(300)
            stopProcess()
            _events.emit(SipEngineEvent.CallClosed(statusCode = 487))
        }
    }

    override fun stopService() {
        stopProcess()
    }

    private fun sendCommand(command: String) {
        runCatching {
            stdin?.apply {
                write(command)
                newLine()
                flush()
            }
        }
    }

    private fun stopProcess() {
        stdoutJob?.cancel()
        stdoutJob = null

        runCatching { sendCommand("/quit") }
        runCatching { stdin?.close() }
        stdin = null

        process?.let { proc ->
            if (proc.isAlive) {
                proc.destroy()
                if (!proc.waitFor(2, TimeUnit.SECONDS)) {
                    proc.destroyForcibly()
                }
            }
        }
        process = null
    }

    /** Как [net.lds.online.sip.SipService.startAudioCall] — все поля обязательны, account может быть `""`. */
    private fun isPlaceCallParamsValid(params: SipCallParams): Boolean =
        params.host.isNotBlank() &&
            params.port != 0 &&
            params.transport.isNotBlank() &&
            params.account != null &&
            params.login.isNotBlank() &&
            params.password.isNotBlank() &&
            params.operator.isNotBlank()

    private fun onBaresipLine(line: String) {
        if (LOG_ENABLED) {
            println("$TAG baresip> $line")
        }
        parseLine(line)
    }

    /** baresip 4.x: bare extension → «could not find UA»; нужен полный SIP URI. */
    private fun buildDialUri(params: SipCallParams): String =
        "sip:${params.operator}@${params.host}"

    private fun parseLine(line: String) {
        val lower = line.lowercase()

        when {
            isRegistrationFailed(lower) ->
                _events.tryEmit(SipEngineEvent.InitResult(success = false))

            // UA_EVENT_CALL_RINGING → SipService.startRingbackPlayer()
            lower.contains("ringing") ||
                lower.contains("180 ringing") ||
                lower.contains("sip progress") && lower.contains("180") ->
                _events.tryEmit(SipEngineEvent.CallRinging(extractCallId(line)))

            // UA_EVENT_CALL_ESTABLISHED
            lower.contains("call established") ||
                (lower.contains("200 ok") && lower.contains("invite")) ->
                _events.tryEmit(SipEngineEvent.CallEstablished)

            // UA_EVENT_CALL_CLOSED
            lower.contains("call closed") ||
                lower.contains("call terminated") ||
                lower.contains("session closed") ||
                (lower.contains("terminated") && lower.contains("call")) -> {
                val code = extractStatusCode(line) ?: 0
                _events.tryEmit(SipEngineEvent.CallClosed(statusCode = code))
                stopProcess()
            }
        }
    }

    private fun isRegistrationFailed(lower: String): Boolean =
        (lower.contains("register") && lower.contains("fail")) ||
            lower.contains("401 ") ||
            lower.contains("403 forbidden")

    private fun extractCallId(line: String): String? =
        CALL_ID_PATTERN.matcher(line).let { matcher ->
            if (matcher.find()) matcher.group(1) else null
        }

    private fun extractStatusCode(line: String): Int? {
        STATUS_CODE_PATTERN.matcher(line).let { matcher ->
            while (matcher.find()) {
                matcher.group(1)?.toIntOrNull()?.let { return it }
            }
        }
        return null
    }

    companion object {
        private const val TAG = "DesktopSipEngine"
        private const val LOG_ENABLED = true
        private val CALL_ID_PATTERN = Pattern.compile("call-id[:\\s]+([\\w@-]+)", Pattern.CASE_INSENSITIVE)
        private val STATUS_CODE_PATTERN = Pattern.compile("\\b(4\\d{2}|5\\d{2}|6\\d{2})\\b")
    }
}
