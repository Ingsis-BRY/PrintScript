plugins {
    id("printscript.kotlin-module")
}

dependencies {
    api(project(":ast"))
    api(project(":token"))
    api(project(":report"))

    testImplementation(project(":lexer"))
    testImplementation(project(":parser"))
}
