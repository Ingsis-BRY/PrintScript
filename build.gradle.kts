plugins {
    // apply false makes the plugin available to subprojects without applying it to the root project
    kotlin("jvm") version "2.3.10" apply false
    id("dev.detekt") version "2.0.0-alpha.3" apply false
}

group = "org.example"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    // Apply and configure Detekt consistently across all subprojects
    apply(plugin = "dev.detekt")

    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        buildUponDefaultConfig.set(false)
        ignoreFailures.set(false)
    }

    tasks.register<Copy>("installGitHook") {
        from(layout.projectDirectory.file("scripts/pre-commit"))
        into(layout.projectDirectory.dir(".git/hooks"))
        rename { "pre-commit" }

        doLast {
            val hook = layout.projectDirectory
                .file(".git/hooks/pre-commit")
                .asFile

            hook.setExecutable(true)
        }
    }
}