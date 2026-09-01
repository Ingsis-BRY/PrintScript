package com.printscript.pipeline

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.lexer.Lexer
import com.printscript.lexer.StringSourceReader
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.parser.Parser
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Success
import com.printscript.report.SyntaxSymbol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StatementStreamTest {
    private fun streamOf(source: String): StatementStream {
        val lexer = Lexer(StringSourceReader(source), TokenRecognizers.DEFAULT)

        return StatementStream(
            source = TokenSource(lexer::tokens),
            parser = StatementParser(Parser::parse),
        )
    }

    // drains the whole stream, failing the test on the first error
    private fun statementsOf(source: String): List<Statement> {
        val stream = streamOf(source)
        val statements = mutableListOf<Statement>()

        while (stream.hasNext()) {
            statements.add(assertIs<Success<Statement>>(stream.next()).value)
        }

        return statements
    }

    // Splitting

    @Test
    fun `splits a source into one statement per semicolon`() {
        val statements = statementsOf("let x: number = 5; println(x);")

        assertEquals(2, statements.size)
        assertIs<Statement.VariableDeclaration>(statements[0])
        assertIs<Statement.CallStatement>(statements[1])
    }

    @Test
    fun `recognizes the three kinds of statement in one source`() {
        val statements =
            statementsOf(
                """
                let x: number = 5;
                x = 6;
                println(x);
                """.trimIndent(),
            )

        assertEquals(3, statements.size)
        assertIs<Statement.VariableDeclaration>(statements[0])
        assertIs<Statement.Assignment>(statements[1])
        assertIs<Statement.CallStatement>(statements[2])
    }

    @Test
    fun `an empty source has no statements`() {
        assertFalse(streamOf("   ").hasNext())
    }

    @Test
    fun `whitespace and newlines between statements are ignored`() {
        val statements = statementsOf("\n\n  let x: number = 1;\n\t println(x); \n")

        assertEquals(2, statements.size)
    }

    // Parsing the statement that came out

    @Test
    fun `keeps a declaration's name, type and initializer`() {
        val declaration =
            assertIs<Statement.VariableDeclaration>(
                statementsOf("let total: number = 1 + 2;").single(),
            )

        assertEquals("total", declaration.name)
        assertEquals(Type.NumberType, declaration.declaredType)
        assertIs<Expression.BinaryExpression>(declaration.initializer)
    }

    @Test
    fun `parses an assignment on its own`() {
        val assignment = assertIs<Statement.Assignment>(statementsOf("a = 5;").single())

        assertEquals("a", assignment.name)
        assertIs<Expression.NumberLiteral>(assignment.value)
    }

    @Test
    fun `keeps each statement's start position across lines`() {
        val statements =
            statementsOf(
                """
                let x: number = 1;
                println(x);
                """.trimIndent(),
            )

        assertEquals(1, statements[0].start.line)
        assertEquals(2, statements[1].start.line)
    }

    // Errors

    @Test
    fun `propagates a lexical error from the lexer`() {
        val error = assertIs<Failure>(streamOf("@").next()).error

        val unexpected = assertIs<Diagnostic.UnexpectedCharacter>(error)
        assertEquals('@', unexpected.character)
    }

    @Test
    fun `reports a missing colon as the symbol the parser expected`() {
        val error = assertIs<Failure>(streamOf("let x number = 5;").next()).error

        val expected = assertIs<Diagnostic.ExpectedSymbol>(error)
        assertEquals(SyntaxSymbol.COLON, expected.expected)
    }

    @Test
    fun `a statement without a closing semicolon is a parse error`() {
        val error = assertIs<Failure>(streamOf("println(1)").next()).error

        val expected = assertIs<Diagnostic.ExpectedSymbol>(error)
        assertEquals(SyntaxSymbol.SEMICOLON, expected.expected)
    }

    @Test
    fun `a token that cannot start a statement is rejected`() {
        val error = assertIs<Failure>(streamOf("1 + 2;").next()).error

        assertIs<Diagnostic.UnexpectedToken>(error)
    }

    @Test
    fun `stops at the first error and does not keep reading`() {
        val stream = streamOf("let x: number = @; println(1);")

        assertIs<Failure>(stream.next())
        assertFalse(stream.hasNext())
    }

    // Streaming

    @Test
    fun `streams a large source one statement at a time`() {
        val count = 1000
        val source = "println(1);".repeat(count)
        val stream = streamOf(source)

        var parsed = 0
        while (stream.hasNext()) {
            assertIs<Success<Statement>>(stream.next())
            parsed++
        }

        assertEquals(count, parsed)
        assertTrue(parsed > 0)
    }
}
