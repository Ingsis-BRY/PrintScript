plugins {
    base
    jacoco
}

repositories {
    mavenCentral()
}

// todos los submodulos entran en la medicion, :app incluido. lo unico que queda
// afuera es MainKt: termina el proceso con exitProcess, asi que invocarlo desde
// un test mataria la JVM del worker. el cableado ya no vive ahi - vive en
// PrintScript, que es una clase comun y la ejercitan los tests end to end.
val coveredProjects = subprojects

// entradas comunes al reporte y a la verificacion: los .exec, las fuentes y
// las clases compiladas de los modulos medidos
val executionFiles = files(
    coveredProjects.map { it.layout.buildDirectory.file("jacoco/test.exec") }
)
val sourceDirs = files(coveredProjects.map { it.file("src/main/kotlin") })
val classDirs = files(
    coveredProjects.map { it.layout.buildDirectory.dir("classes/kotlin/main") }
).asFileTree.matching { exclude("**/com/printscript/app/MainKt*") }

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
