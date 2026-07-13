plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
    group = "io.github.mysoundpro"
    version = providers.gradleProperty("projectVersion").get()
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(8)
        }
    }
}
