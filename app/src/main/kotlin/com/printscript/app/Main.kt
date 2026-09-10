package com.printscript.app

import com.printscript.interpreter.ConsoleInput
import com.printscript.interpreter.ConsoleOutput
import com.printscript.interpreter.SystemEnvironment
import picocli.CommandLine
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val command =
        PrintScriptCommand(
            output = ConsoleOutput(),
            input = ConsoleInput,
            environment = SystemEnvironment,
            out = System.out,
            errors = System.err,
        )

    val code =
        CommandLine(command)
            .setCaseInsensitiveEnumValuesAllowed(true)
            .execute(*args)

    System.out.flush()

    exitProcess(code)
}
