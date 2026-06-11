#include "telephony_platform.h"

#ifdef __ANDROID__
void notifyEvent_jni(int event, int scode, const char *callId);
#endif

static telephony_event_cb g_event_callback;

void telephony_set_event_callback(telephony_event_cb cb)
{
    g_event_callback = cb;
}

void telephony_reset_event_callback(void)
{
    g_event_callback = NULL;
}

void notifyEvent(int event, int scode, const char *callId)
{
#ifdef __ANDROID__
    notifyEvent_jni(event, scode, callId);
#endif
    if (g_event_callback) {
        g_event_callback(event, scode, callId);
    }
}
