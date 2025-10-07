#include <jni.h>
#include <vector>
#include <android/log.h>

// If OpenCV is available, include; otherwise compile without it
#if __has_include(<opencv2/imgproc.hpp>)
#  include <opencv2/imgproc.hpp>
#  include <opencv2/core.hpp>
#  define EDGE_HAVE_OPENCV 1
#else
#  define EDGE_HAVE_OPENCV 0
#endif

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "edge", __VA_ARGS__)

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_edgedemo_nativebridge_NativeBridge_processEdgesRgba(
        JNIEnv* env,
        jobject /*this*/,
        jbyteArray rgbaIn,
        jint width,
        jint height,
        jboolean useCanny) {
    const jint w = width;
    const jint h = height;
    const jsize len = env->GetArrayLength(rgbaIn);
    if (len != w * h * 4) {
        LOGE("Invalid input buffer length: %d (expected %d)", len, w*h*4);
        return env->NewByteArray(0);
    }

    std::vector<unsigned char> inBuf(len);
    env->GetByteArrayRegion(rgbaIn, 0, len, reinterpret_cast<jbyte*>(inBuf.data()));
    std::vector<unsigned char> outBuf(len);

#if EDGE_HAVE_OPENCV
    try {
        cv::Mat rgba(h, w, CV_8UC4, inBuf.data());
        if (useCanny) {
            cv::Mat gray, edges;
            cv::cvtColor(rgba, gray, cv::COLOR_RGBA2GRAY);
            cv::Canny(gray, edges, 100, 200);
            cv::Mat edgesRgba;
            cv::cvtColor(edges, edgesRgba, cv::COLOR_GRAY2RGBA);
            memcpy(outBuf.data(), edgesRgba.data, len);
        } else {
            cv::Mat gray;
            cv::cvtColor(rgba, gray, cv::COLOR_RGBA2GRAY);
            cv::Mat grayRgba;
            cv::cvtColor(gray, grayRgba, cv::COLOR_GRAY2RGBA);
            memcpy(outBuf.data(), grayRgba.data, len);
        }
    } catch (...) {
        // Fallback to passthrough on error
        memcpy(outBuf.data(), inBuf.data(), len);
    }
#else
    // No OpenCV: simple passthrough
    memcpy(outBuf.data(), inBuf.data(), len);
#endif

    jbyteArray result = env->NewByteArray(len);
    env->SetByteArrayRegion(result, 0, len, reinterpret_cast<const jbyte*>(outBuf.data()));
    return result;
}


