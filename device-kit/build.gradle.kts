plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.multiplatformLibrary)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.mavenPublish)
    signing
}

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "dev.walhalla.kmp.device"
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
    }

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "device-kitKit"

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
                // Add KMP dependencies here
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
                implementation(libs.androidx.core)
                implementation(libs.androidx.runner)
                implementation(libs.androidx.testExt.junit)
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

        jvmMain {
            dependencies {
            }
        }
    }

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
        "device-kit",
        project.version.toString(),
    )
    pom {
        name = "device-kit"
        description = "Kotlin Multiplatform device and installation identifiers for Android, iOS, and JVM"
        url = "https://github.com/ishumakov881/KMP"
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
            connection.set("scm:git:git://github.com/ishumakov881/KMP.git")
            developerConnection.set("scm:git:ssh://git@github.com/ishumakov881/KMP.git")
            url.set("https://github.com/ishumakov881/KMP")
        }
        issueManagement {
            system.set("Github")
            url.set("https://github.com/ishumakov881/KMP/issues")
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