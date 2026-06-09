package net.lds.sip

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Fake SIP flow for desktop UI testing when [DesktopSipEngine] has no baresip binary.
 * Enable with env `LDS_SIP_SIMULATE=1`.
 */
class SimulatedSipEngine : SipEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _events = MutableSharedFlow<SipEngineEvent>(extraBufferCapacity = 32)
    override val events: Flow<SipEngineEvent> = _events.asSharedFlow()

    private var callJob: kotlinx.coroutines.Job? = null

    override suspend fun start() {
        _events.emit(SipEngineEvent.InitResult(success = true))
    }

    override fun shutdown() {
        callJob?.cancel()
        scope.cancel()
    }

    @OptIn(ExperimentalTime::class)
    override fun placeCall(params: SipCallParams) {
        callJob?.cancel()
        callJob = scope.launch {
            delay(300)
            _events.emit(SipEngineEvent.CallRinging(callId = "sim-${Clock.System.now()}"))
            delay(1500)
            _events.emit(SipEngineEvent.CallEstablished)
        }
    }

    override fun hangup() {
        callJob?.cancel()
        callJob = scope.launch {
            _events.emit(SipEngineEvent.CallClosed(statusCode = 487))
        }
    }

    override fun stopService() {
        hangup()
    }
}
