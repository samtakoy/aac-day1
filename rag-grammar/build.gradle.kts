import org.gradle.jvm.tasks.Jar
import java.io.File
import java.util.Properties

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
val generatedCmakeLists = layout.buildDirectory.file("generated/CMakeLists.txt")

fun resolveCmakeCommand(): String {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val executableName = if (isWindows) "cmake.exe" else "cmake"
    val localPropertiesFile = rootProject.file("local.properties")

    if (!localPropertiesFile.exists()) return executableName

    val properties = Properties().apply {
        localPropertiesFile.inputStream().use(::load)
    }

    val sdkDirValue = properties.getProperty("sdk.dir") ?: return executableName
    val sdkDir = file(sdkDirValue)
    val cmakeRoot = sdkDir.resolve("cmake")
    if (!cmakeRoot.exists() || !cmakeRoot.isDirectory) return executableName

    val candidates = cmakeRoot
        .listFiles()
        ?.filter { it.isDirectory }
        ?.sortedByDescending { it.name }
        .orEmpty()

    for (versionDir in candidates) {
        val cmakeBinary = versionDir.resolve("bin").resolve(executableName)
        if (cmakeBinary.exists()) {
            return cmakeBinary.absolutePath
        }
    }

    return executableName
}

fun resolveJavaHomeForCmake(): String? {
    val envJavaHome = System.getenv("JAVA_HOME")?.takeIf { it.isNotBlank() }?.let(::file)
    if (envJavaHome != null && envJavaHome.resolve("include/jni.h").exists()) {
        return envJavaHome.absolutePath.replace('\\', '/')
    }

    val jdksDir = File(System.getProperty("user.home")).resolve(".jdks")
    if (!jdksDir.exists() || !jdksDir.isDirectory) return null

    val candidates = jdksDir
        .listFiles()
        ?.filter { it.isDirectory && it.resolve("include/jni.h").exists() }
        ?.sortedByDescending { it.name }
        .orEmpty()

    return candidates.firstOrNull()?.absolutePath?.replace('\\', '/')
}

val cmakeCommand = resolveCmakeCommand()
val cmakeJavaHome = resolveJavaHomeForCmake()

val normalizeGeneratedCmakePaths = tasks.register("normalizeGeneratedCmakePaths") {
    dependsOn("generateGrammarFiles")
    inputs.file(generatedCmakeLists)
    outputs.file(generatedCmakeLists)
    doLast {
        val cmakeListsFile = generatedCmakeLists.get().asFile
        var normalized = cmakeListsFile.readText().replace('\\', '/')
        if (cmakeJavaHome != null) {
            val jniIncludes =
                "include_directories(\"$cmakeJavaHome/include\" \"$cmakeJavaHome/include/win32\")"
            normalized = normalized.replace(
                "find_package(JNI REQUIRED)",
                "find_package(JNI REQUIRED)\n$jniIncludes",
            )
        }
        cmakeListsFile.writeText(normalized)
    }
}

val cmakeConfigure = tasks.register<Exec>("cmakeConfigure") {
    dependsOn(normalizeGeneratedCmakePaths)
    inputs.dir("grammar/kotlin/src")
    inputs.file(generatedCmakeLists)
    outputs.dir(cmakeBuildDir)
    doFirst { cmakeBuildDir.get().asFile.mkdirs() }
    workingDir(cmakeBuildDir)
    commandLine(
        cmakeCommand,
        layout.buildDirectory.dir("generated").get().asFile.absolutePath,
        "-DCMAKE_BUILD_TYPE=Release",
        *(cmakeJavaHome?.let { arrayOf("-DJAVA_HOME=$it") } ?: emptyArray()),
    )
    if (cmakeJavaHome != null) {
        environment("JAVA_HOME", cmakeJavaHome)
    }
}

val cmakeBuild = tasks.register<Exec>("cmakeBuild") {
    dependsOn(cmakeConfigure)
    workingDir(cmakeBuildDir)
    commandLine(cmakeCommand, "--build", ".", "--config", "Release")
    if (cmakeJavaHome != null) {
        environment("JAVA_HOME", cmakeJavaHome)
    }
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
