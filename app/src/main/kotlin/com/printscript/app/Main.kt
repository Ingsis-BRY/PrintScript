package com.printscript.app

import com.printscript.interpreter.ConsoleOutput
import picocli.CommandLine
import kotlin.system.exitProcess

/**
 * el entry point, y lo unico que no se puede ejercitar desde un test: termina el
 * proceso. todo lo demas vive en [PrintScriptCommand], que devuelve el codigo en
 * lugar de aplicarlo.
 */
fun main(args: Array<String>) {
    val command = PrintScriptCommand(ConsoleOutput(), System.err)

    exitProcess(
        CommandLine(command)
            .setCaseInsensitiveEnumValuesAllowed(true)
            .execute(*args),
    )
}
