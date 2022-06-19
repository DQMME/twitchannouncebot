import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.google.devtools.ksp") version "1.6.21-1.0.5"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"
    id("dev.schlaubi.mikbot.gradle-plugin") version "2.3.2"
}

group = "de.dqmme"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    @Suppress("DependencyOnStdlib")
    compileOnly(kotlin("stdlib-jdk8"))
    mikbot("dev.schlaubi", "mikbot-api", "3.2.0-SNAPSHOT")
    ksp("dev.schlaubi", "mikbot-plugin-processor", "2.2.0")
    implementation("com.github.twitch4j", "twitch4j", "1.10.0")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-rx2", "1.6.2")
    implementation("com.github.akarnokd", "rxjava2-interop", "0.13.7")
    plugin("dev.schlaubi", "mikbot-ktor", "2.2.2")
}

mikbotPlugin {
    description.set("A bot that announces Twitch Streams!")
    provider.set("DQMME")
    license.set("MIT")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "18"
            freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
        }
    }
}

pluginPublishing {
    repositoryUrl.set("https://katze.streamerflash.de")
    targetDirectory.set(rootProject.file("ci-repo").toPath())
    projectUrl.set("https://github.com/DQMME/nothing")
}
