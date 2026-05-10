import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.nanan.coc"
    compileSdk = 35
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.nanan.coc"
        minSdk = 26
        targetSdk = 35
        versionCode = 43
        versionName = "1.2.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release-key.jks")
            storePassword = "123456"
            keyAlias = "coc"
            keyPassword = "123456"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// 从 local.properties 读取验证密钥注入 BuildConfig
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}

android.buildTypes.all {
    val prefix = "AUTH_"
    for ((key, value) in localProps) {
        if (key.toString().startsWith(prefix)) {
            buildConfigField("String", key.toString(), "\"$value\"")
        }
    }
}

android.applicationVariants.all {
    val variant = this
    val capitalizeName = variant.name.replaceFirstChar { it.uppercase() }
    val renameTask = tasks.register("renameApk${capitalizeName}") {
        dependsOn("package${capitalizeName}")
        doLast {
            val dir = layout.buildDirectory.dir("outputs/apk/${variant.name}").get().asFile
            val from = dir.resolve("app-${variant.name}.apk")
            val to = dir.resolve("coc-layouts-${versionName}-${variant.name}.apk")
            if (from.exists()) {
                to.delete()
                from.renameTo(to)
            }
        }
    }
    tasks.named("assemble${capitalizeName}") {
        dependsOn(renameTask)
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON parsing
    implementation("org.json:json:20231013")

    // Coil (image loading)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // SwipeRefresh
    implementation("com.google.accompanist:accompanist-swiperefresh:0.34.0")
}
