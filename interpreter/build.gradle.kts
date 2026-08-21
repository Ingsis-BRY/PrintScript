plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":ast"))
    implementation(project(":report"))
    implementation(project(":language"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}