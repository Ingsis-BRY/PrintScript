package com.printscript.app

import com.printscript.ast.Statement
import com.printscript.cli.Analyzing
import com.printscript.cli.Cli
import com.printscript.cli.Formatting
import com.printscript.cli.Program
import com.printscript.cli.ProgressPrinter
import com.printscript.cli.StatementSource
import com.printscript.formatter.Config
import com.printscript.formatter.ConfigError
import com.printscript.formatter.Formatter
import com.printscript.interpreter.Environment
import com.printscript.interpreter.Interpreter
import com.printscript.interpreter.OutputEmitter
import com.printscript.interpreter.ValueOps
import com.printscript.interpreter.executor.StatementExecutors
import com.printscript.lexer.Lexer
import com.printscript.lexer.StreamSourceReader
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.linter.Linter
import com.printscript.linter.config.LintConfig
import com.printscript.linter.config.identifier.IdentifierStyle
import com.printscript.linter.report.LintFinding
import com.printscript.parser.Parser
import com.printscript.parser.syntax.StatementSyntaxes
import com.printscript.pipeline.StatementStream
import com.printscript.pipeline.TokenSource
import com.printscript.report.ErrorRenderer
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.token.Token
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path

class PrintScript(
    private val output: OutputEmitter,
    private val formatted: Appendable,
    private val progress: Appendable,
    private val errors: Appendable,
    private val config: Path?,
) {
    fun cli(): Cli =
        Cli(
            newStatements = ::statementsIn,
            newProgram = ::newProgram,
            newFormatting = ::formattingIn,
            newAnalyzing = ::analyzingIn,
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
                    parser = Parser(StatementSyntaxes.DEFAULT)::parse,
                ),
        )
    }

    private fun newProgram(): Program {
        val interpreter = Interpreter(Environment(), output, ValueOps(), StatementExecutors.DEFAULT)

        return InterpreterProgram(interpreter)
    }

    private fun formattingIn(file: Path): Formatting {
        val settings = Config.read(configFile())
        val reader = Files.newBufferedReader(file)
        val lexer = Lexer(StreamSourceReader(reader), TokenRecognizers.DEFAULT)

        return StreamFormatting(
            reader = reader,
            formatter = Formatter(settings),
            tokens = LexerTokens(lexer),
            out = formatted,
        )
    }

    private fun analyzingIn(file: Path): Analyzing {
        val settings = LintConfig.read(configFile())

        return StreamAnalyzing(
            statements = statementsIn(file),
            linter = Linter(settings),
        )
    }

    private fun configFile(): Path =
        config ?: throw ConfigError("formatting needs a configuration file: pass --config <file>.")

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

private class StreamFormatting(
    private val reader: Reader,
    private val formatter: Formatter,
    private val tokens: TokenSource,
    private val out: Appendable,
) : Formatting {
    override fun format(): Result<Unit> = formatter.format(tokens.tokens(), out)

    override fun close() = reader.close()
}

private class StreamAnalyzing(
    private val statements: StatementSource,
    private val linter: Linter,
) : Analyzing {
    override fun analyze(): Result<Unit> {
        while (statements.hasNext()) {
            val result = statements.next()

            if (result is Success) {
                linter
                    .lint(listOf(result.value))
                    .findings
                    .forEach { println(formatFinding(it)) }
            }
        }

        return Success(Unit)
    }

    override fun close() {
        statements.close()
    }

    private fun formatFinding(finding: LintFinding): String {
        val location =
            "${finding.span.start.line}:${finding.span.start.column}"

        return "$location ${message(finding)}"
    }

    private fun message(finding: LintFinding): String =
        when (finding) {
            is LintFinding.InvalidIdentifier ->
                "Invalid identifier '${finding.name}': expected ${style(finding.expectedStyle)}."

            is LintFinding.InvalidPrintlnArgument ->
                "Invalid println argument: expected a variable or literal."

            is LintFinding.InvalidReadInputArgument ->
                "Invalid readInput argument: expected a variable or literal."
        }

    private fun style(style: IdentifierStyle): String =
        when (style) {
            IdentifierStyle.CAMEL_CASE -> "camel case"
            IdentifierStyle.SNAKE_CASE -> "snake case"
        }
}
