// Automatically generated file. DO NOT MODIFY

#include <jni.h>
#include <tree-sitter-kotlin.h>

#ifndef __ANDROID__
#define NATIVE_FUNCTION(name) JNIEXPORT jlong JNICALL name(JNIEnv * _env, jclass _class)
#else
#define NATIVE_FUNCTION(name) JNIEXPORT jlong JNICALL name()
#endif

NATIVE_FUNCTION(Java_com_example_spike_grammar_KotlinLanguage_tree_1sitter_1kotlin) {
    return (jlong)tree_sitter_kotlin();
}
