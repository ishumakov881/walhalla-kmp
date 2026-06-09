package net.lds.sip

import kotlinx.coroutines.flow.Flow

data class SipCallParams(
    val host: String,
    val port: Int,
    val transport: String,
    val account: String?,
    val login: String,
    val password: String,
    val operator: String,
)

sealed interface SipEngineEvent {
    data class InitResult(val success: Boolean) : SipEngineEvent
    data class CallRinging(val callId: String?) : SipEngineEvent
    data object CallEstablished : SipEngineEvent
    data class CallClosed(val statusCode: Int) : SipEngineEvent
}

interface SipEngine {
    val events: Flow<SipEngineEvent>

    suspend fun start()
    fun shutdown()
    fun placeCall(params: SipCallParams)
    fun hangup()
    fun stopService()
}
