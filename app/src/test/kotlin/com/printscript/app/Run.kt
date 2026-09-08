package com.printscript.app

import com.printscript.interpreter.CollectingOutput
import java.nio.file.Files
import java.nio.file.Path

internal fun sourceFile(source: String): Path = temporaryFile(source, ".ps")

internal fun configFile(settings: String): Path = temporaryFile(settings, ".json")

private fun temporaryFile(
    content: String,
    suffix: String,
): Path {
    val file = Files.createTempFile("printscript", suffix)
    file.toFile().deleteOnExit()
    Files.writeString(file, content)
    return file
}

internal class Run(
    val output: CollectingOutput = CollectingOutput(),
    val formatted: StringBuilder = StringBuilder(),
    val progress: StringBuilder = StringBuilder(),
    val errors: StringBuilder = StringBuilder(),
    config: Path? = null,
) {
    val cli = PrintScript(output, formatted, progress, errors, config).cli()
}
