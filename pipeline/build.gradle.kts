plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    api(project(":ast"))
    api(project(":token"))
    api(project(":report"))

    testImplementation(project(":lexer"))
    testImplementation(project(":parser"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
