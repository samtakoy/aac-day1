import org.gradle.jvm.tasks.Jar

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.github.tree-sitter.ktreesitter-plugin") version "0.24.1"
}

group = "com.example.day"
version = "1.0.0"

grammar {
    grammarName = "kotlin"
    baseDir = file("grammar/kotlin")
    files = arrayOf(
        file("grammar/kotlin/src/parser.c"),
        file("grammar/kotlin/src/scanner.c"),
    )
    packageName = "com.example.day.raggrammar"
    className = "KotlinLanguage"
    libraryName = "kotlin"
    languageMethods = mapOf("language" to "tree_sitter_kotlin")
}

val cmakeBuildDir = layout.buildDirectory.dir("cmake-build")

val cmakeConfigure = tasks.register<Exec>("cmakeConfigure") {
    dependsOn("generateGrammarFiles")
    inputs.dir("grammar/kotlin/src")
    inputs.file(layout.buildDirectory.file("generated/CMakeLists.txt"))
    outputs.dir(cmakeBuildDir)
    doFirst { cmakeBuildDir.get().asFile.mkdirs() }
    workingDir(cmakeBuildDir)
    commandLine(
        "cmake",
        layout.buildDirectory.dir("generated").get().asFile.absolutePath,
        "-DCMAKE_BUILD_TYPE=Release",
    )
}

val cmakeBuild = tasks.register<Exec>("cmakeBuild") {
    dependsOn(cmakeConfigure)
    workingDir(cmakeBuildDir)
    commandLine("cmake", "--build", ".", "--config", "Release")
    outputs.dir(cmakeBuildDir)
}

val copyNativeLib = tasks.register<Copy>("copyNativeLib") {
    dependsOn(cmakeBuild)
    val osName = System.getProperty("os.name")!!.lowercase()
    val archName = System.getProperty("os.arch")!!.lowercase()
    val (os, prefix, ext) = when {
        "mac" in osName -> Triple("macos", "lib", "dylib")
        "linux" in osName -> Triple("linux", "lib", "so")
        else -> Triple("windows", "", "dll")
    }
    val arch = when {
        "aarch64" in archName || "arm64" in archName -> "aarch64"
        else -> "x64"
    }
    from(cmakeBuildDir) {
        include("${prefix}kotlin.${ext}")
    }
    into(layout.buildDirectory.dir("resources/main/lib/$os/$arch"))
}

tasks.named("processResources") { dependsOn(copyNativeLib) }
tasks.named("compileKotlin") { dependsOn("generateGrammarFiles") }

dependencies {
    implementation("io.github.tree-sitter:ktreesitter:0.24.1")
}
