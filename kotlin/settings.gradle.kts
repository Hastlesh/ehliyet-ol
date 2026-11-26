// Ehliyet Ol — Kotlin Multiplatform (Android + iOS), arayüz Compose Multiplatform
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "EhliyetOl"
include(":composeApp")
