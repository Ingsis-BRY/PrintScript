plugins {
    id("printscript.kotlin-module")
}

dependencies {
    implementation(project(":common"))
    api(project(":token"))
    api(project(":report"))
}
