plugins {
    // apply false makes the plugin available to subprojects without applying it to the root project
    kotlin("jvm") version "2.3.10" apply false
    id("dev.detekt") version "2.0.0-alpha.3" apply false
    base
    jacoco
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
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
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

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

// :app queda fuera de la medicion: solo contiene el entrypoint, que cablea los
// modulos y termina el proceso con exitProcess. Invocarlo desde un test mataria
// la JVM del worker, asi que no es codigo que se pueda cubrir.
val coveredProjects = subprojects.filter { it.name != "app" }

// entradas comunes al reporte y a la verificacion: los .exec, las fuentes y
// las clases compiladas de los modulos medidos
val executionFiles = files(
    coveredProjects.map { it.layout.buildDirectory.file("jacoco/test.exec") }
)
val sourceDirs = files(coveredProjects.map { it.file("src/main/kotlin") })
val classDirs = files(
    coveredProjects.map { it.layout.buildDirectory.dir("classes/kotlin/main") }
)

/**
* suma la cobertura de todos los submodulos en un unico reporte, para leerlo
* en HTML y para que CI lo consuma en XML
*/
val coverageReport by tasks.registering(JacocoReport::class) {
    dependsOn(coveredProjects.map { it.tasks.named("test") })

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
        // las seis metricas que mide JaCoCo, todas contra el mismo umbral: asi
        // el numero es el mismo lo mire quien lo mire, sin depender de que
        // columna del reporte se lea. BRANCH es la que mas aporta: una linea
        // con `a || b` cuenta como cubierta aunque nunca se haya ejercitado la
        // segunda mitad, y solo esa metrica lo detecta.
        rule {
            element = "BUNDLE"

            listOf(
                "INSTRUCTION",
                "BRANCH",
                "LINE",
                "COMPLEXITY",
                "METHOD",
                "CLASS",
            ).forEach { metric ->
                limit {
                    counter = metric
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(coverageVerification)
}
