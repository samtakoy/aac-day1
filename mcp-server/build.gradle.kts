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
    implementation("io.ktor:ktor-server-core:3.2.3")
    implementation("io.ktor:ktor-server-netty:3.2.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.2.3")
    implementation("io.ktor:ktor-server-sse:3.2.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.3")
    implementation("io.ktor:ktor-client-core:3.2.3")
    implementation("io.ktor:ktor-client-okhttp:3.2.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.2.3")
    implementation("io.ktor:ktor-client-logging:3.2.3")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}

application {
    mainClass.set("com.example.day.mcpserver.McpServerKt")
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set("mcp-server")
    archiveVersion.set("")
}
