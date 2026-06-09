package net.lds.sip

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class StubSipEngine : SipEngine {
    override val events: Flow<SipEngineEvent> = emptyFlow()
    override suspend fun start() = Unit
    override fun shutdown() = Unit
    override fun placeCall(params: SipCallParams) = Unit
    override fun hangup() = Unit
    override fun stopService() = Unit
}
