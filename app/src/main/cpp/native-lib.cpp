#include <jni.h>
#include <string>
#include<opencv2/opencv.hpp>
#include<opencv2/core.hpp>
#include<opencv2/dnn.hpp>
#include<opencv2/imgcodecs.hpp>
#include<opencv2/imgproc.hpp>
using namespace std;
using namespace cv;

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_opencv_1app_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
