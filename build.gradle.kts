// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.dagger.hilt.android") version "2.57.2" apply false
    kotlin("kapt") version "2.2.10" apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory) // buildDir yerine layout.buildDirectory kullanıyoruz
}