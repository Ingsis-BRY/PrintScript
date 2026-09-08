plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":cli"))
    implementation(project(":formatter"))
    implementation(project(":pipeline"))
    implementation(project(":lexer"))
    implementation(project(":linter"))
    implementation(project(":parser"))
    implementation(project(":interpreter"))
    implementation(project(":report"))

    // parsea argv y nada mas: no arma ningun grafo de objetos
    implementation("info.picocli:picocli:4.7.7")

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
