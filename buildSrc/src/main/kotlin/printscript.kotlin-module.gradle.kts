plugins {
    kotlin("jvm")
    id("dev.detekt")
    id("org.jlleitschuh.gradle.ktlint")
    jacoco
    `maven-publish`
}

group = "com.printscript"
version = (findProperty("releaseVersion") as String? ?: "1.0-SNAPSHOT").removePrefix("v")

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig.set(false)
    ignoreFailures.set(false)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Ingsis-BRY/PrintScript")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
}
