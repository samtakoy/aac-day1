// Automatically generated file. DO NOT MODIFY

package com.example.spike.grammar

import dalvik.annotation.optimization.CriticalNative
import javax.annotation.processing.Generated

@Suppress("FunctionName")
@Generated("io.github.treesitter.ktreesitter-plugin")
actual object KotlinLanguage {
    init {
        System.loadLibrary("kotlin")
    }

    actual fun language(): Any = tree_sitter_kotlin()

    @JvmStatic
    @CriticalNative
    private external fun tree_sitter_kotlin(): Long

}
