package com.printscript.report

import com.printscript.ast.BinaryOperator
import com.printscript.ast.Type
import com.printscript.common.Position
import com.printscript.common.Span
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ErrorRendererTest {
    private val renderer = ErrorRenderer()

    private val span = Span(Position(2, 9), Position(2, 14))

    private val oneCharacter = Span.at(Position(1, 3))

    // Span

    @Test
    fun `a message opens with the start and the end of the error`() {
        val rendered = renderer.render(Diagnostic.DivisionByZero(span))

        assertTrue(rendered.startsWith("(2:9)-(2:14) "), rendered)
    }

    @Test
    fun `a one character error repeats its position on both ends`() {
        val rendered = renderer.render(Diagnostic.DivisionByZero(oneCharacter))

        assertEquals("(1:3)-(1:3) Division by zero.", rendered)
    }

    @Test
    fun `a multi line error keeps both lines`() {
        val rendered =
            renderer.render(
                Diagnostic.DivisionByZero(Span(Position(4, 20), Position(6, 2))),
            )

        assertEquals("(4:20)-(6:2) Division by zero.", rendered)
    }

    // Lexical

    @Test
    fun `renders an unexpected character`() {
        assertEquals(
            "(1:3)-(1:3) Unexpected character '@'.",
            renderer.render(Diagnostic.UnexpectedCharacter('@', oneCharacter)),
        )
    }

    @Test
    fun `renders an unterminated string literal`() {
        assertEquals(
            "(2:9)-(2:14) Unterminated string literal.",
            renderer.render(
                Diagnostic.MalformedLexeme(LexicalFault.UNTERMINATED_STRING, span),
            ),
        )
    }

    @Test
    fun `renders a source that could not be read`() {
        assertEquals(
            "(1:3)-(1:3) Could not read source: disk went away.",
            renderer.render(Diagnostic.SourceUnreadable("disk went away", oneCharacter)),
        )
    }

    // Syntactic

    @Test
    fun `renders every symbol the parser can demand`() {
        val expected =
            mapOf(
                SyntaxSymbol.LET to "Expected 'let'.",
                SyntaxSymbol.IDENTIFIER to "Expected an identifier.",
                SyntaxSymbol.COLON to "Expected ':'.",
                SyntaxSymbol.TYPE_NAME to "Expected a type.",
                SyntaxSymbol.ASSIGN to "Expected '='.",
                SyntaxSymbol.SEMICOLON to "Expected ';' at the end of the statement.",
                SyntaxSymbol.LEFT_PAREN to "Expected '('.",
                SyntaxSymbol.RIGHT_PAREN to "Expected ')'.",
                SyntaxSymbol.PRINTLN to "Expected 'println'.",
            )

        assertEquals(SyntaxSymbol.entries.toSet(), expected.keys)

        for ((symbol, message) in expected) {
            assertEquals(
                "(1:3)-(1:3) $message",
                renderer.render(Diagnostic.ExpectedSymbol(symbol, oneCharacter)),
            )
        }
    }

    @Test
    fun `renders an unexpected token`() {
        assertEquals(
            "(1:3)-(1:3) Unexpected token '+'.",
            renderer.render(Diagnostic.UnexpectedToken("+", oneCharacter)),
        )
    }

    @Test
    fun `renders both ways the input can run out`() {
        assertEquals(
            "(1:3)-(1:3) Unexpected end of expression.",
            renderer.render(
                Diagnostic.UnexpectedEndOfInput(SyntacticUnit.EXPRESSION, oneCharacter),
            ),
        )
        assertEquals(
            "(1:3)-(1:3) Unexpected end of statement.",
            renderer.render(
                Diagnostic.UnexpectedEndOfInput(SyntacticUnit.STATEMENT, oneCharacter),
            ),
        )
    }

    @Test
    fun `renders an unknown type`() {
        assertEquals(
            "(2:9)-(2:14) Unknown type 'boolean'.",
            renderer.render(Diagnostic.UnknownType("boolean", span)),
        )
    }

    @Test
    fun `renders a malformed number`() {
        assertEquals(
            "(2:9)-(2:14) Malformed number '1.2.3'.",
            renderer.render(Diagnostic.MalformedNumber("1.2.3", span)),
        )
    }

    // Semantic

    @Test
    fun `renders a variable declared twice`() {
        assertEquals(
            "(2:9)-(2:14) Variable 'x' is already declared.",
            renderer.render(Diagnostic.VariableAlreadyDeclared("x", span)),
        )
    }

    @Test
    fun `renders a variable that was never declared`() {
        assertEquals(
            "(2:9)-(2:14) Variable 'x' is not declared.",
            renderer.render(Diagnostic.VariableNotDeclared("x", span)),
        )
    }

    @Test
    fun `renders a variable read before it holds a value`() {
        assertEquals(
            "(2:9)-(2:14) Variable 'x' is used before it has a value.",
            renderer.render(Diagnostic.VariableWithoutValue("x", span)),
        )
    }

    @Test
    fun `a variable that was never declared reads differently from one without a value`() {
        assertNotEquals(
            renderer.render(Diagnostic.VariableNotDeclared("x", span)),
            renderer.render(Diagnostic.VariableWithoutValue("x", span)),
        )
    }

    @Test
    fun `renders an assignment of the wrong type, naming both types`() {
        assertEquals(
            "(2:9)-(2:14) Cannot assign a string value to variable 'x' of type number.",
            renderer.render(
                Diagnostic.IncompatibleAssignment(
                    name = "x",
                    declared = Type.NumberType,
                    actual = Type.StringType,
                    span = span,
                ),
            ),
        )
    }

    @Test
    fun `renders operands an operator does not accept`() {
        assertEquals(
            "(2:9)-(2:14) Cannot apply '-' to string and number.",
            renderer.render(
                Diagnostic.IncompatibleOperands(
                    operator = BinaryOperator.Subtraction,
                    left = Type.StringType,
                    right = Type.NumberType,
                    span = span,
                ),
            ),
        )
    }

    @Test
    fun `renders every operator by the symbol it is written with`() {
        val symbols =
            mapOf(
                BinaryOperator.Addition to "+",
                BinaryOperator.Subtraction to "-",
                BinaryOperator.Multiplication to "*",
                BinaryOperator.Division to "/",
            )

        for ((operator, symbol) in symbols) {
            val rendered =
                renderer.render(
                    Diagnostic.IncompatibleOperands(
                        operator = operator,
                        left = Type.StringType,
                        right = Type.StringType,
                        span = span,
                    ),
                )

            assertTrue(rendered.contains("'$symbol'"), rendered)
        }
    }

    @Test
    fun `renders a call to a function that does not exist`() {
        assertEquals(
            "(2:9)-(2:14) Unknown function 'print'.",
            renderer.render(Diagnostic.UnknownFunction("print", span)),
        )
    }

    @Test
    fun `renders a division by zero`() {
        assertEquals(
            "(2:9)-(2:14) Division by zero.",
            renderer.render(Diagnostic.DivisionByZero(span)),
        )
    }
}
