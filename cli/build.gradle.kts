plugins {
    id("printscript.kotlin-module")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":ast"))
    implementation(project(":report"))
}
