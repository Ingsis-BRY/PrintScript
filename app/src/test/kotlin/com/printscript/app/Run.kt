package com.printscript.app

import com.printscript.interpreter.CollectingOutput
import java.nio.file.Files
import java.nio.file.Path

internal fun sourceFile(source: String): Path {
    val file = Files.createTempFile("printscript", ".ps")
    file.toFile().deleteOnExit()
    Files.writeString(file, source)
    return file
}

internal class Run(
    val output: CollectingOutput = CollectingOutput(),
    val progress: StringBuilder = StringBuilder(),
    val errors: StringBuilder = StringBuilder(),
) {
    val cli = PrintScript(output, progress, errors).cli()
}
