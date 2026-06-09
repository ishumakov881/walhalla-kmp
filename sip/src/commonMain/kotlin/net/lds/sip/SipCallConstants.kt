package net.lds.sip

public object SipCallConstants {

    // String Keys for Intent extras and Bundle states
    const val KEY_CABINET_TOKEN = "cabinet.token"
    const val KEY_CABINET_ACCOUNT = "cabinet.account"
    const val KEY_ACCESS_POINT_NAME = "ap.name"
    const val KEY_ACCESS_POINT_MAC = "ap.mac"
    const val KEY_STATUS = "status"
    const val KEY_CHRONOMETER_VISIBILITY = "chronometer"
    const val KEY_CHRONOMETER_BASE = "chrono.base"
    const val KEY_WAS_CALL_ESTABLISHED = "call.estab"

    // Integer constants for CommunicationTask and call end reasons
    const val TASK_CALL_START = 0
    const val TASK_CALL_CANCEL = 1

    const val CALL_END_HANGUP = 0
    const val CALL_END_HANGUP_BACK = 1
    const val CALL_END_BYE = 2
    const val CALL_END_BUSY = 3
    const val CALL_END_ERROR = 4

    // Other constants
    const val RSSI_LOW_VALUE = -67
}