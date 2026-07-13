pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "MySound-Pro"

include(
    ":mysound-api",
    ":mysound-core",
    ":mysound-registry-processor",
    ":mysound-sources",
    ":mysound-myting-stubs",
    ":mysound-myting-host",
    ":mysound-testkit",
)
