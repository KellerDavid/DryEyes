import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    jvmToolchain(21)
    jvm("desktop")

    sourceSets {
        val desktopMain by getting
        val desktopTest by getting

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.material3)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
        desktopTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    add("kspDesktop", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

compose.resources {
    packageOfResClass = "com.eyecare.resources"
    publicResClass = false
}

compose.desktop {
    application {
        mainClass = "com.eyecare.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "DryEyes"
            packageVersion = "1.0.0"
            description = "Szemkímélő szünetek és szemtorna"
            vendor = "DryEyes"
            modules("java.sql", "jdk.unsupported")
            macOS { bundleID = "com.eyecare.dryeyes" }
            windows { menuGroup = "DryEyes"; upgradeUuid = "8f3c2a5e-6b1d-4f7e-9a2c-3d4e5f6a7b8c" }
            linux { packageName = "dryeyes" }
        }
    }
}
