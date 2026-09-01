package com.printscript.parser

import com.printscript.ast.BinaryOperator
import com.printscript.ast.Expression
import com.printscript.common.Position
import com.printscript.language.NumberCodec
import com.printscript.parser.ParsingSupport.unexpectedEndOfExpression
import com.printscript.parser.ParsingSupport.unexpectedToken
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.SyntaxSymbol
import com.printscript.report.flatMap
import com.printscript.report.map
import com.printscript.token.Token

class ExpressionParser(
    private val cursor: TokenCursor,
) {
    fun parse(): Result<Expression> = parseExpression(cursor, 0)

    /**
     * parses an expression while respecting the minimum operator precedence
     */
    private fun parseExpression(
        cursor: TokenCursor,
        minPrecedence: Int,
    ): Result<Expression> =
        parsePrimary(cursor).flatMap { left ->
            parseBinaryExpression(cursor, left, minPrecedence)
        }

    /**
     * builds binary expressions according to operator precedence
     */
    private fun parseBinaryExpression(
        cursor: TokenCursor,
        left: Expression,
        minPrecedence: Int,
    ): Result<Expression> {
        var currentLeft = left

        while (true) {
            val token = cursor.peek() ?: break
            val precedence = PrecedenceTable.precedenceOf(token)

            if (precedence <= minPrecedence) {
                break
            }

            cursor.consume()

            val operator = binaryOperatorOf(token)

            when (val right = parseExpression(cursor, precedence)) {
                is Failure -> return right

                is Success -> {
                    currentLeft =
                        createBinaryExpression(
                            left = currentLeft,
                            operator = operator,
                            right = right.value,
                        )
                }
            }
        }

        return Success(currentLeft)
    }

    /**
     * parses the atomic expressions that can appear before a binary operator
     */
    private fun parsePrimary(cursor: TokenCursor): Result<Expression> {
        val token =
            cursor.consume()
                ?: return unexpectedEndOfExpression(cursor)

        return parsePrimaryToken(cursor, token)
    }

    /**
     * parses a token into the corresponding primary expression
     */
    private fun parsePrimaryToken(
        cursor: TokenCursor,
        token: Token,
    ): Result<Expression> =
        when (token) {
            is Token.NumberLiteralToken ->
                parseNumber(token)

            is Token.StringLiteralToken ->
                Success(
                    Expression.StringLiteral(
                        value = token.value,
                        start = token.start,
                        end = token.end,
                    ),
                )

            is Token.IdentifierToken ->
                Success(
                    Expression.VariableReference(
                        name = token.lexeme,
                        start = token.start,
                        end = token.end,
                    ),
                )

            is Token.LeftParenToken ->
                parseParenthesizedExpression(cursor, token)

            else -> unexpectedToken(token)
        }

    /**
     * parses a numeric literal using [NumberCodec], preserving its source position
     */
    private fun parseNumber(token: Token.NumberLiteralToken): Result<Expression> =
        NumberCodec
            .parse(
                text = token.value,
                span = token.span,
            ).map { value ->
                Expression.NumberLiteral(
                    value = value,
                    start = token.start,
                    end = token.end,
                )
            }

    /**
     * parses the expression inside parentheses and extends its position to include them
     */
    private fun parseParenthesizedExpression(
        cursor: TokenCursor,
        openingParen: Token.LeftParenToken,
    ): Result<Expression> =
        parseExpression(cursor, 0).flatMap { expression ->
            val closingParen = cursor.consume()

            if (closingParen is Token.RightParenToken) {
                Success(
                    withPosition(
                        expression,
                        openingParen.start,
                        closingParen.end,
                    ),
                )
            } else {
                Failure(
                    Diagnostic.ExpectedSymbol(
                        expected = SyntaxSymbol.RIGHT_PAREN,
                        span = closingParen?.span ?: cursor.endOfInput(),
                    ),
                )
            }
        }

    /**
     * creates a binary expression from its left operand, operator, and right operand
     */
    private fun createBinaryExpression(
        left: Expression,
        operator: BinaryOperator,
        right: Expression,
    ): Expression =
        Expression.BinaryExpression(
            left = left,
            operator = operator,
            right = right,
            start = left.start,
            end = right.end,
        )

    /**
     * maps a binary operator token to its corresponding AST operator
     * assumes [token] has already been identified as a binary operator
     */
    private fun binaryOperatorOf(token: Token): BinaryOperator =
        when (token) {
            is Token.PlusToken -> BinaryOperator.Addition
            is Token.MinusToken -> BinaryOperator.Subtraction
            is Token.StarToken -> BinaryOperator.Multiplication
            is Token.SlashToken -> BinaryOperator.Division
            else -> error("Token is not a binary operator")
        }

    /**
     * updates an expression's source range without changing its structure
     */
    private fun withPosition(
        expression: Expression,
        start: Position,
        end: Position,
    ): Expression =
        when (expression) {
            is Expression.NumberLiteral ->
                expression.copy(start = start, end = end)

            is Expression.StringLiteral ->
                expression.copy(start = start, end = end)

            is Expression.VariableReference ->
                expression.copy(start = start, end = end)

            is Expression.BinaryExpression ->
                expression.copy(start = start, end = end)
        }
}
