import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.mavenPublish)
    signing
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }

    jvm("desktop")

    val xcfName = "sipKit"
    iosX64 { binaries.framework { baseName = xcfName } }
    iosArm64 { binaries.framework { baseName = xcfName } }
    iosSimulatorArm64 { binaries.framework { baseName = xcfName } }

    val iosNativeTargets = listOf(
        targets.getByName("iosArm64") as KotlinNativeTarget,
        targets.getByName("iosX64") as KotlinNativeTarget,
        targets.getByName("iosSimulatorArm64") as KotlinNativeTarget,
    )

    iosNativeTargets.forEach { target ->
        val konanTargetName = target.konanTarget.name
        val nativeLibDir = layout.buildDirectory.dir("native/ios/$konanTargetName")
        val buildTelephonyTask = tasks.register<Exec>("buildTelephony${target.name.replaceFirstChar(Char::uppercaseChar)}") {
            group = "native"
            description = "Build libtelephony.a for ${target.name}"
            onlyIf { OperatingSystem.current().isMacOsX }
            workingDir = file("native")
            commandLine(
                "bash",
                "build-ios.sh",
                konanTargetName,
                nativeLibDir.get().asFile.absolutePath,
            )
            inputs.dir(file("native"))
            outputs.file(nativeLibDir.map { it.file("libtelephony.a") })
        }

        target.compilations.getByName("main") {
            cinterops {
                val telephony by creating {
                    defFile(project.file("src/nativeInterop/cinterop/telephony.def"))
                    includeDirs(project.file("native"))
                }
            }
        }

        target.binaries.all {
            linkerOpts(
                "-L${nativeLibDir.get().asFile.absolutePath}",
                "-ltelephony",
                "-framework",
                "CoreAudio",
                "-framework",
                "AudioToolbox",
                "-framework",
                "AVFoundation",
                "-framework",
                "CoreFoundation",
                "-framework",
                "Security",
                "-framework",
                "SystemConfiguration",
            )
            linkTaskProvider.configure {
                dependsOn(buildTelephonyTask)
            }
        }
    }

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
