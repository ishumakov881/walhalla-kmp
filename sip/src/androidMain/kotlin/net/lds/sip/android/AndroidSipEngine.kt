package net.lds.sip.android

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.lds.online.sip.ServiceUtils
import net.lds.online.sip.SipService
import net.lds.sip.SipCallParams
import net.lds.sip.SipEngine
import net.lds.sip.SipEngineEvent
import net.lds.sip.SipEvent

class AndroidSipEngine(
    private val context: Context,
) : SipEngine {

    private val _events = MutableSharedFlow<SipEngineEvent>(extraBufferCapacity = 32)
    override val events: SharedFlow<SipEngineEvent> = _events.asSharedFlow()

    private var sipService: SipService? = null
    private var isBound = false
    private var receiverRegistered = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SipService.LocalBinder
            sipService = binder.service
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sipService = null
            isBound = false
        }
    }

    private val eventReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action != SipService.MSG_EVENT) return
            val eventId = intent.getIntExtra(SipService.KEY_EVENT_ID, SipEvent.SIP_EVENT_NONE)
            when (eventId) {
                SipEvent.SIP_EVENT_THREAD_STARTED,
                SipEvent.UA_EVENT_REGISTER_FAIL,
                -> {
                    val success = intent.getBooleanExtra(SipService.KEY_INIT_STATUS, false)
                    _events.tryEmit(SipEngineEvent.InitResult(success))
                }
                SipEvent.UA_EVENT_CALL_RINGING -> {
                    _events.tryEmit(
                        SipEngineEvent.CallRinging(
                            intent.getStringExtra(SipService.KEY_CALL_ID),
                        ),
                    )
                }
                SipEvent.UA_EVENT_CALL_ESTABLISHED -> {
                    _events.tryEmit(SipEngineEvent.CallEstablished)
                }
                SipEvent.UA_EVENT_CALL_CLOSED -> {
                    _events.tryEmit(
                        SipEngineEvent.CallClosed(
                            intent.getIntExtra(SipService.KEY_CALL_SCODE, 0),
                        ),
                    )
                }
            }
        }
    }

    override suspend fun start() {
        ServiceUtils.startSipService(context.applicationContext)
        context.bindService(
            Intent(context, SipService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )
        if (!receiverRegistered) {
            LocalBroadcastManager.getInstance(context).registerReceiver(
                eventReceiver,
                IntentFilter(SipService.MSG_EVENT),
            )
            receiverRegistered = true
        }
    }

    override fun shutdown() {
        if (receiverRegistered) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(eventReceiver)
            receiverRegistered = false
        }
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
        sipService = null
    }

    override fun placeCall(params: SipCallParams) {
        sipService?.startAudioCall(
            params.host,
            params.port,
            params.transport,
            params.account,
            params.login,
            params.password,
            params.operator,
        )
    }

    override fun hangup() {
        sipService?.hangup()
    }

    override fun stopService() {
        val intent = Intent(context, SipService::class.java).apply {
            putExtra(SipService.CMD_STOP, 0)
        }
        context.startService(intent)
    }
}
