import org.gradle.api.internal.HasConvention
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    idea
    java
    kotlin("jvm").version("1.6.21")
    id("org.jetbrains.intellij").version("1.13.3")
}
repositories {
    mavenCentral()
}

val SourceSet.kotlin: SourceDirectorySet
    get() = (this as HasConvention).convention.getPlugin<KotlinSourceSet>().kotlin

sourceSets {
    main {
        kotlin.srcDirs("./src")
        resources.srcDirs("./resources")
    }
    test {
        kotlin.srcDirs("./test")
    }
}

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "11"
        apiVersion = "1.6"
        languageVersion = "1.6"
        // Compiler flag to allow building against pre-released versions of Kotlin
        // because IJ EAP can be built using pre-released Kotlin but it's still worth doing to check API compatibility
        freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-metadata-version-check")
    }
}

configure<IntelliJPluginExtension> {
    val ideVersion = System.getenv().getOrDefault("IJ_VERSION",
        "212.4746.92" // with kotlin 1.5.10 support
//        "LATEST-EAP-SNAPSHOT"
    )
    println("Using ide version: $ideVersion")
    version.set(ideVersion)
    pluginName.set("man-page-viewer")
    downloadSources.set(true)
    sameSinceUntilBuild.set(false)
    updateSinceUntilBuild.set(false)
}
