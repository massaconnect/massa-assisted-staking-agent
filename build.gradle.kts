import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
    kotlin("plugin.serialization") version "1.9.21"
}

group = "com.massapay"
version = "1.0.0"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // QR Code generation
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.google.zxing:javase:3.5.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Ktor Server for WebSocket
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-websockets:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    
    // OkHttp for HTTP requests to Massa Node
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

compose.desktop {
    application {
        mainClass = "com.massapay.agent.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MassaAgent"
            packageVersion = "1.0.0"
            description = "Massa Agent - Remote Staking Control"
            vendor = "MassaPay"

            windows {
                menuGroup = "MassaPay"
                upgradeUuid = "2F9B3C4D-5E6F-7A8B-9C0D-1E2F3A4B5C6D"
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
        }
    }
}

kotlin {
    jvmToolchain(17)
}
