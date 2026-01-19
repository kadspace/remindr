import com.kizitonwose.remindr.buildsrc.Android
import com.kizitonwose.remindr.buildsrc.Config
import com.kizitonwose.remindr.buildsrc.Version

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.mavenPublish)
}

android {
    compileSdk = Android.compileSdk
    namespace = "com.kizitonwose.remindr.view"
    defaultConfig {
        minSdk = Android.minSdk
    }
    java {
        toolchain {
            languageVersion.set(Config.compatibleJavaLanguageVersion)
        }
    }
    kotlin {
        jvmToolchain {
            languageVersion.set(Config.compatibleJavaLanguageVersion)
        }
    }
}

dependencies {
    api(project(":remindr"))
    implementation(project(":data"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.core.ktx)

    // Expose RecyclerView which is CalendarView"s superclass.
    api(libs.androidx.recyclerview)
}

mavenPublishing {
    coordinates(version = Version.android)
}
