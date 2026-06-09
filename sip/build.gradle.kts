plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.mavenPublish)
    signing
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }

    jvm("desktop")

    val xcfName = "sipKit"
    iosX64 { binaries.framework { baseName = xcfName } }
    iosArm64 { binaries.framework { baseName = xcfName } }
    iosSimulatorArm64 { binaries.framework { baseName = xcfName } }

    sourceSets {
        val desktopMain by getting

        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.localbroadcastmanager)
            }
        }

        iosMain.dependencies {}
        desktopMain.dependencies {}
    }
}

android {
    namespace = "net.lds.sip"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        externalNativeBuild {
            cmake {
                cppFlags("")
                arguments("-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("native/CMakeLists.txt")
            version = "3.25.0+"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
        "sip",
        project.version.toString(),
    )
    pom {
        name = "sip"
        description = "Kotlin Multiplatform SIP engine (baresip) for Android, iOS, and JVM"
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
