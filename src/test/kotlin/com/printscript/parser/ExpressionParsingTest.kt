package com.printscript.parser

import com.printscript.ast.BinaryOperator
import com.printscript.ast.Expression
import com.printscript.common.Failure
import com.printscript.common.Position
import com.printscript.common.Success
import com.printscript.token.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExpressionParsingTest {

    @Test
    fun `should parse a number literal`() {
        val result = Parser.parseExpression(
            listOf(number("42", 2, 4))
        )

        assertEquals(
            Success(
                Expression.NumberLiteral(
                    value = 42.0,
                    start = Position(2, 4),
                    end = Position(2, 6)
                )
            ),
            result
        )
    }

    @Test
    fun `should parse a decimal number literal`() {
        val result = Parser.parseExpression(
            listOf(number("3.14", 4, 7))
        )

        assertEquals(
            Success(
                Expression.NumberLiteral(
                    value = 3.14,
                    start = Position(4, 7),
                    end = Position(4, 11)
                )
            ),
            result
        )
    }

    @Test
    fun `should parse a string literal`() {
        val result = Parser.parseExpression(
            listOf(string("hello", 3, 5))
        )

        assertEquals(
            Success(
                Expression.StringLiteral(
                    value = "hello",
                    start = Position(3, 5),
                    end = Position(3, 10)
                )
            ),
            result
        )
    }

    @Test
    fun `should parse another string literal at a different position`() {
        val result = Parser.parseExpression(
            listOf(string("world", 6, 8))
        )

        assertEquals(
            Success(
                Expression.StringLiteral(
                    value = "world",
                    start = Position(6, 8),
                    end = Position(6, 13)
                )
            ),
            result
        )
    }

    @Test
    fun `should parse a variable reference`() {
        val result = Parser.parseExpression(
            listOf(identifier("foo", 5, 3))
        )

        assertEquals(
            Success(
                Expression.VariableReference(
                    name = "foo",
                    start = Position(5, 3),
                    end = Position(5, 6)
                )
            ),
            result
        )
    }

    @Test
    fun `should parse another variable reference at a different position`() {
        val result = Parser.parseExpression(
            listOf(identifier("total", 8, 6))
        )

        assertEquals(
            Success(
                Expression.VariableReference(
                    name = "total",
                    start = Position(8, 6),
                    end = Position(8, 11)
                )
            ),
            result
        )
    }

    @Test
    fun `should parse addition`() {
        val result = Parser.parseExpression(
            listOf(
                number("12", 2, 3),
                plus(2, 6),
                number("7", 2, 8)
            )
        )

        assertEquals(
            Success(
                Expression.BinaryExpression(
                    left = Expression.NumberLiteral(
                        12.0,
                        Position(2, 3),
                        Position(2, 5)
                    ),
                    operator = BinaryOperator.Addition,
                    right = Expression.NumberLiteral(
                        7.0,
                        Position(2, 8),
                        Position(2, 9)
                    ),
                    start = Position(2, 3),
                    end = Position(2, 9)
                )
            ),
            result
        )
    }

    @Test
    fun `multiplication should have higher precedence than addition`() {
        val result = Parser.parseExpression(
            listOf(
                number("8", 4, 2),
                plus(4, 4),
                number("15", 4, 6),
                star(4, 9),
                number("3", 4, 11)
            )
        )

        val expected = Expression.BinaryExpression(
            left = Expression.NumberLiteral(
                8.0,
                Position(4, 2),
                Position(4, 3)
            ),
            operator = BinaryOperator.Addition,
            right = Expression.BinaryExpression(
                left = Expression.NumberLiteral(
                    15.0,
                    Position(4, 6),
                    Position(4, 8)
                ),
                operator = BinaryOperator.Multiplication,
                right = Expression.NumberLiteral(
                    3.0,
                    Position(4, 11),
                    Position(4, 12)
                ),
                start = Position(4, 6),
                end = Position(4, 12)
            ),
            start = Position(4, 2),
            end = Position(4, 12)
        )

        assertEquals(Success(expected), result)
    }

    @Test
    fun `should respect parentheses over operator precedence`() {
        val result = Parser.parseExpression(
            listOf(
                leftParen(6, 4),
                number("5", 6, 5),
                plus(6, 7),
                number("6", 6, 9),
                rightParen(6, 10),
                star(6, 12),
                number("2", 6, 14)
            )
        )

        val expected = Expression.BinaryExpression(
            left = Expression.BinaryExpression(
                left = Expression.NumberLiteral(
                    5.0,
                    Position(6, 5),
                    Position(6, 6)
                ),
                operator = BinaryOperator.Addition,
                right = Expression.NumberLiteral(
                    6.0,
                    Position(6, 9),
                    Position(6, 10)
                ),
                start = Position(6, 4),
                end = Position(6, 11)
            ),
            operator = BinaryOperator.Multiplication,
            right = Expression.NumberLiteral(
                2.0,
                Position(6, 14),
                Position(6, 15)
            ),
            start = Position(6, 4),
            end = Position(6, 15)
        )

        assertEquals(Success(expected), result)
    }

    @Test
    fun `should parse nested expression with three levels`() {
        val result = Parser.parseExpression(
            listOf(
                number("10", 9, 2),
                plus(9, 5),
                number("20", 9, 7),
                star(9, 10),
                number("3", 9, 12),
                minus(9, 14),
                number("4", 9, 16)
            )
        )

        val expected = Expression.BinaryExpression(
            left = Expression.BinaryExpression(
                left = Expression.NumberLiteral(
                    10.0,
                    Position(9, 2),
                    Position(9, 4)
                ),
                operator = BinaryOperator.Addition,
                right = Expression.BinaryExpression(
                    left = Expression.NumberLiteral(
                        20.0,
                        Position(9, 7),
                        Position(9, 9)
                    ),
                    operator = BinaryOperator.Multiplication,
                    right = Expression.NumberLiteral(
                        3.0,
                        Position(9, 12),
                        Position(9, 13)
                    ),
                    start = Position(9, 7),
                    end = Position(9, 13)
                ),
                start = Position(9, 2),
                end = Position(9, 13)
            ),
            operator = BinaryOperator.Subtraction,
            right = Expression.NumberLiteral(
                4.0,
                Position(9, 16),
                Position(9, 17)
            ),
            start = Position(9, 2),
            end = Position(9, 17)
        )

        assertEquals(Success(expected), result)
    }

    @Test
    fun `should propagate malformed number error`() {
        val result = Parser.parseExpression(
            listOf(number("1.2.3", 11, 5))
        )

        val failure = assertIs<Failure>(result)

        assertEquals("Malformed number: 1.2.3", failure.error.message)
        assertEquals(Position(11, 5), failure.error.position)
    }

    @Test
    fun `should return error for unexpected token`() {
        val result = Parser.parseExpression(
            listOf(plus(13, 7))
        )

        val failure = assertIs<Failure>(result)

        assertEquals("Unexpected token: +", failure.error.message)
        assertEquals(Position(13, 7), failure.error.position)
    }

    @Test
    fun `should return error when closing parenthesis is missing`() {
        val result = Parser.parseExpression(
            listOf(
                leftParen(15, 3),
                number("9", 15, 4),
                plus(15, 6),
                number("11", 15, 8)
            )
        )

        val failure = assertIs<Failure>(result)

        assertEquals("Expected closing parenthesis", failure.error.message)
        assertEquals(Position(15, 4), failure.error.position)
    }

    @Test
    fun `should parse parenthesized expression with different position`() {
        val result = Parser.parseExpression(
            listOf(
                leftParen(18, 7),
                number("25", 18, 8),
                rightParen(18, 10)
            )
        )

        assertEquals(
            Success(
                Expression.NumberLiteral(
                    value = 25.0,
                    start = Position(18, 7),
                    end = Position(18, 11)
                )
            ),
            result
        )
    }

    private fun number(
        value: String,
        line: Int,
        column: Int
    ): Token.NumberLiteralToken =
        Token.NumberLiteralToken(
            lexeme = value,
            value = value,
            start = Position(line, column),
            end = Position(line, column + value.length)
        )

    private fun string(
        value: String,
        line: Int,
        column: Int
    ): Token.StringLiteralToken =
        Token.StringLiteralToken(
            lexeme = value,
            value = value,
            start = Position(line, column),
            end = Position(line, column + value.length)
        )

    private fun identifier(
        name: String,
        line: Int,
        column: Int
    ): Token.IdentifierToken =
        Token.IdentifierToken(
            lexeme = name,
            start = Position(line, column),
            end = Position(line, column + name.length)
        )

    private fun plus(line: Int, column: Int): Token.PlusToken =
        Token.PlusToken(
            lexeme = "+",
            start = Position(line, column),
            end = Position(line, column + 1)
        )

    private fun minus(line: Int, column: Int): Token.MinusToken =
        Token.MinusToken(
            lexeme = "-",
            start = Position(line, column),
            end = Position(line, column + 1)
        )

    private fun star(line: Int, column: Int): Token.StarToken =
        Token.StarToken(
            lexeme = "*",
            start = Position(line, column),
            end = Position(line, column + 1)
        )

    private fun leftParen(line: Int, column: Int): Token.LeftParenToken =
        Token.LeftParenToken(
            lexeme = "(",
            start = Position(line, column),
            end = Position(line, column + 1)
        )

    private fun rightParen(line: Int, column: Int): Token.RightParenToken =
        Token.RightParenToken(
            lexeme = ")",
            start = Position(line, column),
            end = Position(line, column + 1)
        )
}