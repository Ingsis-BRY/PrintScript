plugins {
    id("printscript.kotlin-module")
}

dependencies {
    implementation(project(":common"))
    api(project(":ast"))
    api(project(":report"))
}
