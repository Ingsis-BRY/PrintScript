plugins {
    // apply false makes the plugin available to subprojects without applying it to the root project
    kotlin("jvm") version "2.3.10" apply false
    id("dev.detekt") version "2.0.0-alpha.3" apply false
    base
    jacoco
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
    apply(plugin = "jacoco")

    extensions.configure<JacocoPluginExtension> {
        toolVersion = "0.8.15"
    }

    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        buildUponDefaultConfig.set(false)
        ignoreFailures.set(false)
    }
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

// entradas comunes al reporte y a la verificacion: los .exec, las fuentes y
// las clases compiladas de todos los submodulos
val executionFiles = files(
    subprojects.map { it.layout.buildDirectory.file("jacoco/test.exec") }
)
val sourceDirs = files(subprojects.map { it.file("src/main/kotlin") })
val classDirs = files(
    subprojects.map { it.layout.buildDirectory.dir("classes/kotlin/main") }
)

/**
* suma la cobertura de todos los submodulos en un unico reporte, para leerlo
* en HTML y para que CI lo consuma en XML
*/
val coverageReport by tasks.registering(JacocoReport::class) {
    dependsOn(subprojects.map { it.tasks.named("test") })

    executionData.setFrom(executionFiles.filter { it.exists() })
    sourceDirectories.setFrom(sourceDirs)
    classDirectories.setFrom(classDirs)

    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

/**
* barrera de calidad: mide el umbral sobre el total del proyecto, no modulo
* por modulo, para que un submodulo chico no frene el build por unas pocas
* lineas sin cubrir
*/
val coverageVerification by tasks.registering(JacocoCoverageVerification::class) {
    dependsOn(coverageReport)

    executionData.setFrom(executionFiles.filter { it.exists() })
    sourceDirectories.setFrom(sourceDirs)
    classDirectories.setFrom(classDirs)

    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(coverageVerification)
}
