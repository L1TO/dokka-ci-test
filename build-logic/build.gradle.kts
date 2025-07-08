import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinPluginVersion: String by project
val agpVersion: String by project
val dokkaVersion: String by project

plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinPluginVersion")
    compileOnly("com.android.tools.build:gradle:$agpVersion")
    compileOnly("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-base:$dokkaVersion")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
}