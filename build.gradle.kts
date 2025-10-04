plugins {
    id("com.android.application") version "8.3.2"
    id("org.jetbrains.kotlin.android") version "1.9.24"
}

android {
    namespace = "com.example.termuxultra"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.termuxultra"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    packagingOptions {
        pickFirst("**/libc++_shared.so")
        pickFirst("**/libbifrost.so")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.code.gson:gson:2.10.1")
    
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.0")
}

// Custom task to build Rust library before Android build
tasks.register<Exec>("buildRustLibrary") {
    workingDir = file("rust-core")
    commandLine("./build-android.sh")
    
    doFirst {
        println("Building Rust library for Android...")
    }
}

// Make Android build depend on Rust build
tasks.whenTaskAdded {
    if (name == "preBuild") {
        dependsOn("buildRustLibrary")
    }
}

// Clean additional directories
tasks.named("clean") {
    doLast {
        delete(file("rust-core/target"))
    }
}
