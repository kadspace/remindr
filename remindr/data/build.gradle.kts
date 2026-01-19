
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
    implementation(project(":remindr"))
    implementation(libs.kotlin.stdlib)

    testImplementation(platform(libs.test.junit5.bom))
    testImplementation(libs.test.junit5.api)
    testRuntimeOnly(libs.test.junit5.engine)
    testRuntimeOnly(libs.test.junit.platform.launcher)
}

mavenPublishing {
    coordinates(version = Version.android)
}
