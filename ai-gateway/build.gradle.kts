import org.gradle.jvm.tasks.Jar

plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "com.example.day"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared:simple-chat-api"))
    implementation("io.ktor:ktor-server-core:3.2.3")
    implementation("io.ktor:ktor-server-netty:3.2.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.2.3")
    implementation("io.ktor:ktor-server-rate-limit:3.2.3")
    implementation("io.ktor:ktor-server-forwarded-header:3.2.3")
    implementation("io.ktor:ktor-client-core:3.2.3")
    implementation("io.ktor:ktor-client-okhttp:3.2.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.2.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.3")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation("org.slf4j:slf4j-simple:2.0.17")

    testImplementation("io.ktor:ktor-server-test-host:3.2.3")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.example.day.aigateway.AiGatewayServerKt")
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set("ai-gateway")
    archiveVersion.set("")
    manifest { attributes["Main-Class"] = "com.example.day.aigateway.AiGatewayServerKt" }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
