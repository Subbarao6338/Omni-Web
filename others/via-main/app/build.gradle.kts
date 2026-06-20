import java.io.ByteArrayOutputStream
import java.util.Properties
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.nature.browser"
    compileSdk = 34

    val versionPropsFile = rootProject.file("version.properties")
    val versionProps = Properties()
    if (versionPropsFile.exists()) {
        FileInputStream(versionPropsFile).use { versionProps.load(it) }
    }

    val vMajor = versionProps.getProperty("VERSION_MAJOR")?.toInt() ?: 1
    val vMinor = versionProps.getProperty("VERSION_MINOR")?.toInt() ?: 0
    val vPatch = versionProps.getProperty("VERSION_PATCH")?.toInt() ?: 0
    val vOffset = versionProps.getProperty("VERSION_CODE_OFFSET")?.toInt() ?: 0

    val gitCommitCount = try {
        val stdout = ByteArrayOutputStream()
        project.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = stdout
        }
        stdout.toString().trim().toInt()
    } catch (e: Exception) {
        0
    }

    val calculatedVersionCode: Int = vOffset + gitCommitCount

    val calculatedVersionName = "$vMajor.$vMinor.$vPatch"

    defaultConfig {
        applicationId = "com.nature.browser"
        minSdk = 24
        targetSdk = 34
        versionCode = calculatedVersionCode
        versionName = calculatedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = project.findProperty("releaseKeyStore")?.let { file(it) }
            storePassword = project.findProperty("releaseKeyStorePassword")?.toString()
            keyAlias = project.findProperty("releaseKeyAlias")?.toString()
            keyPassword = project.findProperty("releaseKeyPassword")?.toString()
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.register("printVersion") {
    val vName = android.defaultConfig.versionName
    val vCode = android.defaultConfig.versionCode
    doLast {
        println("VERSION_NAME=$vName")
        println("VERSION_CODE=$vCode")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.webkit:webkit:1.11.0")

    // GeckoView
    implementation("org.mozilla.geckoview:geckoview-beta-arm64-v8a:122.0.20240112091933")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.fragment:fragment-ktx:1.7.1")

    // CameraX for QR Scanner
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Room
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
