plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":cli"))
    implementation(project(":pipeline"))
    implementation(project(":lexer"))
    implementation(project(":parser"))
    implementation(project(":interpreter"))
    implementation(project(":report"))

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.printscript.app.MainKt")
    applicationName = "printscript"
}

// so a relative path on the command line resolves from the repo root and not
// from this module directory
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

tasks.test {
    useJUnitPlatform()
}
