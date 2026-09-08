package com.printscript.app

import com.printscript.interpreter.ConsoleOutput
import picocli.CommandLine
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val command = PrintScriptCommand(ConsoleOutput(), System.out, System.err)

    val code =
        CommandLine(command)
            .setCaseInsensitiveEnumValuesAllowed(true)
            .execute(*args)

    System.out.flush()

    exitProcess(code)
}
