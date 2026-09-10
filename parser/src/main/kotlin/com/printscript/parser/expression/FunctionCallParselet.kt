package com.printscript.parser.expression

import com.printscript.ast.Expression
import com.printscript.parser.ParsingSupport.parseLeftParen
import com.printscript.parser.ParsingSupport.parseRightParen
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.flatMap
import com.printscript.token.Token

class FunctionCallParselet(
    private val name: String,
) : PrefixParselet {
    override fun matches(token: Token): Boolean =
        token is Token.IdentifierToken && token.lexeme == name

    override fun parse(
        token: Token,
        context: ExpressionContext,
    ): Result<Expression> =
        parselet<Token.IdentifierToken>(token) { callee ->
            parseLeftParen(context.cursor).flatMap {
                context.parseExpression().flatMap { argument ->
                    parseRightParen(context.cursor).flatMap { close ->
                        Success(
                            Expression.FunctionCall(
                                callee = callee.lexeme,
                                argument = argument,
                                start = callee.start,
                                end = close.end,
                            ),
                        )
                    }
                }
            }
        }
}
