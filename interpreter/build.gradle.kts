plugins {
    id("printscript.kotlin-module")
}

dependencies {
    api(project(":common"))
    api(project(":ast"))
    api(project(":report"))
    implementation(project(":language"))
}
