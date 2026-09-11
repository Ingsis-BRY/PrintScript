package com.printscript.parser.syntax

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.parser.TokenCursor
import com.printscript.report.Result

interface ParsingContext {
    val cursor: TokenCursor

    fun parseExpression(): Result<Expression>

    fun parseStatement(): Result<Statement>
}
