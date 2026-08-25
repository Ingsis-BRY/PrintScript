plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":ast"))
    implementation(project(":token"))
    implementation(project(":lexer"))
    implementation(project(":parser"))
    implementation(project(":report"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}