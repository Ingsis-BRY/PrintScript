package com.printscript.parser

import com.printscript.ast.Statement
import com.printscript.report.Result
import com.printscript.token.Token

object Parser {
    /**
     * parses a list of tokens into a statement
     */
    fun parse(tokens: List<Token>): Result<Statement> = StatementParser(TokenCursor(tokens)).parse()
}
