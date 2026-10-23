import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Base64

// CI provides the release keystore as base64 through the repository secrets.
// When the secrets are absent (for example on pull requests from forks), the
// release build falls back to the debug keystore so the APK can still be built.
val signingKeystoreBase64 = System.getenv("SIGNING_KEYSTORE_BASE64")
val signingStorePassword = System.getenv("SIGNING_STORE_PASSWORD")
val signingKeyAlias = System.getenv("SIGNING_KEY_ALIAS")
val signingKeyPassword = System.getenv("SIGNING_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    signingKeystoreBase64,
    signingStorePassword,
    signingKeyAlias,
    signingKeyPassword,
).all { !it.isNullOrBlank() }

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

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                val keystoreFile = rootProject.layout.projectDirectory.file(".signing/release.jks").asFile
                keystoreFile.parentFile.mkdirs()
                keystoreFile.writeBytes(
                    Base64.getMimeDecoder().decode(requireNotNull(signingKeystoreBase64)),
                )
                storeFile = keystoreFile
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "Release signing secrets are not configured: " +
                        "SIGNING_KEYSTORE_BASE64 / SIGNING_STORE_PASSWORD / " +
                        "SIGNING_KEY_ALIAS / SIGNING_KEY_PASSWORD. " +
                        "The release APK will be signed with the debug keystore.",
                )
                signingConfigs.getByName("debug")
            }
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
