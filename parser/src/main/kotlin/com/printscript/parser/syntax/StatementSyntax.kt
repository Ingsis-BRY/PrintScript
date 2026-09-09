package com.printscript.parser.syntax

import com.printscript.ast.Statement
import com.printscript.report.Result
import com.printscript.token.Token

interface StatementSyntax {
    fun matches(token: Token): Boolean

    fun parse(context: ParsingContext): Result<Statement>
}
