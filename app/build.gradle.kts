plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// 릴리즈 서명 자격 — 환경변수(CI) 또는 ~/.gradle/gradle.properties(로컬)에서 읽는다.
// 저장소에는 키도 비밀번호도 커밋하지 않는다. 값이 없으면 release 빌드는
// 서명되지 않은 채로 만들어지고, 그 사실이 빌드 로그에 찍힌다.
// 빈 문자열은 "없음"으로 취급한다. CI가 시크릿 없이 env를 넘기면 getenv는
// null이 아니라 ""를 돌려주고, 그대로 file("")을 부르면 설정 단계에서 죽는다.
fun secret(env: String, prop: String): String? =
    (System.getenv(env) ?: project.findProperty(prop) as String?)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

val releaseStorePath = secret("RELEASE_KEYSTORE_PATH", "stepupReleaseKeystorePath")
val releaseStorePassword = secret("RELEASE_KEYSTORE_PASSWORD", "stepupReleaseKeystorePassword")
val releaseKeyAlias = secret("RELEASE_KEY_ALIAS", "stepupReleaseKeyAlias")
val releaseKeyPassword = secret("RELEASE_KEY_PASSWORD", "stepupReleaseKeyPassword")
val releaseSigningReady =
    releaseStorePath != null &&
        releaseStorePassword != null &&
        releaseKeyAlias != null &&
        releaseKeyPassword != null &&
        file(releaseStorePath).exists()

android {
    namespace = "com.giwa.strideup"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.giwa.strideup"
        minSdk = 26
        targetSdk = 35
        versionCode = 23
        versionName = "1.15.1"
    }

    signingConfigs {
        // CI 러너는 매번 새로 생성되므로 AGP가 자동 생성하는 debug 키스토어는
        // 빌드마다 서명이 달라진다. 그러면 이전에 설치한 앱 위에 덮어쓸 때
        // INSTALL_FAILED_UPDATE_INCOMPATIBLE로 설치가 거부된다.
        // 저장소에 고정 키스토어를 두어 모든 빌드가 같은 서명을 갖게 한다.
        // (표준 Android debug 키와 동일한 성격의 공개 키 — 배포용 서명이 아니다.)
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            // 사이드로딩 호환성을 위해 v1(JAR) 서명도 함께 넣는다.
            enableV1Signing = true
            enableV2Signing = true
        }

        // Play Console 업로드용. 자격이 없으면 아예 만들지 않는다 — 빈 값으로
        // 만들어 두면 AGP가 서명 단계에서 알기 어려운 오류를 낸다.
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(releaseStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            // R8을 지금부터 켜 둔다. 출시 직전에 켜면 그때 처음 보는 난독화
            // 문제를 출시 압박 속에서 고치게 된다.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    // AAB에는 언어 리소스를 전부 담되, 기기가 받을 때만 골라 받게 한다.
    // (4개 언어를 지원하므로 language 스플릿을 끄면 APK가 불필요하게 커진다.)
    bundle {
        language { enableSplit = false } // 앱 내 언어 설정이 있으므로 분리하면 안 된다
        density { enableSplit = true }
        abi { enableSplit = true }
    }
}

// 릴리즈 서명 자격이 없는 채로 release 빌드가 돌면 로그에 분명히 남긴다.
tasks.matching {
    it.name.endsWith("Release") && (it.name.startsWith("assemble") || it.name.startsWith("bundle"))
}
    .configureEach {
        doFirst {
            if (!releaseSigningReady) {
                logger.lifecycle(
                    "⚠️  릴리즈 서명 자격이 없어 서명하지 않고 빌드합니다. " +
                        "Play Console 업로드에는 쓸 수 없습니다. " +
                        "설정 방법: docs/RELEASE-SIGNING.md",
                )
            }
        }
    }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
