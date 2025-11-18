pluginManagement {
    repositories {
        google()        // ✅ Necesario para ML Kit
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()        // ✅ Importante
        mavenCentral()
    }
}
rootProject.name = "SecureLoginApp"
include(":app")

