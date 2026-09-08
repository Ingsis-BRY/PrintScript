plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    api(project(":common"))
    api(project(":ast"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
