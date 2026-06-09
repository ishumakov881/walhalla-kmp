plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.multiplatformLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.mavenPublish) apply false

    //alias(libs.plugins.kmpnotifier) apply false // Apply KMPNotifier plugin

    // Apply only ONE of the following based on your needs:
    // For Firebase
    //-->>> alias(libs.plugins.google.gms.google.services) apply false

    alias(libs.plugins.android.lint) apply false
}

allprojects {
    group = "io.github.ishumakov881"
    version = project.properties["kmpLibsVersion"] as String
}

subprojects {
    val publishableModules = setOf(":device-kit", ":nointernet", ":sip")
    if (project.path !in publishableModules) return@subprojects

    apply(plugin = "org.jetbrains.dokka")
}