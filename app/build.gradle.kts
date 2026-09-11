plugins {
    id("printscript.kotlin-application")
}

dependencies {
    implementation(project(":checker"))
    implementation(project(":cli"))
    implementation(project(":formatter"))
    implementation(project(":pipeline"))
    implementation(project(":lexer"))
    implementation(project(":linter"))
    implementation(project(":parser"))
    implementation(project(":interpreter"))
    implementation(project(":report"))

    // parsea argv y nada mas: no arma ningun grafo de objetos
    implementation("info.picocli:picocli:4.7.7")
}

application {
    mainClass.set("com.printscript.app.MainKt")
    applicationName = "printscript"
}
