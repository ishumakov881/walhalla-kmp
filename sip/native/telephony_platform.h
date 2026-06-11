#ifndef TELEPHONY_PLATFORM_H_
#define TELEPHONY_PLATFORM_H_

#include <stdint.h>

typedef int boolean_t;
enum {
    FALSE, TRUE
};

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

typedef void (*telephony_event_cb)(int event, int scode, const char *callId);

void telephony_set_event_callback(telephony_event_cb cb);
void telephony_reset_event_callback(void);

void notifyEvent(int event, int scode, const char *callId);

int telephony_init(const char *path);
int telephony_mainLoop(void);
void telephony_cmd(int cmd, void *data);

#endif
