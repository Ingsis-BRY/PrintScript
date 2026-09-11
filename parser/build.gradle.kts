plugins {
    id("printscript.kotlin-module")
}

dependencies {
    implementation(project(":common"))
    api(project(":ast"))
    api(project(":token"))
    implementation(project(":language"))
    api(project(":report"))

    testImplementation(project(":lexer"))
}
