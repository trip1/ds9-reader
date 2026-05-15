import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                // Arrow
                implementation(libs.arrow.core)
                implementation(libs.arrow.fx.coroutines)

                // SQLDelight runtime
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)

                // Voyager navigation
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.screenmodel)
                implementation(libs.voyager.transitions)

                // Readium core
                implementation(libs.readium.shared)
                implementation(libs.readium.streamer)

                // Koin DI
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                // Kotlinx
                implementation(libs.kotlinx.coroutines)
                implementation(libs.kotlinx.datetime)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.fragment.ktx)
                implementation(libs.sqldelight.android.driver)
                implementation(libs.readium.navigator)
                implementation(libs.koin.android)
                implementation(libs.koin.androidx.compose)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

        // iOS nativeMain — shared across all iOS targets
        val nativeMain by creating {
            dependsOn(commonMain)
        }

        val iosX64Main by getting { dependsOn(nativeMain) }
        val iosArm64Main by getting { dependsOn(nativeMain) }
        val iosSimulatorArm64Main by getting { dependsOn(nativeMain) }

        nativeMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "com.example.ds9reader"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.ds9reader"
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}

compose.desktop {
    application {
        mainClass = "com.example.ds9reader.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.example.ds9reader"
            packageVersion = "1.0.0"
        }
    }
}

sqldelight {
    databases {
        create("LibraryDatabase") {
            packageName.set("com.example.ds9reader.database")
        }
    }
}