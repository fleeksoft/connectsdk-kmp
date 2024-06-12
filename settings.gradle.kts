pluginManagement {
    // Configure repositories required for the Amper plugin
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/amper/amper")
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}

plugins {
    // Add the plugin
    id("org.jetbrains.amper.settings.plugin").version("0.3.1")
}