import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.JavaExec

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)

    //alias(libs.plugins.kmpnotifier) // Apply KMPNotifier plugin

    // Apply only ONE of the following based on your needs:
    // For Firebase
    //-- alias(libs.plugins.google.gms.google.services)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    //doc: https://kumar-dev.medium.com/fix-your-ios-simulator-on-intel-macs-for-kmp-kmm-projects-%EF%B8%8F-25c93996daea


    listOf(
        iosArm64(),
        iosSimulatorArm64(),

        //iosArm32(),
        iosX64(),// This is the key line to ensure Intel compatibility

    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            export("io.github.ishumakov881:kmpnotifier:1.6.7")
        }
    }

    jvm()
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.webkit)
            implementation(libs.ktor.client.okhttp)

            implementation(libs.koin.android)
            implementation(libs.firebase.messaging)

            api(libs.permissions)
            api(libs.permissions.compose)
            implementation(libs.permissions.notifications)
            implementation(libs.permissions.storage)
        }
        iosMain.dependencies {
            api(libs.permissions)
            api(libs.permissions.compose)
            implementation(libs.permissions.notifications)
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            // Compose Multiplatform: через compose.* — иначе IDE часто не видит
            // androidx.compose.runtime.* в commonMain при смеси с androidx compose-bom.
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.material.icons.extended)
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            implementation("be.digitalia.compose.htmlconverter:htmlconverter:1.1.1")


            //api(libs.kmpnotifier)
            api("io.github.ishumakov881:kmpnotifier:1.6.7")

            api("io.github.kevinnzou:compose-webview-multiplatform:2.0.3")
            implementation("com.chrynan.uri:uri-core:0.4.0")
            api("network.chaintech:cmptoast:1.0.8")
//            api("io.github.kevinnzou:compose-webview-multiplatform:2.0.3") {
//                exclude(group = "androidx.navigationevent", module = "navigationevent-iossimulatorarm64")
//            }
//            api("io.github.kevinnzou:compose-webview-multiplatform:2.0.3") {
//                exclude(group = "androidx.navigationevent", module = "navigationevent-iossimulatorarm64")
//            }

            // Source: https://mvnrepository.com/artifact/androidx.navigationevent/navigationevent-iossimulatorarm64
            //implementation("androidx.navigationevent:navigationevent-iossimulatorarm64:1.1.1")

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeVM)

            api("com.russhwolf:multiplatform-settings-no-arg:1.3.0")
            api("com.russhwolf:multiplatform-settings-coroutines:1.3.0")
            // In commonMain or androidMain dependencies
            api("com.russhwolf:multiplatform-settings:1.3.0")
            implementation(libs.ktor.client.core)

            implementation("network.chaintech:cmptoast:1.0.8")
            implementation(libs.kotlinx.datetime)

            implementation(project(":device-kit"))
            implementation(project(":nointernet"))


        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.cio)
            // KCEF / JCEF для Desktop WebView (см. README.desktop.md compose-webview)
            implementation("org.jogamp.jogl:jogl-all:2.6.0")
        }
    }
}

android {
    namespace = "net.example.online"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        //applicationId = "dev.walhalla.online"
        applicationId = "dev.walhalla.online.webview"

        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "dev.walhalla.online.MainKt"
        // Skiko: до инициализации GPU (см. main.kt). Снимает «Failed to create DirectX12 device» на части ПК/RDP.
        jvmArgs += listOf("-Dskiko.renderApi=OPENGL")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "dev.walhalla.online"
            packageVersion = "1.0.0"
        }
    }
}

// KCEF: обязательные --add-opens для Desktop WebView
// https://github.com/KevinnZou/compose-webview-multiplatform/blob/main/README.desktop.md
afterEvaluate {
    tasks.withType<JavaExec>().configureEach {
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }
}