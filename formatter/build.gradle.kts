plugins {
    id("printscript.kotlin-module")
}

dependencies {
    api(project(":token"))
    api(project(":report"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    testImplementation(project(":lexer"))
}
