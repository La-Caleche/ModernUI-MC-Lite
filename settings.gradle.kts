pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
    }

    plugins {
        id("fabric-loom") version settings.providers.gradleProperty("loomVersion")
    }
}

rootProject.name = "modernui-mc-lite"
