plugins {
    id("printscript.kotlin-module")
    application
}

// so a relative path on the command line resolves from the repo root and not
// from this module directory
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
