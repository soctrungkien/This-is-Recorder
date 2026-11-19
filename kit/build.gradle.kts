plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.zeroui.kit"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 29

        // Both design languages stay in the build; the recorder switches
        // between them through its own UiStyle preference.
        buildConfigField("boolean", "LOCK_MIUIX", "false")
        buildConfigField("boolean", "LOCK_MATERIAL", "false")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // Keep the Compose compiler aligned with the app module.
    add("kotlinCompilerClasspath", "org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.21")

    api("androidx.compose.ui:ui:1.12.0")
    api("androidx.compose.ui:ui-graphics:1.12.0")
    api("androidx.compose.foundation:foundation:1.12.0")
    api("androidx.compose.animation:animation:1.12.0")
    api("androidx.compose.material3:material3:1.5.0-alpha29")

    api("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    api("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4")
    api("top.yukonga.miuix.kmp:miuix-nav-android:0.9.4")
    api("androidx.navigationevent:navigationevent-compose-android:1.1.2")

    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.4")
    implementation("com.materialkolor:material-color-utilities:5.0.1")
    implementation("io.github.kyant0:backdrop:2.0.0")
    implementation("io.github.kyant0:shapes:1.2.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
