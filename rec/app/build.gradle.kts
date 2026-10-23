import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hihi.recorder"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.hihi.recorder"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    // AGP 9 uses built-in Kotlin. The classpath override in the root project
    // upgrades KGP, while this keeps the compiler itself on the same version as
    // the Compose compiler plugin (the built-in default may still be older).
    add("kotlinCompilerClasspath", "org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.21")

    // Miuix (HyperOS / Miuix style components)
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4")

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")

    // Compose 1.12 is the version Miuix 0.9.4 is built against.
    implementation("androidx.compose.foundation:foundation:1.12.0")
    implementation("androidx.compose.ui:ui:1.12.0")
    implementation("androidx.compose.animation:animation:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
