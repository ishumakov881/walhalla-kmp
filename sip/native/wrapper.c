#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include "telephony.h"

const char *TAG = "MOB_CAB";

static JavaVM *g_javaVM;
static jobject g_jSipThreadObj; // SipThread object pointer.
static jmethodID g_jMessageFromJniMethodId; // SipThread.messageFromJni() method pointer


static JNIEnv *getJniEnv(boolean_t *pFlagAttached) {
    JNIEnv *jniEnv;
    jint err;

    if (pFlagAttached)
        *pFlagAttached = FALSE;

    if (!g_javaVM)
        return NULL;

    err = (*g_javaVM)->GetEnv(g_javaVM, (void **) &jniEnv, JNI_VERSION_1_6);
    if (JNI_OK != err) {
        err = (*g_javaVM)->AttachCurrentThread(g_javaVM, &jniEnv, NULL);
        if (JNI_OK == err) {
            if (pFlagAttached)
                *pFlagAttached = TRUE;
        } else {
            LOGE("Failed to AttachCurrentThread, ErrorCode = %d", err);
            return NULL;
        }
    }

    return jniEnv;
}

static void detachCurrentThread(boolean_t flagAttached) {
    if (flagAttached && g_javaVM) {
        (*g_javaVM)->DetachCurrentThread(g_javaVM);
    }
}

void resetJniPointers(void) {
    if (g_jSipThreadObj) {
        boolean_t flagAttached;
        JNIEnv *jniEnv = getJniEnv(&flagAttached);
        if (jniEnv) {
            (*jniEnv)->DeleteGlobalRef(jniEnv, g_jSipThreadObj);
            detachCurrentThread(flagAttached);
        }
        g_jSipThreadObj = NULL;
    }

    g_jMessageFromJniMethodId = NULL;
}

void notifyEvent_jni(int event, int scode, const char *callId) {
    boolean_t flagAttached;
    JNIEnv *jniEnv = getJniEnv(&flagAttached);
    jobject jObj = NULL;

    if (!jniEnv)
        return;

    if (NULL != callId) {
        jObj = (*jniEnv)->NewStringUTF(jniEnv, callId);

    } else if (0 != scode) {
        jclass integerClass = (*jniEnv)->FindClass(jniEnv, "java/lang/Integer");
        if (NULL != integerClass) {
            // Locate a constructor of the Integer class.
            jmethodID jMethodId = (*jniEnv)->GetMethodID(jniEnv, integerClass, "<init>",
                                                         "(I)V");
            if (NULL != jMethodId) {
                jObj = (*jniEnv)->NewObject(jniEnv, integerClass, jMethodId, scode);
            }
        }
    }

    // Pass the event to Java code - call SipThread.messageFromJni() method.
    (*jniEnv)->CallVoidMethod(jniEnv, g_jSipThreadObj, g_jMessageFromJniMethodId, event,
                              jObj);

    if (NULL != jObj) {
        (*jniEnv)->DeleteLocalRef(jniEnv, jObj);
    }

    detachCurrentThread(flagAttached);
}

static char *my_strdup(const char *src) {
    char *dst = (char *) malloc(strlen(src) + 1);
    return strcpy(dst, src);
}


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;

    (void) reserved;
    g_javaVM = vm;
    if (!vm || JNI_OK != (*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6)) {
        return JNI_ERR; // JNI version isn't supported.
    }

    return JNI_VERSION_1_6;
}

JNIEXPORT jboolean JNICALL
Java_net_lds_online_sip_SipThread_telephony_1init(JNIEnv *env, jobject thiz,
                                                         jstring jstrConfigPath) {
    jclass jCallerClz;
    const char *cfgPath;
    int err;

    resetJniPointers();

    // Get a callback method pointer
    jCallerClz = (*env)->GetObjectClass(env, thiz);
    g_jSipThreadObj = (*env)->NewGlobalRef(env, thiz);
    g_jMessageFromJniMethodId = (*env)->GetMethodID(env, jCallerClz, "messageFromJni",
                                                    "(ILjava/lang/Object;)V");

    cfgPath = (*env)->GetStringUTFChars(env, jstrConfigPath, 0);
    err = telephony_init(cfgPath);
    (*env)->ReleaseStringUTFChars(env, jstrConfigPath, cfgPath);

    return err ? JNI_FALSE : JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_net_lds_online_sip_SipThread_telephony_1mainLoop(JNIEnv *env, jobject thiz) {
    (void) env;
    (void) thiz;
    telephony_mainLoop();
}

JNIEXPORT void JNICALL
Java_net_lds_online_sip_SipThread_telephony_1startAudioCall(JNIEnv *env, jobject thiz,
                                                                   jstring jstrHost, int port,
                                                                   jstring jstrTransport,
                                                                   jstring jstrAccount,
                                                                   jstring jstrUser,
                                                                   jstring jstrPassw,
                                                                   jstring jstrPeer) {
    const char *host, *transport, *account, *user, *passw, *peer;
    AudioCall_t *ac;

    (void) thiz;

    host = (*env)->GetStringUTFChars(env, jstrHost, 0);
    transport = (*env)->GetStringUTFChars(env, jstrTransport, 0);
    account = (*env)->GetStringUTFChars(env, jstrAccount, 0);
    user = (*env)->GetStringUTFChars(env, jstrUser, 0);
    passw = (*env)->GetStringUTFChars(env, jstrPassw, 0);
    peer = (*env)->GetStringUTFChars(env, jstrPeer, 0);

    ac = (AudioCall_t *) malloc(sizeof(AudioCall_t));
    ac->host = my_strdup(host);
    ac->port = port;
    ac->transport = my_strdup(transport);
    ac->account = my_strdup(account);
    ac->user = my_strdup(user);
    ac->passw = my_strdup(passw);
    ac->peer = my_strdup(peer);

    telephony_cmd(CMD_AUDIO_CALL, ac);

    (*env)->ReleaseStringUTFChars(env, jstrPeer, peer);
    (*env)->ReleaseStringUTFChars(env, jstrPassw, passw);
    (*env)->ReleaseStringUTFChars(env, jstrUser, user);
    (*env)->ReleaseStringUTFChars(env, jstrAccount, account);
    (*env)->ReleaseStringUTFChars(env, jstrTransport, transport);
    (*env)->ReleaseStringUTFChars(env, jstrHost, host);
}

JNIEXPORT void JNICALL
Java_net_lds_online_sip_SipThread_telephony_1hangup(JNIEnv *env, jobject thiz) {
    (void) env;
    (void) thiz;
    telephony_cmd(CMD_HANGUP, NULL);
}

JNIEXPORT void JNICALL
Java_net_lds_online_sip_SipThread_telephony_1stop(JNIEnv *env, jobject thiz) {
    (void) env;
    (void) thiz;
    telephony_cmd(CMD_STOP, NULL);
}
