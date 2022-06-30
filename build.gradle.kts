import dev.schlaubi.mikbot.gradle.GenerateDefaultTranslationBundleTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Locale

plugins {
    id("com.google.devtools.ksp") version "1.7.0-1.0.6"
    kotlin("jvm") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.0"
    id("dev.schlaubi.mikbot.gradle-plugin") version "2.4.1"
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
    mikbot("dev.schlaubi", "mikbot-api", "3.3.0-SNAPSHOT")
    ksp("dev.schlaubi", "mikbot-plugin-processor", "2.2.0")
    implementation("com.github.twitch4j", "twitch4j", "1.10.0")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-rx2", "1.6.2")
    implementation("com.github.akarnokd", "rxjava2-interop", "0.13.7")
    plugin("dev.schlaubi", "mikbot-ktor", "2.3.0")
    plugin("dev.schlaubi", "mikbot-game-animator", "2.4.0")
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

    val generateDefaultResourceBundle = task<GenerateDefaultTranslationBundleTask>("generateDefaultResourceBundle") {
        defaultLocale.set(Locale("en", "GB"))
    }

    assemblePlugin {
        dependsOn(generateDefaultResourceBundle)
    }

    assembleBot {
        bundledPlugins.set(
            listOf("game-animator@2.4.0")
        )
    }
}

pluginPublishing {
    repositoryUrl.set("https://twitchannounce.dqmme.gay")
    targetDirectory.set(rootProject.file("ci-repo").toPath())
    projectUrl.set("https://github.com/DQMME/twitchannouncebot")
}
