package com.printscript.pipeline

import com.printscript.ast.Statement
import com.printscript.report.Result
import com.printscript.token.Token

fun interface StatementParser {
    fun parse(tokens: List<Token>): Result<Statement>
}
