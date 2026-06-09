package net.lds.sip

// Events from User-Agent
// NOTE: All UA_EVENT_* are taken from baresip.h file and must be the same!
object SipEvent {

    const val SIP_EVENT_THREAD_STARTED: Int = -3
    const val SIP_EVENT_THREAD_FINISHED: Int = -2
    const val SIP_EVENT_NONE: Int = -1

    //    static final int UA_EVENT_REGISTERING = 0;
    //    static final int UA_EVENT_REGISTER_OK = 1;
    const val UA_EVENT_REGISTER_FAIL: Int = 2

    //    static final int UA_EVENT_UNREGISTERING = 3;
    //    static final int UA_EVENT_FALLBACK_OK = 4;
    //    static final int UA_EVENT_FALLBACK_FAIL = 5;
    //    static final int UA_EVENT_MWI_NOTIFY = 6;
    //    static final int UA_EVENT_SHUTDOWN = 7;
    //    static final int UA_EVENT_EXIT = 8;
    //    static final int UA_EVENT_CALL_INCOMING = 9;
    const val UA_EVENT_CALL_RINGING: Int = 10

    //    static final int UA_EVENT_CALL_PROGRESS = 11;
    const val UA_EVENT_CALL_ESTABLISHED: Int = 12
    const val UA_EVENT_CALL_CLOSED: Int = 13 //    static final int UA_EVENT_CALL_TRANSFER = 14;
    //    static final int UA_EVENT_CALL_TRANSFER_FAILED = 15;
    //    static final int UA_EVENT_CALL_DTMF_START = 16;
    //    static final int UA_EVENT_CALL_DTMF_END = 17;
    //    static final int UA_EVENT_CALL_RTPESTAB = 18;
    //    static final int UA_EVENT_CALL_RTCP = 19;
    //    static final int UA_EVENT_CALL_MENC = 20;
    //    static final int UA_EVENT_VU_TX = 21;
    //    static final int UA_EVENT_VU_RX = 22;
    //    static final int UA_EVENT_AUDIO_ERROR = 23;
    //    static final int UA_EVENT_CALL_LOCAL_SDP = 24;   // param: offer or answer
    //    static final int UA_EVENT_CALL_REMOTE_SDP = 25;  // param: offer or answer
    //    static final int UA_EVENT_MAX = 26;
}
