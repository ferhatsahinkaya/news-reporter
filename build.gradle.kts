import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50" apply false
    `java-library`
}

subprojects {
    val buildVersion: String? by project
    version = buildVersion ?: "DEV-SNAPSHOT"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply<JavaLibraryPlugin>()

    repositories {
        mavenCentral()
    }

    val junitVersion = "5.3.2"
    dependencies {
        implementation(kotlin("stdlib-jdk8"))

        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    }

    tasks {
        test {
            useJUnitPlatform()
        }

        withType<KotlinCompile>().configureEach {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
}