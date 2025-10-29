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
<<<<<<< HEAD
=======
        maven { url = java.net.URI("https://devrepo.kakao.com/nexus/content/groups/public/") }
>>>>>>> 051e103096799e51629f301552ffd1c0542ca37d
    }
}

rootProject.name = "My Application"
include(":app")
