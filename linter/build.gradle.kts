plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    api(project(":common"))
    api(project(":ast"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
