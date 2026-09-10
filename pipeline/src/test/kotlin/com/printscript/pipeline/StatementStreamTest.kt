package com.printscript.pipeline

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.lexer.Lexer
import com.printscript.lexer.StringSourceReader
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.parser.Parser
import com.printscript.parser.expression.PrefixParselets
import com.printscript.parser.syntax.StatementSyntaxes
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
    private fun streamOf(source: String): StatementStream = streamOf(source, version10)

    private fun streamOf(
        source: String,
        version: Version,
    ): StatementStream {
        val lexer = Lexer(StringSourceReader(source), version.recognizers)

        return StatementStream(
            source = TokenSource(lexer::tokens),
            parser = StatementParser(Parser(version.syntaxes, version.parselets)::parse),
            boundary = version.boundary,
        )
    }

    private data class Version(
        val recognizers: List<com.printscript.lexer.recognizer.TokenRecognizer>,
        val syntaxes: List<com.printscript.parser.syntax.StatementSyntax>,
        val parselets: List<com.printscript.parser.expression.PrefixParselet>,
        val boundary: StatementBoundary,
    )

    private val version10 =
        Version(
            TokenRecognizers.V1_0,
            StatementSyntaxes.V1_0,
            PrefixParselets.V1_0,
            StatementBoundaries.V1_0,
        )

    private val version11 =
        Version(
            TokenRecognizers.V1_1,
            StatementSyntaxes.V1_1,
            PrefixParselets.V1_1,
            StatementBoundaries.V1_1,
        )

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

    private fun statements11Of(source: String): List<Statement> {
        val stream = streamOf(source, version11)
        val statements = mutableListOf<Statement>()

        while (stream.hasNext()) {
            statements.add(assertIs<Success<Statement>>(stream.next()).value)
        }

        return statements
    }

    @Test
    fun `a semicolon inside a block does not end the statement`() {
        val statements = statements11Of("if (a) { println(1); println(2); }")

        assertEquals(1, statements.size)
        val conditional = assertIs<Statement.IfStatement>(statements.single())
        assertEquals(2, conditional.consequence.size)
    }

    @Test
    fun `an else keeps the statement going past the closing brace`() {
        val statements = statements11Of("if (a) { println(1); } else { println(2); }")

        assertEquals(1, statements.size)
        assertEquals(1, assertIs<Statement.IfStatement>(statements.single()).alternative?.size)
    }

    @Test
    fun `a closing brace with no else ends the statement`() {
        val statements = statements11Of("if (a) { println(1); } println(2);")

        assertEquals(2, statements.size)
        assertIs<Statement.IfStatement>(statements.first())
        assertIs<Statement.CallStatement>(statements.last())
    }

    @Test
    fun `only the outermost closing brace ends the statement`() {
        val statements = statements11Of("if (a) { if (a) { println(1); } } println(2);")

        assertEquals(2, statements.size)
        val outer = assertIs<Statement.IfStatement>(statements.first())
        assertIs<Statement.IfStatement>(outer.consequence.single())
    }

    @Test
    fun `an if on its own is one statement`() {
        assertEquals(1, statements11Of("if (a) { println(1); }").size)
    }

    @Test
    fun `statements around a block are still split by their semicolons`() {
        val statements =
            statements11Of("let a: boolean = true; if (a) { println(1); } println(2);")

        assertEquals(3, statements.size)
    }

    @Test
    fun `a block that never closes is handed to the parser as it is`() {
        val stream = streamOf("if (a) { println(1);", version11)

        assertIs<Failure>(stream.next())
    }

    @Test
    fun `a stray closing brace ends the batch instead of running away`() {
        val stream = streamOf("} println(1);", version11)

        assertIs<Failure>(stream.next())
        assertTrue(stream.hasNext(), "the tokens after the stray brace are still there")
    }

    @Test
    fun `the lookahead does not lose a token`() {
        val statements = statements11Of("println(1); println(2); println(3);")

        assertEquals(3, statements.size)
    }

    @Test
    fun `a lexical failure read while looking ahead is reported on the next statement`() {
        val stream = streamOf("println(1); @", version11)

        assertIs<Success<Statement>>(stream.next())
        assertTrue(stream.hasNext())
        assertIs<Failure>(stream.next())
    }
}
