package net.lds.sip

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCString
import kotlinx.cinterop.toKString
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import kotlin.concurrent.thread
import net.lds.sip.native.telephony_kn_hangup
import net.lds.sip.native.telephony_kn_init
import net.lds.sip.native.telephony_kn_main_loop
import net.lds.sip.native.telephony_kn_set_event_handler
import net.lds.sip.native.telephony_kn_start_audio_call
import net.lds.sip.native.telephony_kn_stop

/**
 * iOS SIP через встроенный baresip (AudioUnit + Opus), аналог Android [net.lds.online.sip.SipThread].
 *
 * В Info.plist приложения укажите `NSMicrophoneUsageDescription`.
 * Для фоновых звонков добавьте `UIBackgroundModes` → `audio` или `voip`.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSipEngine : SipEngine {

    private val _events = MutableSharedFlow<SipEngineEvent>(extraBufferCapacity = 32)
    override val events: SharedFlow<SipEngineEvent> = _events.asSharedFlow()

    private var loopThread: Thread? = null
    private var started = false

    private val nativeEventHandler = staticCFunction { event: Int, scode: Int, callId: CPointer<ByteVar>? ->
        val engine = activeEngine ?: return@staticCFunction
        engine.dispatchNativeEvent(event, scode, callId?.toKString())
    }

    override suspend fun start() {
        if (started) {
            return
        }
        started = true
        activeEngine = this

        val configPath = ensureConfigDirectory()
        telephony_kn_set_event_handler(nativeEventHandler)
        val initOk = memScoped {
            telephony_kn_init(configPath.toCString().ptr) == 0
        }
        _events.emit(SipEngineEvent.InitResult(success = initOk))
        if (!initOk) {
            started = false
            activeEngine = null
            return
        }

        loopThread = thread(name = "IosSipTelephony", isDaemon = true) {
            telephony_kn_main_loop()
            activeEngine = null
            started = false
        }
    }

    override fun shutdown() {
        if (!started) {
            return
        }
        telephony_kn_stop()
        loopThread?.join(30_000)
        loopThread = null
        started = false
        if (activeEngine === this) {
            activeEngine = null
        }
        telephony_kn_set_event_handler(null)
    }

    override fun placeCall(params: SipCallParams) {
        if (!started) {
            return
        }
        memScoped {
            telephony_kn_start_audio_call(
                params.host.toCString().ptr,
                params.port,
                params.transport.toCString().ptr,
                params.account?.toCString()?.ptr,
                params.login.toCString().ptr,
                params.password.toCString().ptr,
                params.operator.toCString().ptr,
            )
        }
    }

    override fun hangup() {
        if (!started) {
            return
        }
        telephony_kn_hangup()
    }

    override fun stopService() {
        shutdown()
    }

    private fun dispatchNativeEvent(event: Int, statusCode: Int, callId: String?) {
        when (event) {
            SipEvent.UA_EVENT_REGISTER_FAIL -> {
                _events.tryEmit(SipEngineEvent.InitResult(success = false))
            }
            SipEvent.UA_EVENT_CALL_RINGING -> {
                _events.tryEmit(SipEngineEvent.CallRinging(callId = callId))
            }
            SipEvent.UA_EVENT_CALL_ESTABLISHED -> {
                _events.tryEmit(SipEngineEvent.CallEstablished)
            }
            SipEvent.UA_EVENT_CALL_CLOSED -> {
                _events.tryEmit(SipEngineEvent.CallClosed(statusCode = statusCode))
            }
        }
    }

    private fun ensureConfigDirectory(): String {
        val base = NSTemporaryDirectory() + "sip-baresip/"
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = base,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        return base
    }

    companion object {
        private var activeEngine: IosSipEngine? = null
    }
}
