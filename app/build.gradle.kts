plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

dependencies {
    implementation(project(":ext"))
    compileOnly(libs.echo)
    compileOnly(libs.kotlin.stdlib)
}

val extType: String by project
val extId: String by project
val extClass: String by project

val extIconUrl: String? by project
val extName: String by project
val extDescription: String? by project

val extAuthor: String by project
val extAuthorUrl: String? by project

val extRepoUrl: String? by project
val extUpdateUrl: String? by project

val gitHash = execute("git", "rev-parse", "HEAD").take(7)
val gitCount = execute("git", "rev-list", "--count", "HEAD").toInt()
val verCode = gitCount
val verName = "v$gitHash"

val outputDir = file("${layout.buildDirectory.asFile.get()}/generated/proguard")
val generatedProguard = file("${outputDir}/generated-rules.pro")

tasks.register("generateProguardRules") {
    doLast {
        outputDir.mkdirs()
        generatedProguard.writeText(
            "-dontobfuscate\n-keep,allowoptimization class dev.brahmkshatriya.echo.extension.$extClass"
        )
    }
}

tasks.named("preBuild") {
    dependsOn("generateProguardRules")
}

tasks.register("uninstall") {
    android.run {
        execute(
            adbExecutable.absolutePath, "shell", "pm", "uninstall", defaultConfig.applicationId!!
        )
    }
}

android {
    namespace = "dev.brahmkshatriya.echo.extension"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.brahmkshatriya.echo.extension.$extId"
        minSdk = 24
        targetSdk = 35

        manifestPlaceholders.apply {
            put("type", "dev.brahmkshatriya.echo.${extType}")
            put("id", extId)
            put("class_path", "dev.brahmkshatriya.echo.extension.${extClass}")
            put("version", verName)
            put("version_code", verCode.toString())
            put("icon_url", extIconUrl ?: "")
            put("app_name", "Echo : $extName Extension")
            put("name", extName)
            put("description", extDescription ?: "")
            put("author", extAuthor)
            put("author_url", extAuthorUrl ?: "")
            put("repo_url", extRepoUrl ?: "")
            put("update_url", extUpdateUrl ?: "")
        }
    }

    buildTypes {
        all {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                generatedProguard.absolutePath,
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

fun execute(vararg command: String): String {
    val processBuilder = ProcessBuilder(*command)
    val hashCode = command.joinToString().hashCode().toString()
    val output = File.createTempFile(hashCode, "")
    processBuilder.redirectOutput(output)
    val process = processBuilder.start()
    process.waitFor()
    return output.readText().dropLast(1)
}
