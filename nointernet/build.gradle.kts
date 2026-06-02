plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.multiplatformLibrary)
    alias(libs.plugins.android.lint)

//    alias(libs.plugins.kotlin.compose)
//    alias(libs.plugins.jetbrainsCompose)

    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)
    signing
}

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "com.walhalla.nointernet"
        compileSdk {
            version = release(36) {
                minorApiLevel = 1
            }
        }
        minSdk = 24

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
    }

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "kmp:nointernetKit"

    iosX64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    jvm()

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.compose.material3) // This might be an older version, review needed if errors persist
                implementation(libs.compose.foundation)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.material.icons.extended)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
//                implementation(libs.androidx.core)
//                implementation(libs.androidx.junit)
//                implementation(libs.androidx.runner)
            }
        }

        iosMain {
            dependencies {
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP’s default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }
    }

}
compose.resources {
    packageOfResClass = "dev.walhalla.kmp.nointernet"
    publicResClass = true
}

mavenPublishing {
    configure(
        com.vanniktech.maven.publish.KotlinMultiplatform(
            javadocJar = com.vanniktech.maven.publish.JavadocJar.Dokka("dokkaHtml"),
            sourcesJar = true,
        )
    )
    coordinates(
        project.group.toString(),
        "nointernet",
        project.version.toString(),
    )
    pom {
        name = "nointernet"
        description = "Kotlin Multiplatform Compose UI for offline / no internet state"
        url = "https://github.com/ishumakov881/walhalla-kmp"
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://opensource.org/licenses/Apache-2.0")
            }
        }
        developers {
            developer {
                id.set("ishumakov881")
                name.set("ishumakov881")
                url.set("https://github.com/ishumakov881")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/ishumakov881/walhalla-kmp.git")
            developerConnection.set("scm:git:ssh://git@github.com/ishumakov881/walhalla-kmp.git")
            url.set("https://github.com/ishumakov881/walhalla-kmp")
        }
        issueManagement {
            system.set("Github")
            url.set("https://github.com/ishumakov881/walhalla-kmp/issues")
        }
    }

    publishToMavenCentral(automaticRelease = true)
    val isSigningRequired = project.findProperty("signing.required")?.toString()?.toBoolean() ?: true
    val hasSigningKeys =
        project.hasProperty("signingInMemoryKey") ||
            System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null ||
            project.hasProperty("signing.keyId") ||
            System.getenv("SIGNING_KEY_ID") != null

    if (isSigningRequired && hasSigningKeys) {
        signAllPublications()
    }
}

signing {
    val isSigningRequired = project.findProperty("signing.required")?.toString()?.toBoolean() ?: true
    val hasSigningKeys =
        project.hasProperty("signingInMemoryKey") ||
            System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null ||
            project.hasProperty("signing.keyId") ||
            System.getenv("SIGNING_KEY_ID") != null
    isRequired = isSigningRequired && hasSigningKeys
}