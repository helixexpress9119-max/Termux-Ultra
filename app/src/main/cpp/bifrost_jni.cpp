#include <jni.h>
#include <android/log.h>
#include <string>
#include <memory>

#define LOG_TAG "BifrostJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Forward declarations for Rust functions
extern "C" {
    char* bifrost_hello(const char* input);
    bool bifrost_init();
    char* bifrost_execute_task(const char* task_json);
    char* bifrost_run_python(const char* code);
    void bifrost_free_string(char* ptr);
}

// JNI wrapper functions
extern "C" JNIEXPORT jstring JNICALL
Java_com_example_termuxultra_MainActivity_bifrostHello(JNIEnv *env, jobject /* this */, jstring input) {
    const char* input_str = env->GetStringUTFChars(input, nullptr);
    char* result = bifrost_hello(input_str);
    env->ReleaseStringUTFChars(input, input_str);
    
    if (result) {
        jstring jresult = env->NewStringUTF(result);
        bifrost_free_string(result);
        return jresult;
    }
    return env->NewStringUTF("Error in bifrost_hello");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_termuxultra_MainActivity_bifrostRunPython(JNIEnv *env, jobject /* this */, jstring code) {
    const char* code_str = env->GetStringUTFChars(code, nullptr);
    char* result = bifrost_run_python(code_str);
    env->ReleaseStringUTFChars(code, code_str);
    
    if (result) {
        jstring jresult = env->NewStringUTF(result);
        bifrost_free_string(result);
        return jresult;
    }
    return env->NewStringUTF("{\"success\":false,\"error\":\"Python execution failed\"}");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_termuxultra_MainActivity_bifrostInfer(JNIEnv *env, jobject /* this */, jstring prompt) {
    const char* prompt_str = env->GetStringUTFChars(prompt, nullptr);
    
    // Create a simple inference task
    std::string task_json = "{\"id\":\"" + std::to_string(rand()) + "\",\"agent_type\":\"python\",\"command\":\"print('AI response to: " + std::string(prompt_str) + "')\",\"args\":[],\"environment\":{}}";
    
    char* result = bifrost_execute_task(task_json.c_str());
    env->ReleaseStringUTFChars(prompt, prompt_str);
    
    if (result) {
        jstring jresult = env->NewStringUTF(result);
        bifrost_free_string(result);
        return jresult;
    }
    return env->NewStringUTF("{\"success\":false,\"error\":\"Inference failed\"}");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_termuxultra_MainActivity_initBifrost(JNIEnv *env, jobject /* this */) {
    return bifrost_init();
}