#ifndef BARESIPTEST3_TELEPHONY_H_
#define BARESIPTEST3_TELEPHONY_H_

#include <android/log.h>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__))
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__))

typedef int boolean_t;
enum {
    FALSE, TRUE
};

extern const char *TAG;

#define CMD_AUDIO_CALL  101
#define CMD_HANGUP      102
#define CMD_STOP        103

typedef struct AudioCall {
    char *host;
    int port;
    char *transport;
    char *account;
    char *user;
    char *passw;
    char *peer;
} AudioCall_t;


void resetJniPointers(void);

void notifyEvent(int event, int scode, const char *callId);


int telephony_init(const char *path);

int telephony_mainLoop(void);

void telephony_cmd(int cmd, void *data);


#endif //BARESIPTEST3_TELEPHONY_H_
