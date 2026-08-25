plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":ast"))
    implementation(project(":lexer"))
    implementation(project(":pipeline"))
    implementation(project(":report"))
    implementation(project(":interpreter"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}