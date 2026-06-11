#ifndef BARESIPTEST3_TELEPHONY_H_
#define BARESIPTEST3_TELEPHONY_H_

#include "telephony_platform.h"

#include <android/log.h>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__))
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__))

extern const char *TAG;

void resetJniPointers(void);

#endif //BARESIPTEST3_TELEPHONY_H_
