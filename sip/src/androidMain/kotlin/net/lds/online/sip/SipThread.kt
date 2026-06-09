package net.lds.online.sip

import android.os.Handler
import android.util.Log
import net.lds.sip.SipEvent

class SipThread internal constructor(
    private val mServiceHandler: Handler,
    private val mFilesDir: String?
) : Thread() {
    private external fun telephony_init(configPath: String?): Boolean

    private external fun telephony_mainLoop()

    private external fun telephony_startAudioCall(
        host: String?, port: Int, transport: String?,
        account: String?, login: String?,
        password: String?, peer: String?
    )

    private external fun telephony_hangup()

    private external fun telephony_stop()


    // NOTE: This method is called from a native code.
    private fun messageFromJni(what: Int, obj: Any?) {
        mServiceHandler.sendMessage(mServiceHandler.obtainMessage(what, obj))
    }


    override fun run() {
        Log.d(TAG, "SipThread started.")

        val res = telephony_init(mFilesDir)

        // Notify SipService that SipThread has started.
        mServiceHandler.sendMessage(
            mServiceHandler.obtainMessage(
                SipEvent.SIP_EVENT_THREAD_STARTED, res
            )
        )

        if (!res) {
            Log.d(TAG, "telephony init error.")
        } else {
            Log.d(TAG, "Starting telephony main loop...")
            try {
                telephony_mainLoop()
            }catch (e: Exception){
                e.printStackTrace()
            }
        }

        mServiceHandler.sendEmptyMessage(SipEvent.SIP_EVENT_THREAD_FINISHED)

        Log.d(TAG, "SipThread finished.")
    }

    fun startAudioCall(
        host: String?, port: Int, transport: String?, account: String?, login: String?,
        password: String?, peer: String?
    ) {
        Log.d(TAG, "telephony_startAudioCall()")
        telephony_startAudioCall(host, port, transport, account, login, password, peer)
    }

    fun hangup() {
        Log.d(TAG, "telephony_hangup()")
        telephony_hangup()
    }

    fun setStop() {
        Log.d(TAG, "telephony_stop()")
        telephony_stop()
    }

    companion object {
        private const val TAG = "SipThread"

        init {
            System.loadLibrary("telephony")
        }
    }
}
