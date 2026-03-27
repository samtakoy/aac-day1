import org.gradle.jvm.tasks.Jar

plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "com.example.day"
version = "1.0.0"

dependencies {
    implementation(libs.mcp.kotlin.sdk.server)
    // Ktor 3.2.3 — must match mcp-server; MCP SDK 0.8.4 is binary-incompatible with Ktor 3.4+
    implementation("io.ktor:ktor-server-core:3.2.3")
    implementation("io.ktor:ktor-server-netty:3.2.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.2.3")
    implementation("io.ktor:ktor-server-sse:3.2.3")
    implementation("io.ktor:ktor-client-core:3.2.3")
    implementation("io.ktor:ktor-client-okhttp:3.2.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.2.3")
    implementation("io.ktor:ktor-client-logging:3.2.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.3")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    // AST-based chunking
    implementation(project(":rag-grammar"))
    implementation("io.github.tree-sitter:ktreesitter:0.24.1")

    // Exposed + SQLite
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
    implementation("org.slf4j:slf4j-simple:2.0.17")
}

application {
    mainClass.set("com.example.day.ragserver.RagServerKt")
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set("rag-server")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "com.example.day.ragserver.RagServerKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
