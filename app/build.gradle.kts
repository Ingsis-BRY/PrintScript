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
}

tasks.test {
    useJUnitPlatform()
}
