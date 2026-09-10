package com.printscript.app

import com.printscript.interpreter.EnvironmentSource
import com.printscript.interpreter.InputProvider
import com.printscript.interpreter.executor.StatementExecutor
import com.printscript.interpreter.executor.StatementExecutors
import com.printscript.interpreter.function.ValueFunction
import com.printscript.interpreter.function.ValueFunctions
import com.printscript.lexer.recognizer.TokenRecognizer
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.parser.expression.PrefixParselet
import com.printscript.parser.expression.PrefixParselets
import com.printscript.parser.syntax.StatementSyntax
import com.printscript.parser.syntax.StatementSyntaxes
import com.printscript.pipeline.StatementBoundaries
import com.printscript.pipeline.StatementBoundary

class Dialect(
    val version: String,
    val recognizers: List<TokenRecognizer>,
    val syntaxes: List<StatementSyntax>,
    val parselets: List<PrefixParselet>,
    val executors: List<StatementExecutor>,
    val boundary: StatementBoundary,
    val functions: (InputProvider, EnvironmentSource) -> Map<String, ValueFunction>,
) {
    companion object {
        const val DEFAULT_VERSION: String = "1.0"

        private val V1_0 =
            Dialect(
                version = "1.0",
                recognizers = TokenRecognizers.V1_0,
                syntaxes = StatementSyntaxes.V1_0,
                parselets = PrefixParselets.V1_0,
                executors = StatementExecutors.V1_0,
                boundary = StatementBoundaries.V1_0,
                functions = { _, _ -> ValueFunctions.NONE },
            )

        private val V1_1 =
            Dialect(
                version = "1.1",
                recognizers = TokenRecognizers.V1_1,
                syntaxes = StatementSyntaxes.V1_1,
                parselets = PrefixParselets.V1_1,
                executors = StatementExecutors.V1_1,
                boundary = StatementBoundaries.V1_1,
                functions = ValueFunctions::readers,
            )

        private val ALL = listOf(V1_0, V1_1)

        fun of(version: String): Dialect? = ALL.firstOrNull { it.version == version }

        fun versions(): List<String> = ALL.map { it.version }
    }
}
