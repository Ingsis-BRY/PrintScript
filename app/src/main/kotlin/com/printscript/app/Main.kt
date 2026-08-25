package com.printscript.app

import com.printscript.cli.Cli
import com.printscript.cli.Operation
import com.printscript.cli.Program
import com.printscript.cli.ProgressPrinter
import com.printscript.interpreter.ConsoleOutput
import com.printscript.interpreter.Environment
import com.printscript.interpreter.Interpreter
import com.printscript.interpreter.ValueOps
import com.printscript.lexer.Lexer
import com.printscript.lexer.StreamSourceReader
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.parser.Parser
import com.printscript.pipeline.StatementParser
import com.printscript.pipeline.StatementStream
import com.printscript.pipeline.TokenSource
import com.printscript.report.ErrorRenderer
import com.printscript.report.Failure
import java.io.IOException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val usage = "usage: printscript <validation|execution> <file> [version]"

    if (args.size !in 2..3) {
        System.err.println(usage)
        exitProcess(2)
    }

    val operation = when (args[0].lowercase()) {
        "validation" -> Operation.VALIDATION
        "execution" -> Operation.EXECUTION
        else -> {
            System.err.println(usage)
            exitProcess(2)
        }
    }

    val cli = Cli(
        newStream = { reader ->
            val lexer = Lexer(StreamSourceReader(reader), TokenRecognizers.DEFAULT)

            StatementStream(
                source = TokenSource(lexer::tokens),
                parser = StatementParser(Parser::parse)
            )
        },
        newProgram = {
            val interpreter = Interpreter(Environment(), ConsoleOutput(), ValueOps())

            Program(interpreter::execute)
        },
        renderer = ErrorRenderer(),
        progress = ProgressPrinter(System.err),
        errors = System.err
    )

    val result = try {
        cli.run(operation, Path.of(args[1]), version = args.getOrNull(2))
    } catch (error: IllegalArgumentException) {
        System.err.println(error.message)
        exitProcess(2)
    } catch (error: NoSuchFileException) {
        System.err.println("cannot read ${args[1]}: no such file")
        exitProcess(2)
    } catch (error: IOException) {
        System.err.println("cannot read ${args[1]}: ${error.message ?: "I/O error"}")
        exitProcess(2)
    }

    exitProcess(if (result is Failure) 1 else 0)
}
