plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.houvven.guise.hook"
    // 同 app：compileSdk=37，可在编译期直接引用到 Android 16 / 17 的新常量和反射目标类签名，
    // 运行时再通过 SystemVersion.require() 做版本保护，两者不冲突。
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
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
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.majorVersion
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(libs.yuki.api)
    compileOnlyApi(libs.xposed.api)
    ksp(libs.yuki.ksp.xposed)

    implementation(libs.kotlin.serialization.json)
    implementation(libs.betterandroid.extension.system)
}