package com.printscript.parser.expression

import com.printscript.ast.Expression
import com.printscript.report.Result
import com.printscript.token.Token

interface PrefixParselet {
    fun matches(token: Token): Boolean

    fun parse(
        token: Token,
        context: ExpressionContext,
    ): Result<Expression>
}
