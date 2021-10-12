import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("edu.sc.seis.launch4j") version "2.5.1"
    id ("com.github.johnrengelman.shadow") version "7.1.0"
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.serialization") version "1.5.31"
    application
}

group = "arena.group"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

launch4j {
    jarTask = tasks["shadowJar"]
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    api("com.azure:azure-storage-blob:12.14.0")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}


tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }


}