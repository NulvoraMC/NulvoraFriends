pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "nulvorafriends"

include("common")
include("velocity")
include("papermc")
