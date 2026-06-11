#include <stdlib.h>
#include <string.h>

#include "telephony_kn.h"

static telephony_kn_event_fn g_kn_handler;

static void kn_event_bridge(int event, int scode, const char *callId)
{
    if (g_kn_handler) {
        g_kn_handler(event, scode, callId);
    }
}

void telephony_kn_set_event_handler(telephony_kn_event_fn handler)
{
    g_kn_handler = handler;
    telephony_set_event_callback(handler ? kn_event_bridge : NULL);
}

int telephony_kn_init(const char *config_path)
{
    return telephony_init(config_path);
}

void telephony_kn_main_loop(void)
{
    telephony_mainLoop();
}

static char *kn_strdup(const char *src)
{
    size_t len;
    char *dst;

    if (!src) {
        return NULL;
    }

    len = strlen(src);
    dst = (char *) malloc(len + 1);
    if (!dst) {
        return NULL;
    }
    memcpy(dst, src, len + 1);
    return dst;
}

void telephony_kn_start_audio_call(
        const char *host,
        int port,
        const char *transport,
        const char *account,
        const char *user,
        const char *password,
        const char *peer)
{
    AudioCall_t *ac = (AudioCall_t *) malloc(sizeof(AudioCall_t));
    if (!ac) {
        return;
    }

    ac->host = kn_strdup(host);
    ac->port = port;
    ac->transport = kn_strdup(transport);
    ac->account = kn_strdup(account);
    ac->user = kn_strdup(user);
    ac->passw = kn_strdup(password);
    ac->peer = kn_strdup(peer);

    telephony_cmd(CMD_AUDIO_CALL, ac);
}

void telephony_kn_hangup(void)
{
    telephony_cmd(CMD_HANGUP, NULL);
}

void telephony_kn_stop(void)
{
    telephony_cmd(CMD_STOP, NULL);
}
