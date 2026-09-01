plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    api(project(":common"))
    api(project(":ast"))
    api(project(":report"))
    implementation(project(":language"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
