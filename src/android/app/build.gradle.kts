import java.io.IOException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

fun String.runCommand(workingDir: File = File(".")): String? {
    try {
        val proc = ProcessBuilder(*trim().split("\\s".toRegex()).toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        proc.waitFor(1, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
}

fun getGitHash(): String? = "git log --format=%h -1".runCommand()?.trim()

val versionMajor: String? = System.getenv("EMULATOR_VERSION_MAJOR")
val versionMinor: String? = System.getenv("EMULATOR_VERSION_MINOR")

fun getVersionName(): String {
    if (versionMajor != null && versionMinor != null)
        return "$versionMajor.$versionMinor"
    return getGitHash() ?: "1.0"
}

fun getVersionCode(): Int = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1

android {
    namespace = "info.cemu.cemu"
    compileSdk = 35
    ndkVersion = "26.3.11579264"
    defaultConfig {
        applicationId = "info.cemu.cemu"
        minSdk = 30
        targetSdk = 35
        versionCode = getVersionCode()
        versionName = getVersionName()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        jniLibs.useLegacyPackaging = true
    }
    val keystoreFilePath: String? = System.getenv("ANDROID_KEYSTORE_FILE")
    signingConfigs {
        if (keystoreFilePath != null) {
            create("releaseSigningConfig") {
                storeFile = file(keystoreFilePath)
                storePassword = System.getenv("ANDROID_KEY_STORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_STORE_PASSWORD")
            }
        }
    }
    androidComponents {
        beforeVariants { variantBuilder ->
            if (variantBuilder.name == "release") {
                variantBuilder.enable = false
            }
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        create("githubRelease") {
            if (keystoreFilePath != null) {
                initWith(getByName("release"))
                isMinifyEnabled = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                signingConfig = signingConfigs.getByName("releaseSigningConfig")
            } else {
                initWith(getByName("debug"))
                externalNativeBuild {
                    cmake {
                        arguments.add("-DCMAKE_BUILD_TYPE=DEBUG")
                    }
                }
            }
        }
        create("nokeyRelease") {
            initWith(getByName("debug"))
            applicationIdSuffix = ""
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_21)
        targetCompatibility(JavaVersion.VERSION_21)
    }
    externalNativeBuild {
        cmake {
            version = "3.22.1"
            path = file("../../../CMakeLists.txt")
        }
    }
    val versionArguments = if (versionMajor != null && versionMinor != null)
        arrayOf(
            "-DEMULATOR_VERSION_MAJOR=$versionMajor",
            "-DEMULATOR_VERSION_MINOR=$versionMinor"
        ) else emptyArray()
    val cmakeArguments = arrayOf(
        "-DANDROID_STL=c++_shared",
        "-DENABLE_VCPKG=ON",
        "-DVCPKG_TARGET_ANDROID=ON",
        "-DENABLE_SDL=OFF",
        "-DENABLE_WXWIDGETS=OFF",
        "-DENABLE_OPENGL=OFF",
        "-DENABLE_BLUEZ=OFF",
        "-DBUNDLE_SPEEX=ON",
        "-DENABLE_DISCORD_RPC=OFF",
        "-DENABLE_NSYSHID_LIBUSB=OFF",
        "-DENABLE_WAYLAND=OFF",
        "-DENABLE_HIDAPI=OFF"
    ) + versionArguments
    defaultConfig {
        externalNativeBuild {
            cmake {
                arguments.addAll(cmakeArguments)
                abiFilters("arm64-v8a")
            }
        }
    }
    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
        compose = true
    }
    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.okhttp)
    implementation(libs.okhttp.coroutines)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
