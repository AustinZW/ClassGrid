import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val releaseKeystoreFile = rootProject.file("keystore.properties")
val releaseKeystore = Properties().apply {
    if (releaseKeystoreFile.isFile) {
        releaseKeystoreFile.reader(Charsets.UTF_8).use { load(it) }
    }
}

val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    group = "verification"
    description = "Checks that local release signing credentials are configured."
    doLast {
        val required = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
        check(required.all { !releaseKeystore.getProperty(it).isNullOrBlank() }) {
            "正式版签名配置不完整，请在项目根目录配置 keystore.properties（参见 RELEASE-SIGNING.md）。"
        }
        check(rootProject.file(releaseKeystore.getProperty("storeFile")).isFile) {
            "正式版签名密钥库不存在，请检查 keystore.properties 中的 storeFile。"
        }
    }
}

android {
    namespace = "net.yeeren.classgrid"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.yeeren.classgrid"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testApplicationId = "net.yeeren.classgrid.androidtest"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            storeFile = releaseKeystore.getProperty("storeFile")
                ?.takeIf { it.isNotBlank() }?.let { rootProject.file(it) }
            storePassword = releaseKeystore.getProperty("storePassword")
            keyAlias = releaseKeystore.getProperty("keyAlias")
            keyPassword = releaseKeystore.getProperty("keyPassword")
        }
    }

    buildTypes {
        create("preview") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".test"
            matchingFallbacks += listOf("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

tasks.matching { it.name == "validateSigningRelease" }.configureEach {
    dependsOn(validateReleaseSigning)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }
