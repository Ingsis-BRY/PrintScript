package com.printscript.app

import com.printscript.interpreter.CollectingOutput
import java.nio.file.Files
import java.nio.file.Path

/**
 * escribe la fuente en un archivo temporal, porque el CLI recibe un Path
 */
internal fun sourceFile(source: String): Path {
    val file = Files.createTempFile("printscript", ".ps")
    file.toFile().deleteOnExit()
    Files.writeString(file, source)
    return file
}

/**
 * los tres lugares donde una corrida escribe, juntos para que un test pueda
 * mirar cualquiera de ellos.
 *
 * arma el grafo real a traves de [PrintScript] y reemplaza unicamente los sinks:
 * un test end to end tiene que ejercitar el cableado que se ejecuta en
 * produccion, y si lo copiara podria quedar verde contra un cableado que Main ya
 * no usa.
 */
internal class Run(
    val output: CollectingOutput = CollectingOutput(),
    val progress: StringBuilder = StringBuilder(),
    val errors: StringBuilder = StringBuilder(),
) {
    val cli = PrintScript(output, progress, errors).cli()
}
