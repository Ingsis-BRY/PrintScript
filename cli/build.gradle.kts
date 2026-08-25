plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":ast"))
    implementation(project(":pipeline"))
    implementation(project(":report"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
