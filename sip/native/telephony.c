#include <stdlib.h>
#include <string.h>

#define DEBUG_MODULE "telephony"
#define DEBUG_LEVEL 7

#include <re_dbg.h>
#include <re_types.h>
#include <re_fmt.h>
#include <re_conf.h>
#include <re_jbuf.h>
#include <re_list.h>
#include <re_main.h>
#include <re_mqueue.h>
#include <re_msg.h>
#include <re_mod.h>
#include <re_odict.h>
#include <re_rtp.h>
#include <re_sa.h>
#include <re_sdp.h>
#include <re_udp.h>
#include <re_uri.h>
#include <re_sip.h>
#include <re_mem.h>
#include <baresip.h>
#include "telephony_platform.h"

#ifdef __ANDROID__
#include "telephony.h"
#elif defined(__APPLE__)
#include <stdio.h>
#define LOGI(...) fprintf(stderr, "telephony: " __VA_ARGS__); fprintf(stderr, "\n")
#define LOGW(...) fprintf(stderr, "telephony: WARN: " __VA_ARGS__); fprintf(stderr, "\n")
#define LOGE(...) fprintf(stderr, "telephony: ERROR: " __VA_ARGS__); fprintf(stderr, "\n")
#define LOGD(...) fprintf(stderr, "telephony: DEBUG: " __VA_ARGS__); fprintf(stderr, "\n")
#endif

#ifdef __APPLE__
static const char *g_cfgBuf =
        "# Core\n"
        "poll_method kqueue\n"
        "\n"
        "# Call\n"
        "call_local_timeout 120\n"
        "call_max_calls 1\n"
        "\n"
        "# Audio\n"
        "audio_player audiounit,nil\n"
        "audio_source audiounit,nil\n"
        "audio_alert audiounit,nil\n"
        "audio_level no\n"
        "ausrc_format s16\n"
        "auplay_format s16\n"
        "auenc_format s16\n"
        "audec_format s16\n"
        "audio_buffer 20-160\n"
        "\n"
        "# AVT\n"
        "rtp_tos\t\t\t184\n"
        "rtcp_mux\t\tno\n"
        "jitter_buffer_type\tfixed\n"
        "jitter_buffer_delay\t5-10\n"
        "rtp_stats\t\tno\n"
        "\n"
        "# Network\n"
        "dns_fallback\t\t8.8.8.8:53\n"
        "\n"
        "# Modules\n"
        "module\t\t\topus.so\n"
        "module\t\t\tg711.so\n"
        "module\t\t\taudiounit.so\n"
        "module\t\t\tstun.so\n"
        "module\t\t\tturn.so\n"
        "module\t\t\tice.so\n"
        "module_tmp\t\tuuid.so\n"
        "opus_bitrate\t\t28000\n"
        "opus_stereo\t\tno\n"
        "opus_samplerate\t48000\n"
        "opus_ms_channels\t2\n";
#else
static const char *g_cfgBuf =
        "# Core\n"
        "poll_method epoll # poll, select, epoll ..\n"
        "\n"
        "# Call\n"
        "call_local_timeout 120\n"
        "call_max_calls 1\n"
        "\n"
        "# Audio\n"
        "audio_player opensles,nil\n"
        "audio_source opensles,nil\n"
        "audio_alert opensles,nil\n"
        "audio_level no\n"
        "ausrc_format s16 # s16, float, ..\n"
        "auplay_format s16 # s16, float, ..\n"
        "auenc_format s16 # s16, float, ..\n"
        "audec_format s16 # s16, float, ..\n"
        "audio_buffer 20-160 # ms\n"
        "\n"
        "# AVT - Audio/Video Transport\n"
        "rtp_tos\t\t\t184\n"
        "rtcp_mux\t\tno\n"
        "jitter_buffer_type\tfixed\t\t# off, fixed, adaptive\n"
        "jitter_buffer_delay\t5-10\t\t# frames\n"
        "rtp_stats\t\tno\n"
        "\n"
        "# Network\n"
        "dns_fallback\t\t8.8.8.8:53\n"
        "\n"
        "# Modules\n"
        "\n"
        "# Audio codec Modules (in order)\n"
        "module\t\t\topus.so\n"
        "module\t\t\tg711.so\n"
        "\n"
        "# Audio driver Modules\n"
        "module\t\t\topensles.so\n"
        "\n"
        "# Media NAT modules\n"
        "module\t\t\tstun.so\n"
        "module\t\t\tturn.so\n"
        "module\t\t\tice.so\n"
        "\n"
        "# Temporary Modules (loaded then unloaded)\n"
        "\n"
        "module_tmp\t\tuuid.so\n"
        "\n"
        "# Opus codec parameters\n"
        "opus_bitrate\t\t28000 # 6000-510000\n"
        "opus_stereo\t\tno\n"
        "#opus_sprop_stereo\tyes\n"
        "#opus_cbr\t\tno\n"
        "#opus_inbandfec\t\tno\n"
        "#opus_dtx\t\tno\n"
        "#opus_mirror\t\tno\n"
        "#opus_complexity\t10\n"
        "#opus_application\taudio\t# {voip,audio}\n"
        "opus_samplerate\t48000\n"
        "#opus_packet_loss\t10\t# 0-100 percent\n"
        "\n"
        "# Opus Multistream codec parameters\n"
        "opus_ms_channels\t2\t#total channels (2 or 4)\n"
        "#opus_ms_streams\t\t2\t#number of streams\n"
        "#opus_ms_c_streams\t2\t#number of coupled streams\n";
#endif

static struct mqueue *g_messageQueue;
static struct call *g_call;

static void cmd_hangup();

static int cmd_audioCall(AudioCall_t *ac);


#if defined(__ANDROID__)
static void android_log_msg(uint32_t level, const char *msg) {
    const char delims[] = "\n";
    char *cpy = strdup(msg);
    char *line = strtok(cpy, delims);
    while (NULL != line) {
        if (level > 2) {
            LOGE("%s", line);
        } else if (level == 2) {
            LOGW("%s", line);
        } else if (level == 1) {
            LOGI("%s", line);
        } else if (level == 0) {
            LOGD("%s", line);
        }
        line = strtok(NULL, delims);
    }
    free(cpy);
}

static struct log g_androidLog = {
        .le = {NULL, NULL, NULL, NULL},
        .h = &android_log_msg
};
#elif defined(__APPLE__)
static void apple_log_msg(uint32_t level, const char *msg) {
    (void) level;
    LOGI("%s", msg);
}

static struct log g_appleLog = {
        .le = {NULL, NULL, NULL, NULL},
        .h = &apple_log_msg
};
#endif

static void event_listener(struct ua *ua, enum ua_event ev, struct call *call, const char *prm,
                           void *arg) {
    const char *callId;
    uint16_t scode;

    (void) ua;
    (void) call;
    (void) prm;
    (void) arg;

    callId = UA_EVENT_CALL_RINGING == ev ? call_id(g_call) : NULL;

    if (UA_EVENT_CALL_CLOSED == ev) {
        scode = call_scode(g_call);
        LOGI("UA_EVENT_CALL_CLOSED (%d)", scode);
        g_call = NULL;
    } else {
        scode = 0;
        LOGI("UA_EVENT_%s", uag_event_str(ev));
    }

    notifyEvent((int) ev, scode, callId); // Notify the caller module.
}

static void mqueue_handler(int id, void *data, void *arg) {
    (void) arg;

    switch (id) {
        case CMD_AUDIO_CALL:
            cmd_audioCall((AudioCall_t *) data);
            break;
        case CMD_HANGUP:
            cmd_hangup();
            break;
        case CMD_STOP:
            re_cancel();
            break;
        default:
            break;
    }
}


static void cmd_hangup() {
    struct ua *ua = uag_current();
    ua_hangup(ua, g_call, 0, NULL);
    ua_unregister(ua);
}

static int cmd_audioCall(AudioCall_t *ac) {
    char aor[128];
    struct ua *ua;
    int err;

    if (!ac)
        return EINVAL;

    // Hangup a previous call if any.
    //cmd_hangup();

    snprintf(aor, sizeof(aor),
             "\"%s\" <sip:%s@%s:%d;transport=%s>;"
             "auth_pass=%s;"
            //"regint=7200;regq=0.5;answermode=manual;"
            //"audio_codecs=opus/48000/2,PCMU/8000/1,PCMA/8000/1;"
            //"video_codecs=h264;"
            //"medianat=ice;"
            //"stunserver=stun:@stun.lds.ua"
            , ac->account, ac->user, ac->host, ac->port, ac->transport, ac->passw
    );

    ua = NULL;

    err = ua_alloc(&ua, aor);
    if (err) {
        LOGI("ERROR: ua_alloc error! %d", err);
        goto out;
    }

    err = ua_register(ua);
    if (err) {
        LOGI("ERROR: ua_register error! %d", err);
        goto out;
    }

    err = ua_connect(ua, &g_call, NULL, ac->peer, VIDMODE_OFF);
    if (err) {
        LOGI("ERROR: ua_connect error! %d", err);
        goto out;
    }

    out:
    if (err && ua)
        mem_deref(ua);

    free(ac->peer);
    free(ac->passw);
    free(ac->user);
    free(ac->account);
    free(ac->transport);
    free(ac->host);
    free(ac);

    return err;
}

static void tel_done(void) {
    g_call = NULL;

    mem_deref(g_messageQueue);
    g_messageQueue = NULL;

    // Release the User-Agent
    uag_event_unregister(event_listener);
    ua_stop_all(true);
    ua_close();

    conf_close();
    baresip_close();
    mod_close();
    libre_close();

    dbg_close();
#ifdef __ANDROID__
    log_unregister_handler(&g_androidLog);
    resetJniPointers();
#elif defined(__APPLE__)
    log_unregister_handler(&g_appleLog);
    telephony_reset_event_callback();
#endif
}

int telephony_init(const char *path) {
    struct config *config;
    //struct player *player;
    int err;

#ifdef __ANDROID__
    log_register_handler(&g_androidLog);
#elif defined(__APPLE__)
    log_register_handler(&g_appleLog);
#endif
    log_enable_debug(false);

    dbg_init(DBG_DEBUG, DBG_TIME);

    err = libre_init();
    if (err) {
        tel_done();
        LOGI("ERROR: libre_init error! %d", err);
        return err;
    }

    conf_path_set(path);

    config = conf_config();
    config->call.local_timeout = 2 * 60; /**< Incoming call timeout [sec] 0=off    */
    config->call.max_calls = 1;          /**< Maximum number of calls, 0=unlimited */
    config->avt.rtp_timeout = 2 * 60; /**< RTP Timeout in seconds (0=off) */

    err = conf_configure_buf((const uint8_t *) g_cfgBuf, strlen(g_cfgBuf));
    if (err) {
        tel_done();
        LOGI("ERROR: conf_configure error! %d", err);
        return err;
    }

    //err = poll_method_set(METHOD_EPOLL);
    //if (err) {
    //    tel_done();
    //    LOGI("ERROR: poll_method_set error! %d", err);
    //    return err;
    //}

    err = baresip_init(config);
    if (err) {
        tel_done();
        LOGI("ERROR: baresip_init error! %d", err);
        return err;
    }

    mod_init();

    err = conf_modules();
    if (err) {
        LOGI("ERROR: conf_modules error! %d", err);
        tel_done();
        return err;
    }

//    err = play_init(&player);
//    if (err) {
//        tel_done();
//        LOGI("ERROR: play_init error! %d", err);
//        return err;
//    }
//
//    play_set_path(player, path);
//
//    // It's assumed that audio files are in <path> directory
//    strncpy(config->audio.audio_path, path, sizeof(config->audio.audio_path) - 1);
//    config->audio.audio_path[sizeof(config->audio.audio_path) - 1] = '\0';

    // Initialize the User-Agent
    err = ua_init("LDS Online", true, true, false);
    if (err) {
        LOGI("ERROR: ua_init error! %d", err);
        tel_done();
        return err;
    }

    err = uag_event_register(event_listener, NULL);
    if (err) {
        LOGI("ERROR: uag_event_register error! %d", err);
        tel_done();
        return err;
    }

    err = mqueue_alloc(&g_messageQueue, mqueue_handler, NULL);
    if (err) {
        LOGI("ERROR: mqueue_alloc error! %d", err);
        tel_done();
        return err;
    }

    return 0;
}

int telephony_mainLoop(void) {
    int err = re_main(NULL);
    tel_done();
    return err;
}

void telephony_cmd(int cmd, void *data) {
    mqueue_push(g_messageQueue, cmd, data);
}
