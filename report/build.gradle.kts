plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":ast"))

    testImplementation(project(":lexer"))
    testImplementation(project(":parser"))
    testImplementation(project(":token"))
    testImplementation(project(":interpreter"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}