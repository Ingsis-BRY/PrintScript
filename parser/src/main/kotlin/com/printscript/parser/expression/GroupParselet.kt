package com.printscript.parser.expression

import com.printscript.ast.Expression
import com.printscript.common.Position
import com.printscript.parser.ParsingSupport.parseRightParen
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.flatMap
import com.printscript.token.Token

object GroupParselet : PrefixParselet {
    override fun matches(token: Token): Boolean = token is Token.LeftParenToken

    override fun parse(
        token: Token,
        context: ExpressionContext,
    ): Result<Expression> =
        parselet<Token.LeftParenToken>(token) { open ->
            context.parseExpression().flatMap { expression ->
                parseRightParen(context.cursor).flatMap { close ->
                    Success(spanning(expression, open.start, close.end))
                }
            }
        }

    private fun spanning(
        expression: Expression,
        start: Position,
        end: Position,
    ): Expression =
        when (expression) {
            is Expression.NumberLiteral -> expression.copy(start = start, end = end)
            is Expression.StringLiteral -> expression.copy(start = start, end = end)
            is Expression.BooleanLiteral -> expression.copy(start = start, end = end)
            is Expression.VariableReference -> expression.copy(start = start, end = end)
            is Expression.BinaryExpression -> expression.copy(start = start, end = end)
            is Expression.FunctionCall -> expression.copy(start = start, end = end)
        }
}
