import org.gradle.api.internal.HasConvention
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    idea
    java
    kotlin("jvm").version("1.3.70")
    id("org.jetbrains.intellij").version("0.4.18")
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
        apiVersion = "1.3"
        languageVersion = "1.3"
        // Compiler flag to allow building against pre-released versions of Kotlin
        // because IJ EAP can be built using pre-released Kotlin but it's still worth doing to check API compatibility
        freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-metadata-version-check")
    }
}

configure<IntelliJPluginExtension> {
    val ideVersion = System.getenv().getOrDefault("IJ_VERSION",
        "IC-172.3757.29"
//        "LATEST-EAP-SNAPSHOT"
    )
    println("Using ide version: $ideVersion")
    version = ideVersion
    pluginName = "man-page-viewer"
    downloadSources = true
    sameSinceUntilBuild = false
    updateSinceUntilBuild = false
}

task(name = "runIdeWithDifferentJvm", type = RunIdeTask::class, configuration = {
    setJbrVersion("jbrex8u152b1024.10")
})
