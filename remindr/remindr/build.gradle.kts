import com.kizitonwose.remindr.buildsrc.Config
import com.kizitonwose.remindr.buildsrc.Version

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.mavenPublish)
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

dependencies {
    compileOnly(libs.compose.runtime) // Only needed for @Immutable annotation.
}

mavenPublishing {
    coordinates(version = Version.android)
}
