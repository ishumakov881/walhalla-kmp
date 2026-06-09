package net.lds.online.sip

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Message
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import net.lds.sip.SipEvent
import net.lds.sip.android.SipForegroundNotifications
import net.lds.sip.android.SipRuntime


class SipService : Service(), Handler.Callback {
    private var mHandler: Handler? = null
    private var mThread: SipThread? = null
    private var mTonePlayer: TonePlayer? = null
    var isCallEstablished: Boolean = false
        private set


    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        val service: SipService
            get() =// Return this instance of SipService so clients can call public methods.
                this@SipService
    }

    // Binder given to clients
    private val mBinder: IBinder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }


    override fun onCreate() {
        super.onCreate()
        val cfg = SipRuntime.require()
        cfg.ensureChannels(applicationContext)
        SipForegroundNotifications.promote(this, cfg.foreground)

        mHandler = Handler(this)

        // Start a service thread
        mThread = SipThread(mHandler!!, filesDir.absolutePath)
        mThread!!.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingbackPlayer()

        try {
            // Wait for a service thread to finish.
            mThread!!.join()
        } catch (e: InterruptedException) {
            // empty
        }

        mHandler = null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.hasExtra(CMD_STOP)) {
            stopRingbackPlayer()
            mThread!!.setStop()
        }

        return START_NOT_STICKY
    }

    fun startAudioCall(
        host: String?, port: Int, transport: String?, account: String?, login: String?,
        password: String?, peer: String?
    ) {

//    old app    wwwa sip.lds.online 5060  30020 dus104ht 33001
//    new app    wwwa sip.lds.online 5060   null 30020 dus104ht 33001

        println("SIP_CALL_PARAMS host: $host")
        println("SIP_CALL_PARAMS port: $port")
        println("SIP_CALL_PARAMS transport: $transport")
        println("SIP_CALL_PARAMS account: $account")
        println("SIP_CALL_PARAMS login: $login")
        println("SIP_CALL_PARAMS password: $password")
        println("SIP_CALL_PARAMS peer: $peer")

        if (null != host && 0 != port && null != transport && null != account && null != login && null != password && null != peer) {
            mThread!!.startAudioCall(host, port, transport, account, login, password, peer)
        }
    }

    fun hangup() {
        stopRingbackPlayer()
        mThread!!.hangup()
    }

    override fun handleMessage(msg: Message): Boolean {
        val event = msg.what

        val intent: Intent = Intent(MSG_EVENT)
        intent.putExtra(KEY_EVENT_ID, event)

        when (event) {
            SipEvent.SIP_EVENT_THREAD_STARTED -> intent.putExtra(
                KEY_INIT_STATUS,
                msg.obj as Boolean
            )

            SipEvent.SIP_EVENT_THREAD_FINISHED -> stopSelf()
            SipEvent.UA_EVENT_REGISTER_FAIL -> intent.putExtra(
                KEY_INIT_STATUS,
                this.isCallEstablished
            )

            SipEvent.UA_EVENT_CALL_RINGING -> {
                startRingbackPlayer()
                intent.putExtra(KEY_CALL_ID, msg.obj as String?)
            }

            SipEvent.UA_EVENT_CALL_ESTABLISHED -> {
                stopRingbackPlayer()
                this.isCallEstablished = true
            }

            SipEvent.UA_EVENT_CALL_CLOSED -> {
                stopRingbackPlayer()
                intent.putExtra(KEY_CALL_SCODE, msg.obj as Int?)
                this.isCallEstablished = false
            }

            else -> {}
        }

        // Send a new message broadcast
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        return true
    }

    private fun startRingbackPlayer() {
        if (mTonePlayer == null) {
            mTonePlayer = TonePlayer(TonePlayer.TONE_DIAL_RINGBACK)
            mTonePlayer!!.start()
        }
    }

    private fun stopRingbackPlayer() {
        if (mTonePlayer != null) {
            mTonePlayer!!.stopTone()
            mTonePlayer = null
        }
    }

    companion object {
        val CMD_STOP: String
            get() = "${SipRuntime.require().broadcast.namespace}.stop"

        /**
         * Broadcasting messages
         */

        val MSG_EVENT: String
            get() = "${SipRuntime.require().broadcast.namespace}.event"

        /**
         * Keys for broadcasting messages
         */
        const val KEY_EVENT_ID: String = "event-id"
        const val KEY_INIT_STATUS: String = "init-status"
        const val KEY_CALL_ID: String = "call-id"
        const val KEY_CALL_SCODE: String = "call-scode"
    }
}
