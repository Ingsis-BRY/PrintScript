package com.printscript.parser

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.report.Result
import com.printscript.token.Token

object Parser {
    /**
     * parses a list of tokens into a statement
     */
    fun parse(tokens: List<Token>): Result<Statement> = StatementParser(TokenCursor(tokens)).parse()

    /**
     * parses a list of tokens into an expression using Pratt parsing
     */
    fun parseExpression(tokens: List<Token>): Result<Expression> =
        ExpressionParser(TokenCursor(tokens)).parse()
}
