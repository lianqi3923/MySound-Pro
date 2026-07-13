plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

kotlin { jvmToolchain(17) }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation(project(":mysound-api"))
    implementation(project(":mysound-core"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)
    ksp(project(":mysound-registry-processor"))

    testImplementation(project(":mysound-testkit"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("mysound.live", providers.gradleProperty("mysound.live").getOrElse("false"))
    systemProperty("mysound.live.sampleSize", providers.gradleProperty("mysound.live.sampleSize").getOrElse("100"))
}

val verifyParserLineLimits by tasks.registering {
    group = "verification"
    description = "Rejects source Parser files longer than 300 lines."
    doLast {
        fileTree("src/main/kotlin/io/github/mysoundpro/sources") { include("*.kt") }.files.forEach { parser ->
            check(parser.readLines().size <= 300) { "Parser exceeds 300 lines: ${parser.name}" }
        }
    }
}

tasks.check { dependsOn(verifyParserLineLimits) }
