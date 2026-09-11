package com.printscript.parser.expression

import com.printscript.ast.Expression
import com.printscript.parser.TokenCursor
import com.printscript.report.Result

interface ExpressionContext {
    val cursor: TokenCursor

    fun parseExpression(): Result<Expression>
}
