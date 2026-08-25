plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":common"))
    api(project(":ast"))
    api(project(":report"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}