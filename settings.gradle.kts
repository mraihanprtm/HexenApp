// settings.gradle.kts (Project Level)
// File ini digunakan untuk mendefinisikan repositori plugin dan modul yang termasuk dalam proyek.

pluginManagement {
    repositories {
        google() // Repositori Google untuk plugin Android dan library lainnya
        mavenCentral() // Repositori Maven Central
        gradlePluginPortal() // Portal Plugin Gradle
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // Pengaturan mode resolusi dependensi
    repositories {
        google()
        mavenCentral()
        // Tambahkan repositori lain jika diperlukan, misalnya JitPack
        // maven("https://jitpack.io")
    }
}

// Mendefinisikan nama root proyek
rootProject.name = "HexenApp"
// Menyertakan modul 'app' dalam proyek
include(":app")