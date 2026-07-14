import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.security.MessageDigest
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

kotlin { jvmToolchain(17) }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

val d8 by configurations.creating
val runtimeClasspathForPlugin = configurations.runtimeClasspath

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

val pluginJvmJar = tasks.named<ShadowJar>("shadowJar") {
    group = "distribution"
    description = "Builds the JVM input JAR containing only MySound-Pro classes."
    dependsOn(runtimeClasspathForPlugin)
    archiveFileName.set("my_sound_pro-jvm.jar")
    destinationDirectory.set(layout.buildDirectory.dir("release"))
    configurations = emptyList()
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)
    from({
        val embeddedPrefixes = listOf(
            "mysound-",
            "kotlinx-coroutines-core-jvm-",
            "kotlinx-serialization-core-jvm-",
            "kotlinx-serialization-json-jvm-",
            "jsoup-",
            "okhttp-brotli-",
            "dec-",
        )
        runtimeClasspathForPlugin.get()
            .filter { dependency -> embeddedPrefixes.any(dependency.name::startsWith) }
            .map(::zipTree)
    })
    // MyTingShu loads plugin classes parent-first and ships an incompatible,
    // obfuscated coroutine runtime. Relocation gives the plugin an isolated ABI.
    relocate("kotlinx.coroutines", "io.github.mysoundpro.shadow.coroutines")
    relocate("kotlinx.serialization", "io.github.mysoundpro.shadow.serialization")
    relocate("org.jsoup", "io.github.mysoundpro.shadow.jsoup")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
}

val verifyPluginJvmJar by tasks.registering {
    group = "verification"
    dependsOn(pluginJvmJar)
    doLast {
        ZipFile(pluginJvmJar.get().archiveFile.get().asFile).use { jar ->
            check(jar.getEntry("com/github/eprendre/my_sound_pro/SourceEntry.class") != null) {
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
            check(jar.getEntry("io/github/mysoundpro/shadow/serialization/json/Json.class") != null) {
                "Relocated serialization runtime is missing from the plugin JAR"
            }
            check(jar.getEntry("io/github/mysoundpro/shadow/jsoup/Jsoup.class") != null) {
                "Relocated Jsoup runtime is missing from the plugin JAR"
            }
            check(jar.getEntry("okhttp3/OkHttpClient.class") == null) {
                "Host-provided OkHttp must not be duplicated"
            }
            check(jar.getEntry("kotlin/Unit.class") == null) {
                "Host-provided Kotlin standard library must not be duplicated"
            }
            check(jar.entries().asSequence().none {
                it.name.startsWith("kotlinx/coroutines/") && it.name.endsWith(".class")
            }) {
                "Host-provided coroutines must not be duplicated"
            }
            check(jar.getEntry("io/github/mysoundpro/shadow/coroutines/BuildersKt.class") != null) {
                "Relocated coroutine runtime is missing from the plugin JAR"
            }
            check(jar.entries().asSequence().none {
                it.name.startsWith("org/jsoup/") && it.name.endsWith(".class")
            }) {
                "Unrelocated Jsoup classes must not be packaged"
            }
            check(jar.entries().asSequence().none {
                it.name.startsWith("kotlinx/serialization/") && it.name.endsWith(".class")
            }) {
                "Unrelocated serialization classes must not be packaged"
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

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

val prepareRelease by tasks.registering {
    group = "distribution"
    description = "Builds the complete auditable MySound-Pro release bundle."
    dependsOn(verifyD8PluginJar, ":mysound-sources:verifyParserLineLimits")
    val releaseDir = layout.buildDirectory.dir("release")
    inputs.property("semanticVersion", project.provider { project.version.toString() })
    inputs.property("pluginVersionCode", providers.gradleProperty("pluginVersionCode"))
    inputs.property("projectUrl", providers.gradleProperty("projectUrl"))
    inputs.files(rootProject.files("README.md", "CHANGELOG.md", "LICENSE"))
    outputs.dir(releaseDir)
    doLast {
        val output = releaseDir.get().asFile
        output.mkdirs()
        val jar = output.resolve("my_sound_pro.jar")
        val hash = sha256(jar)
        val semanticVersion = project.version.toString()
        val versionCode = providers.gradleProperty("pluginVersionCode").get()
        val sourceUrl = providers.gradleProperty("projectUrl").get().trimEnd('/')
        val downloadUrl = "$sourceUrl/releases/download/v$semanticVersion/my_sound_pro.jar"

        output.resolve("my_sound_pro.json").writeText(
            """{
  "name": "MySound-Pro",
  "version": "$semanticVersion",
  "versionCode": $versionCode,
  "minimumMyTingShuVersion": "2.6.0",
  "testedMyTingShuVersion": "2.6.0",
  "entryPackage": "com.github.eprendre.my_sound_pro",
  "license": "MIT",
  "sourceUrl": "$sourceUrl",
  "downloadUrl": "$downloadUrl",
  "sha256": "$hash",
  "sources": ["gutenberg-audio", "librivox"]
}
""",
        )
        output.resolve("update.json").writeText(
            """{
  "version": $versionCode,
  "entry_package": "com.github.eprendre.my_sound_pro",
  "download_url": "$downloadUrl",
  "update_msg": "MySound-Pro $semanticVersion：首批公开公版有声书来源、动态配置与可靠性测试。",
  "support_url": "$sourceUrl"
}
""",
        )

        val dependencies = runtimeClasspathForPlugin.get().resolvedConfiguration.resolvedArtifacts
            .map { "${it.moduleVersion.id.group}:${it.name}:${it.moduleVersion.id.version}" }
            .distinct().sorted()
        output.resolve("my_sound_pro.sbom.json").writeText(buildString {
            appendLine("{\"bomFormat\":\"CycloneDX\",\"specVersion\":\"1.5\",\"version\":1,")
            appendLine("\"metadata\":{\"component\":{\"type\":\"library\",\"name\":\"MySound-Pro\",\"version\":\"$semanticVersion\"}},")
            appendLine("\"components\":[")
            dependencies.forEachIndexed { index, coordinate ->
                val parts = coordinate.split(':')
                val comma = if (index == dependencies.lastIndex) "" else ","
                appendLine("{\"type\":\"library\",\"group\":\"${parts[0]}\",\"name\":\"${parts[1]}\",\"version\":\"${parts[2]}\",\"purl\":\"pkg:maven/${parts[0]}/${parts[1]}@${parts[2]}\"}$comma")
            }
            appendLine("]}")
        })
        output.resolve("THIRD_PARTY_NOTICES.md").writeText(
            """# Third-party notices

The release is built from the dependencies recorded in `my_sound_pro.sbom.json`.

- Kotlin and kotlinx libraries: Apache License 2.0.
- OkHttp and Okio: Apache License 2.0 (host-provided runtime, not duplicated in the plugin).
- jsoup: MIT License.
- Brotli decoder and OkHttp Brotli integration: MIT / Apache License 2.0 as declared by their upstream projects.

See each dependency's upstream repository and published POM for the authoritative license text.
""",
        )
        listOf("README.md", "CHANGELOG.md", "LICENSE").forEach { name ->
            rootProject.file(name).copyTo(output.resolve(name), overwrite = true)
        }
        output.resolve("SHA256SUMS").writeText("$hash  my_sound_pro.jar\n")
    }
}

val verifyReleaseBundle by tasks.registering {
    group = "verification"
    dependsOn(prepareRelease)
    doLast {
        val output = layout.buildDirectory.dir("release").get().asFile
        val required = listOf(
            "my_sound_pro.jar", "my_sound_pro.json", "update.json", "my_sound_pro.sbom.json",
            "SHA256SUMS", "THIRD_PARTY_NOTICES.md", "README.md", "CHANGELOG.md", "LICENSE",
        )
        check(required.all { output.resolve(it).isFile }) { "release bundle is incomplete" }
        val hash = sha256(output.resolve("my_sound_pro.jar"))
        check(output.resolve("my_sound_pro.json").readText().contains("\"sha256\": \"$hash\"")) {
            "release manifest SHA-256 does not match JAR"
        }
        val update = output.resolve("update.json").readText()
        check(update.contains("\"entry_package\": \"com.github.eprendre.my_sound_pro\""))
        check(update.contains("\"version\": ${providers.gradleProperty("pluginVersionCode").get()}"))
        check(output.resolve("mysound-pro-default-config.json").exists().not())
    }
}

tasks.check { dependsOn(verifyReleaseBundle) }
