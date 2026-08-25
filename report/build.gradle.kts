plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    api(project(":common"))
    api(project(":ast"))

    testImplementation(project(":lexer"))
    testImplementation(project(":parser"))
    testImplementation(project(":token"))
    testImplementation(project(":interpreter"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}