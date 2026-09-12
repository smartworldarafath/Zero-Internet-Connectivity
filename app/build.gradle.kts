plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.zeronetwork.connectivity"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.zeronetwork.connectivity"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            initWith(getByName("debug"))
        }
    }

    buildTypes {
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
        buildConfig = true
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.google.gson)
    implementation(libs.coil.compose)
    implementation(libs.zxing.core)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.register("copyApkToOutput") {
    dependsOn("assembleRelease")
    doLast {
        val destDir = file("C:/Users/HP Omnibook X Flip/Downloads/Generated APK/Zero Network Connectivity")
        destDir.mkdirs()
        var copied = false
        val bDir = layout.buildDirectory.get().asFile
        bDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "apk") {
                val target = File(destDir, "ZeroNetworkConnectivity-v1.0.3.apk")
                file.copyTo(target, overwrite = true)
                println("SUCCESS: Copied APK from ${file.absolutePath} to ${target.absolutePath} (${target.length()} bytes)")
                copied = true
            }
        }
        if (!copied) {
            rootProject.projectDir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "apk") {
                    val target = File(destDir, "ZeroNetworkConnectivity-v1.0.3.apk")
                    file.copyTo(target, overwrite = true)
                    println("SUCCESS: Copied APK from ${file.absolutePath} to ${target.absolutePath} (${target.length()} bytes)")
                    copied = true
                }
            }
        }
    }
}
