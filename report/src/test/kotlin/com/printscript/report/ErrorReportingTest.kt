package com.printscript.report

import com.printscript.interpreter.CollectingOutput
import com.printscript.interpreter.Environment
import com.printscript.interpreter.Interpreter
import com.printscript.interpreter.ValueOps
import com.printscript.lexer.Lexer
import com.printscript.lexer.StringSourceReader
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.parser.Parser
import com.printscript.token.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Reads a broken statement end to end and checks the line the user gets.
 *
 * These are the only tests that tie a piece of source to a finished message:
 * every component reports its failure as a [Diagnostic] case, and the
 * span it carries has to survive the trip to [ErrorRenderer] intact.
 */
class ErrorReportingTest {
    private val renderer = ErrorRenderer()

    @Test
    fun `a character no token can begin with is blamed on itself`() {
        assertEquals("(1:9)-(1:9) Unexpected character '@'.", reportOf("let x = @;"))
    }

    @Test
    fun `an unterminated literal is blamed over the whole attempt`() {
        assertEquals(
            "(1:17)-(1:20) Unterminated string literal.",
            reportOf("let x: string = \"abc"),
        )
    }

    @Test
    fun `a missing semicolon is blamed where the tokens ran out`() {
        assertEquals(
            "(1:18)-(1:18) Expected ';' at the end of the statement.",
            reportOf("let x: number = 42"),
        )
    }

    @Test
    fun `a word that is not a type name is blamed over the whole word`() {
        // the lexer only knows the type names the language has, so anything
        // else arrives as an identifier and the parser rejects it as a type
        assertEquals(
            "(1:8)-(1:14) Expected a type.",
            reportOf("let x: boolean = 42;"),
        )
    }

    @Test
    fun `a variable that was never declared is blamed over its name`() {
        assertEquals(
            "(1:9)-(1:15) Variable 'missing' is not declared.",
            reportOf("println(missing);"),
        )
    }

    @Test
    fun `an operator its operands do not fit is blamed over the whole expression`() {
        assertEquals(
            "(1:9)-(1:15) Cannot apply '-' to string and number.",
            reportOf("println('a' - 1);"),
        )
    }

    @Test
    fun `a division by zero is blamed over the whole expression`() {
        assertEquals("(1:9)-(1:13) Division by zero.", reportOf("println(1 / 0);"))
    }

    /**
     * lexes, parses and runs one statement, and renders the first failure
     */
    private fun reportOf(source: String): String = renderer.render(errorOf(source))

    private fun errorOf(source: String): Diagnostic {
        val tokens = mutableListOf<Token>()

        for (result in Lexer(StringSourceReader(source), TokenRecognizers.DEFAULT).tokens()) {
            when (result) {
                is Success -> tokens.add(result.value)
                is Failure -> return result.error
            }
        }

        val statement =
            when (val parsed = Parser.parse(tokens)) {
                is Success -> parsed.value
                is Failure -> return parsed.error
            }

        val interpreter = Interpreter(Environment(), CollectingOutput(), ValueOps())

        return when (val executed = interpreter.execute(statement)) {
            is Failure -> executed.error
            is Success -> fail("expected $source to fail, but it ran")
        }
    }
}
