plugins {
    id("printscript.coverage-aggregation")
}

tasks.register("installGitHook") {
    doLast {
        val source = rootProject.file("scripts/pre-commit")
        val target = rootProject.file(".git/hooks/pre-commit")

        target.parentFile.mkdirs()
        source.copyTo(target, overwrite = true)
        target.setExecutable(true)
    }
}
