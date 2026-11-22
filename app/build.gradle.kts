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
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.hihi.recorder"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.hihi.recorder"
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "1.2.0"

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
        aidl = true
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
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-squircle-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-nav-android:0.9.4")

    // Glass modules are only published from 0.9.5-beta01. Exclude their
    // transitive Miuix modules so the rest of the app stays on 0.9.4.
    val glassGroup = "top.yukonga.miuix.kmp"
    implementation("top.yukonga.miuix.kmp:miuix-glass-icons-android:0.9.5-beta01") {
        exclude(group = glassGroup)
    }
    implementation("top.yukonga.miuix.kmp:miuix-glass-android:0.9.5-beta01") {
        exclude(group = glassGroup)
    }

    // Backdrop primitives used by the liquid toggle and slider.
    implementation("io.github.kyant0:backdrop:1.0.6")
    implementation("io.github.kyant0:shapes:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // Optional Material 3 presentation for people who prefer stock Android.
    implementation("androidx.compose.material3:material3:1.4.0")

    // Icon packs selectable in settings.
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("io.github.ardasoyturk.compose.icons:lucide-android:2.0.7")
    implementation("io.github.oneuiproject:icons:1.1.0")

    // COUI (ColorOS 17 style) as an alternative UI language.
    val couiVersion = "0.0.1-alpha14"
    implementation("io.github.itoseo:coui:$couiVersion")
    implementation("io.github.itoseo:coui-icons:$couiVersion")

    // Crash logging with a custom Miuix crash screen.
    implementation("com.github.Bhuvaneshw:CrashHandler:1.0.4")

    // Renders the release-notes changelog as markdown (full GFM).
    implementation("com.mikepenz:multiplatform-markdown-renderer:0.46.0")
    implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.46.0")

    // Shizuku provides a shell-identity UserService without root. The provider
    // is declared in the manifest so the Shizuku manager can verify this app.
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")

    // Compose 1.12 is the version Miuix 0.9.4 is built against.
    implementation("androidx.compose.foundation:foundation:1.12.0")
    implementation("androidx.compose.ui:ui:1.12.0")
    implementation("androidx.compose.animation:animation:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
