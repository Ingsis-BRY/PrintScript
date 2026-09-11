package com.printscript.parser

import com.printscript.ast.BinaryOperator
import com.printscript.ast.Expression
import com.printscript.parser.ParsingSupport.unexpectedEndOfExpression
import com.printscript.parser.ParsingSupport.unexpectedToken
import com.printscript.parser.expression.ExpressionContext
import com.printscript.parser.expression.PrefixParselet
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.flatMap
import com.printscript.token.Token

internal class ExpressionParser(
    private val cursor: TokenCursor,
    private val parselets: List<PrefixParselet>,
) {
    private val context = Context()

    fun parse(): Result<Expression> = parseExpression(0)

    /**
     * parses an expression while respecting the minimum operator precedence
     */
    private fun parseExpression(minPrecedence: Int): Result<Expression> =
        parsePrimary().flatMap { left ->
            parseBinaryExpression(left, minPrecedence)
        }

    /**
     * builds binary expressions according to operator precedence
     */
    private fun parseBinaryExpression(
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

            when (val right = parseExpression(precedence)) {
                is Failure -> return right

                is Success -> {
                    currentLeft =
                        Expression.BinaryExpression(
                            left = currentLeft,
                            operator = operator,
                            right = right.value,
                            start = currentLeft.start,
                            end = right.value.end,
                        )
                }
            }
        }

        return Success(currentLeft)
    }

    /**
     * parses the atomic expressions that can appear before a binary operator
     */
    private fun parsePrimary(): Result<Expression> {
        val token =
            cursor.consume()
                ?: return unexpectedEndOfExpression(cursor)

        val parselet =
            parselets.firstOrNull { it.matches(token) }
                ?: return unexpectedToken(token)

        return parselet.parse(token, context)
    }

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

    private inner class Context : ExpressionContext {
        override val cursor: TokenCursor
            get() = this@ExpressionParser.cursor

        override fun parseExpression(): Result<Expression> =
            this@ExpressionParser.parseExpression(0)
    }
}
