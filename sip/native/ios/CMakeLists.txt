cmake_minimum_required(VERSION 3.22)

project(telephony_ios C)

set(NATIVE_ROOT "${CMAKE_CURRENT_LIST_DIR}/..")

include(${NATIVE_ROOT}/baresip_sources_ios.cmake)
include(${NATIVE_ROOT}/opus_sources.cmake)

list(TRANSFORM BARESIP_SOURCES PREPEND "${NATIVE_ROOT}/")
list(TRANSFORM OPUS_SOURCES PREPEND "${NATIVE_ROOT}/")
list(TRANSFORM CELT_SOURCES PREPEND "${NATIVE_ROOT}/")
list(TRANSFORM SILK_SOURCES PREPEND "${NATIVE_ROOT}/")
list(TRANSFORM BARESIP_HEADERS_DIRS PREPEND "${NATIVE_ROOT}/")
list(TRANSFORM OPUS_HEADERS_DIRS PREPEND "${NATIVE_ROOT}/")

add_library(
        telephony
        STATIC
        ${NATIVE_ROOT}/telephony.c
        ${NATIVE_ROOT}/telephony_notify.c
        ${NATIVE_ROOT}/telephony_kn.c
        ${BARESIP_SOURCES}
        ${OPUS_SOURCES} ${CELT_SOURCES} ${SILK_SOURCES}
)

target_include_directories(
        telephony
        PUBLIC
        ${NATIVE_ROOT}
        ${BARESIP_HEADERS_DIRS}
        ${OPUS_HEADERS_DIRS}
)

target_compile_definitions(
        telephony
        PRIVATE
        STATIC
        SHARE_PATH="/usr/local/share/telephony"
        HAVE_PTHREAD
        HAVE_PTHREAD_RWLOCK
        HAVE_LIBPTHREAD
        HAVE_UNISTD_H
        HAVE_INET6
        HAVE_SELECT
        HAVE_KQUEUE
        HAVE_STRERROR_R
        HAVE_INTTYPES_H
)

find_library(CORE_AUDIO_FRAMEWORK CoreAudio REQUIRED)
find_library(AUDIO_TOOLBOX_FRAMEWORK AudioToolbox REQUIRED)
find_library(AV_FOUNDATION_FRAMEWORK AVFoundation REQUIRED)
find_library(CORE_FOUNDATION_FRAMEWORK CoreFoundation REQUIRED)
find_library(SECURITY_FRAMEWORK Security REQUIRED)
find_library(SYSTEM_CONFIGURATION_FRAMEWORK SystemConfiguration REQUIRED)

target_link_libraries(
        telephony
        PRIVATE
        ${CORE_AUDIO_FRAMEWORK}
        ${AUDIO_TOOLBOX_FRAMEWORK}
        ${AV_FOUNDATION_FRAMEWORK}
        ${CORE_FOUNDATION_FRAMEWORK}
        ${SECURITY_FRAMEWORK}
        ${SYSTEM_CONFIGURATION_FRAMEWORK}
)

set_target_properties(telephony PROPERTIES POSITION_INDEPENDENT_CODE ON)
