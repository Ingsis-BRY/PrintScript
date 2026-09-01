package com.printscript.parser

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.common.Position
import com.printscript.common.Span
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Success
import com.printscript.report.SyntaxSymbol
import com.printscript.token.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StatementParsingTest {
    @Test
    fun `should parse variable declaration with initializer`() {
        val result =
            Parser.parse(
                listOf(
                    let(2, 1),
                    identifier("x", 2, 5),
                    colon(2, 6),
                    typeName("number", 2, 8),
                    assign(2, 15),
                    number("42", 2, 17),
                    semicolon(2, 19),
                ),
            )

        assertEquals(
            Success(
                Statement.VariableDeclaration(
                    name = "x",
                    declaredType = Type.NumberType,
                    initializer =
                        Expression.NumberLiteral(
                            value = 42.0,
                            start = Position(2, 17),
                            end = Position(2, 19),
                        ),
                    start = Position(2, 1),
                    end = Position(2, 20),
                ),
            ),
            result,
        )
    }

    @Test
    fun `should parse variable declaration without initializer`() {
        val result =
            Parser.parse(
                listOf(
                    let(4, 1),
                    identifier("message", 4, 5),
                    colon(4, 12),
                    typeName("string", 4, 14),
                    semicolon(4, 20),
                ),
            )

        assertEquals(
            Success(
                Statement.VariableDeclaration(
                    name = "message",
                    declaredType = Type.StringType,
                    initializer = null,
                    start = Position(4, 1),
                    end = Position(4, 21),
                ),
            ),
            result,
        )
    }

    @Test
    fun `should parse assignment`() {
        val result =
            Parser.parse(
                listOf(
                    identifier("x", 6, 1),
                    assign(6, 3),
                    number("10", 6, 5),
                    semicolon(6, 7),
                ),
            )

        assertEquals(
            Success(
                Statement.Assignment(
                    name = "x",
                    value =
                        Expression.NumberLiteral(
                            value = 10.0,
                            start = Position(6, 5),
                            end = Position(6, 7),
                        ),
                    start = Position(6, 1),
                    end = Position(6, 8),
                ),
            ),
            result,
        )
    }

    @Test
    fun `should parse println statement`() {
        val result =
            Parser.parse(
                listOf(
                    identifier("println", 8, 1),
                    leftParen(8, 8),
                    identifier("x", 8, 9),
                    rightParen(8, 10),
                    semicolon(8, 11),
                ),
            )

        assertEquals(
            Success(
                Statement.CallStatement(
                    callee = "println",
                    argument =
                        Expression.VariableReference(
                            name = "x",
                            start = Position(8, 9),
                            end = Position(8, 10),
                        ),
                    start = Position(8, 1),
                    end = Position(8, 12),
                ),
            ),
            result,
        )
    }

    @Test
    fun `should parse println with expression argument`() {
        val result =
            Parser.parse(
                listOf(
                    identifier("println", 10, 1),
                    leftParen(10, 8),
                    number("5", 10, 9),
                    plus(10, 11),
                    number("3", 10, 13),
                    rightParen(10, 14),
                    semicolon(10, 15),
                ),
            )

        assertEquals(
            Success(
                Statement.CallStatement(
                    callee = "println",
                    argument =
                        Expression.BinaryExpression(
                            left =
                                Expression.NumberLiteral(
                                    value = 5.0,
                                    start = Position(10, 9),
                                    end = Position(10, 10),
                                ),
                            operator = com.printscript.ast.BinaryOperator.Addition,
                            right =
                                Expression.NumberLiteral(
                                    value = 3.0,
                                    start = Position(10, 13),
                                    end = Position(10, 14),
                                ),
                            start = Position(10, 9),
                            end = Position(10, 14),
                        ),
                    start = Position(10, 1),
                    end = Position(10, 16),
                ),
            ),
            result,
        )
    }

    @Test
    fun `should return error when semicolon is missing`() {
        val result =
            Parser.parse(
                listOf(
                    identifier("x", 12, 1),
                    assign(12, 3),
                    number("42", 12, 5),
                ),
            )

        val failure = assertIs<Failure>(result)

        // the tokens ran out, so the error lands where the last one ended
        assertEquals(
            Diagnostic.ExpectedSymbol(
                expected = SyntaxSymbol.SEMICOLON,
                span = Span.at(Position(12, 7)),
            ),
            failure.error,
        )
    }

    @Test
    fun `should return error when declaration identifier is invalid`() {
        val result =
            Parser.parse(
                listOf(
                    let(14, 1),
                    number("5", 14, 5),
                    colon(14, 7),
                    typeName("number", 14, 9),
                    semicolon(14, 15),
                ),
            )

        val failure = assertIs<Failure>(result)

        assertEquals(
            Diagnostic.ExpectedSymbol(
                expected = SyntaxSymbol.IDENTIFIER,
                span = Span(Position(14, 5), Position(14, 6)),
            ),
            failure.error,
        )
    }

    @Test
    fun `should return error for unknown type`() {
        val result =
            Parser.parse(
                listOf(
                    let(16, 1),
                    identifier("x", 16, 5),
                    colon(16, 6),
                    typeName("boolean", 16, 8),
                    semicolon(16, 15),
                ),
            )

        val failure = assertIs<Failure>(result)

        assertEquals(
            Diagnostic.UnknownType(
                name = "boolean",
                span = Span(Position(16, 8), Position(16, 15)),
            ),
            failure.error,
        )
    }

    @Test
    fun `should return error when assignment operator is missing`() {
        val result =
            Parser.parse(
                listOf(
                    identifier("x", 18, 1),
                    number("42", 18, 3),
                    semicolon(18, 5),
                ),
            )

        val failure = assertIs<Failure>(result)

        assertEquals(
            Diagnostic.ExpectedSymbol(
                expected = SyntaxSymbol.ASSIGN,
                span = Span(Position(18, 3), Position(18, 5)),
            ),
            failure.error,
        )
    }

    @Test
    fun `should return error when println is missing opening parenthesis`() {
        val result =
            Parser.parse(
                listOf(
                    identifier("println", 20, 1),
                    number("42", 20, 9),
                    rightParen(20, 11),
                    semicolon(20, 12),
                ),
            )

        val failure = assertIs<Failure>(result)

        assertEquals(
            Diagnostic.ExpectedSymbol(
                expected = SyntaxSymbol.LEFT_PAREN,
                span = Span(Position(20, 9), Position(20, 11)),
            ),
            failure.error,
        )
    }

    private fun let(
        line: Int,
        column: Int,
    ): Token.LetToken =
        Token.LetToken(
            lexeme = "let",
            start = Position(line, column),
            end = Position(line, column + 3),
        )

    private fun identifier(
        name: String,
        line: Int,
        column: Int,
    ): Token.IdentifierToken =
        Token.IdentifierToken(
            lexeme = name,
            start = Position(line, column),
            end = Position(line, column + name.length),
        )

    private fun typeName(
        type: String,
        line: Int,
        column: Int,
    ): Token.TypeNameToken =
        Token.TypeNameToken(
            lexeme = type,
            start = Position(line, column),
            end = Position(line, column + type.length),
        )

    private fun number(
        value: String,
        line: Int,
        column: Int,
    ): Token.NumberLiteralToken =
        Token.NumberLiteralToken(
            lexeme = value,
            value = value,
            start = Position(line, column),
            end = Position(line, column + value.length),
        )

    private fun plus(
        line: Int,
        column: Int,
    ): Token.PlusToken =
        Token.PlusToken(
            lexeme = "+",
            start = Position(line, column),
            end = Position(line, column + 1),
        )

    private fun colon(
        line: Int,
        column: Int,
    ): Token.ColonToken =
        Token.ColonToken(
            lexeme = ":",
            start = Position(line, column),
            end = Position(line, column + 1),
        )

    private fun assign(
        line: Int,
        column: Int,
    ): Token.AssignToken =
        Token.AssignToken(
            lexeme = "=",
            start = Position(line, column),
            end = Position(line, column + 1),
        )

    private fun semicolon(
        line: Int,
        column: Int,
    ): Token.SemicolonToken =
        Token.SemicolonToken(
            lexeme = ";",
            start = Position(line, column),
            end = Position(line, column + 1),
        )

    private fun leftParen(
        line: Int,
        column: Int,
    ): Token.LeftParenToken =
        Token.LeftParenToken(
            lexeme = "(",
            start = Position(line, column),
            end = Position(line, column + 1),
        )

    private fun rightParen(
        line: Int,
        column: Int,
    ): Token.RightParenToken =
        Token.RightParenToken(
            lexeme = ")",
            start = Position(line, column),
            end = Position(line, column + 1),
        )
}
