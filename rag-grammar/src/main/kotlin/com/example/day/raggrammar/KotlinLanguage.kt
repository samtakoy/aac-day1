package com.example.day.raggrammar

import java.io.File

/**
 * JVM wrapper for tree-sitter Kotlin grammar (fwcd/tree-sitter-kotlin).
 * Manually written — no KMP expect/actual.
 *
 * Native lib loading:
 * 1. System.loadLibrary("kotlin") — from java.library.path
 * 2. Fallback: extract from classpath /lib/{os}/{arch}/libkotlin.{ext}
 */
object KotlinLanguage {
    private const val LIB_NAME = "kotlin"

    init {
        try {
            System.loadLibrary(LIB_NAME)
        } catch (ex: UnsatisfiedLinkError) {
            System.load(libPath() ?: throw ex)
        }
    }

    fun language(): Any = tree_sitter_kotlin()

    @JvmStatic
    private external fun tree_sitter_kotlin(): Long

    internal fun libPath(): String? {
        val osName = System.getProperty("os.name")!!.lowercase()
        val archName = System.getProperty("os.arch")!!.lowercase()
        val ext: String
        val os: String
        val prefix: String
        when {
            "windows" in osName -> { ext = "dll"; os = "windows"; prefix = "" }
            "linux" in osName -> { ext = "so"; os = "linux"; prefix = "lib" }
            "mac" in osName -> { ext = "dylib"; os = "macos"; prefix = "lib" }
            else -> throw UnsupportedOperationException("Unsupported OS: $osName")
        }
        val arch = when {
            "aarch64" in archName || "arm64" in archName -> "aarch64"
            "amd64" in archName || "x86_64" in archName -> "x64"
            else -> throw UnsupportedOperationException("Unsupported arch: $archName")
        }
        val libPath = "/lib/$os/$arch/$prefix$LIB_NAME.$ext"
        val libUrl = javaClass.getResource(libPath) ?: return null
        return File.createTempFile(prefix + LIB_NAME, ".$ext").apply {
            writeBytes(libUrl.openStream().use { it.readAllBytes() })
            deleteOnExit()
        }.path
    }
}
