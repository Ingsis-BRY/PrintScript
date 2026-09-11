plugins {
    id("printscript.kotlin-module")
}

dependencies {
    implementation(project(":common"))
    api(project(":ast"))
    api(project(":report"))
    implementation(project(":language"))

    testImplementation(project(":lexer"))
    testImplementation(project(":parser"))
    testImplementation(project(":pipeline"))
}
