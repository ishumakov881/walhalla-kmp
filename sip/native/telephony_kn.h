#ifndef TELEPHONY_KN_H_
#define TELEPHONY_KN_H_

#include "telephony_platform.h"

typedef void (*telephony_kn_event_fn)(int event, int scode, const char *callId);

void telephony_kn_set_event_handler(telephony_kn_event_fn handler);

int telephony_kn_init(const char *config_path);

void telephony_kn_main_loop(void);

void telephony_kn_start_audio_call(
        const char *host,
        int port,
        const char *transport,
        const char *account,
        const char *user,
        const char *password,
        const char *peer);

void telephony_kn_hangup(void);

void telephony_kn_stop(void);

#endif
