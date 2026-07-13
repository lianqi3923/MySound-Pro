import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin { jvmToolchain(17) }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

val d8 by configurations.creating

dependencies {
    implementation(project(":mysound-api"))
    implementation(project(":mysound-core"))
    implementation(project(":mysound-sources"))
    implementation(libs.kotlinx.coroutines.core)
    compileOnly(project(":mysound-myting-stubs"))
    d8(libs.r8)

    testImplementation(project(":mysound-myting-stubs"))
    testImplementation(project(":mysound-testkit"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}

tasks.test { useJUnitPlatform() }

val pluginJvmJar by tasks.registering(Jar::class) {
    group = "distribution"
    description = "Builds the JVM input JAR containing only MySound-Pro classes."
    dependsOn(configurations.runtimeClasspath)
    archiveFileName.set("my_sound_pro-jvm.jar")
    destinationDirectory.set(layout.buildDirectory.dir("release"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)
    from({
        val embeddedPrefixes = listOf(
            "mysound-",
            "kotlinx-coroutines-core-jvm-",
            "kotlinx-serialization-core-jvm-",
            "kotlinx-serialization-json-jvm-",
            "okhttp-brotli-",
            "dec-",
        )
        configurations.runtimeClasspath.get()
            .filter { dependency -> embeddedPrefixes.any(dependency.name::startsWith) }
            .map(::zipTree)
    })
    exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
}

val verifyPluginJvmJar by tasks.registering {
    group = "verification"
    dependsOn(pluginJvmJar)
    doLast {
        ZipFile(pluginJvmJar.get().archiveFile.get().asFile).use { jar ->
            check(jar.getEntry("com/github/eprendre/sources_by_mysound_pro/SourceEntry.class") != null) {
                "MyTingShu SourceEntry is missing from the plugin JAR"
            }
            check(jar.getEntry("io/github/mysoundpro/api/AudioSource.class") != null) {
                "MySound-Pro API classes are missing from the plugin JAR"
            }
            check(jar.entries().asSequence().none { it.name.startsWith("com/github/eprendre/tingshu/") }) {
                "MyTingShu clean-room stubs must never be packaged"
            }
            check(jar.getEntry("okhttp3/brotli/BrotliInterceptor.class") != null) {
                "Brotli runtime is missing from the plugin JAR"
            }
            check(jar.getEntry("kotlinx/serialization/json/Json.class") != null) {
                "Serialization runtime is missing from the plugin JAR"
            }
            check(jar.getEntry("okhttp3/OkHttpClient.class") == null) {
                "Host-provided OkHttp must not be duplicated"
            }
            check(jar.getEntry("kotlin/Unit.class") == null) {
                "Host-provided Kotlin standard library must not be duplicated"
            }
        }
    }
}

val d8PluginJar by tasks.registering(JavaExec::class) {
    group = "distribution"
    description = "Converts the verified JVM plugin JAR into a MyTingShu-loadable DEX JAR."
    dependsOn(verifyPluginJvmJar)
    classpath = d8
    mainClass.set("com.android.tools.r8.D8")
    val outputJar = layout.buildDirectory.file("release/my_sound_pro.jar")
    val androidCompileClasspath = files(configurations.compileClasspath, configurations.runtimeClasspath)
    outputs.file(outputJar)
    inputs.files(androidCompileClasspath)
    doFirst {
        outputJar.get().asFile.delete()
        args(
            "--min-api", "21",
            "--output", outputJar.get().asFile.absolutePath,
        )
        androidCompileClasspath.files.forEach { dependency ->
            args("--classpath", dependency.absolutePath)
        }
        args(pluginJvmJar.get().archiveFile.get().asFile.absolutePath)
    }
}

val verifyD8PluginJar by tasks.registering {
    group = "verification"
    dependsOn(d8PluginJar)
    doLast {
        val dexJar = layout.buildDirectory.file("release/my_sound_pro.jar").get().asFile
        ZipFile(dexJar).use { jar ->
            check(jar.getEntry("classes.dex") != null) { "D8 output does not contain classes.dex" }
        }
    }
}

tasks.check { dependsOn(verifyD8PluginJar) }
