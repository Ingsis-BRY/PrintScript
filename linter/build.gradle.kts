plugins {
    id("printscript.kotlin-module")
}

dependencies {
    api(project(":common"))
    api(project(":ast"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}
