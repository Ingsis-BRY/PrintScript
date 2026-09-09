package com.printscript.parser.syntax

import com.printscript.ast.Expression
import com.printscript.parser.TokenCursor
import com.printscript.report.Result

interface ParsingContext {
    val cursor: TokenCursor

    fun parseExpression(): Result<Expression>
}
