import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

val extensionClass = "JellyfinExtension"
val id = "jellyfin"
val name = "Jellyfin"
val version = "1.0.0"
val description = "Jellyfin extension for Echo."
val author = "Secozzi"
val iconUrl: String = "https://i.imgur.com/VFPYs2e.png"

android {
    namespace = "dev.brahmkshatriya.echo.extension"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.brahmkshatriya.echo.extension.jellyfin"
        minSdk = 24
        targetSdk = 34

        versionCode = 1
        versionName = version

        resValue("string", "app_name", "Echo : $name Extension")
        resValue("string", "class_path", "$namespace.$extensionClass")
        resValue("string", "name", name)
        resValue("string", "id", id)
        resValue("string", "version", version)
        resValue("string", "description", description)
        resValue("string", "author", author)
        resValue("string", "icon_url", iconUrl)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests {
            this.isReturnDefaultValues = true
        }
    }
}

private val configFile = files("$rootDir/config/detekt/detekt.yml")
private val kotlinFiles = "**/*.kt"
private val resourceFiles = "**/resources/**"
private val buildFiles = "**/build/**"
private val generatedFiles = "**/generated/**"
private val scriptsFiles = "**/*.kts"

detekt {
    config.setFrom(configFile)
    buildUponDefaultConfig = true
    autoCorrect = false
    ignoreFailures = false
    allRules = false
}

tasks.withType<Detekt>().configureEach {
    include(kotlinFiles)
    exclude(resourceFiles, buildFiles, generatedFiles, scriptsFiles)
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
    }
}

dependencies {
    val libVersion = "38e1df03f6"
    compileOnly("com.github.brahmkshatriya:echo:$libVersion")

    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.github.brahmkshatriya:echo:$libVersion")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")
}