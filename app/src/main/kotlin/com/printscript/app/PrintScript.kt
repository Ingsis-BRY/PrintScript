package com.printscript.app

import com.printscript.ast.Statement
import com.printscript.cli.Cli
import com.printscript.cli.Program
import com.printscript.cli.ProgressPrinter
import com.printscript.cli.StatementSource
import com.printscript.interpreter.Environment
import com.printscript.interpreter.Interpreter
import com.printscript.interpreter.OutputEmitter
import com.printscript.interpreter.ValueOps
import com.printscript.lexer.Lexer
import com.printscript.lexer.StreamSourceReader
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.parser.Parser
import com.printscript.pipeline.StatementParser
import com.printscript.pipeline.StatementStream
import com.printscript.pipeline.TokenSource
import com.printscript.report.ErrorRenderer
import com.printscript.report.Result
import com.printscript.token.Token
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path

class PrintScript(
    private val output: OutputEmitter,
    private val progress: Appendable,
    private val errors: Appendable,
) {
    fun cli(): Cli =
        Cli(
            newStatements = ::statementsIn,
            newProgram = ::newProgram,
            renderer = ErrorRenderer(),
            progress = ProgressPrinter(progress),
            errors = errors,
        )

    private fun statementsIn(file: Path): StatementSource {
        val reader = Files.newBufferedReader(file)
        val lexer = Lexer(StreamSourceReader(reader), TokenRecognizers.DEFAULT)

        return StreamStatementSource(
            reader = reader,
            stream =
                StatementStream(
                    source = LexerTokens(lexer),
                    parser = ParserStatements,
                ),
        )
    }

    private fun newProgram(): Program {
        val interpreter = Interpreter(Environment(), output, ValueOps())

        return InterpreterProgram(interpreter)
    }

    companion object {
        const val DEFAULT_VERSION: String = "1.0"

        fun supports(version: String): Boolean = version == DEFAULT_VERSION
    }
}

private class LexerTokens(
    private val lexer: Lexer,
) : TokenSource {
    override fun tokens(): Sequence<Result<Token>> = lexer.tokens()
}

private object ParserStatements : StatementParser {
    override fun parse(tokens: List<Token>): Result<Statement> = Parser.parse(tokens)
}

private class InterpreterProgram(
    private val interpreter: Interpreter,
) : Program {
    override fun execute(statement: Statement): Result<Unit> = interpreter.execute(statement)
}

private class StreamStatementSource(
    private val reader: Reader,
    private val stream: StatementStream,
) : StatementSource {
    override fun hasNext(): Boolean = stream.hasNext()

    override fun next(): Result<Statement> = stream.next()

    override fun close() = reader.close()
}
