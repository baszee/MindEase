// settings.gradle.kts

import java.net.URI // <-- Add this import statement at the top

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Corrected line: Use uri() to convert the String to a URI
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MindEase"
include(":app")
