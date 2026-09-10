package com.printscript.parser

import com.printscript.ast.Statement
import com.printscript.parser.expression.PrefixParselet
import com.printscript.parser.syntax.StatementSyntax
import com.printscript.report.Result
import com.printscript.token.Token

class Parser(
    private val syntaxes: List<StatementSyntax>,
    private val parselets: List<PrefixParselet>,
) {
    /**
     * parses a list of tokens into a statement
     */
    fun parse(tokens: List<Token>): Result<Statement> =
        StatementParser(TokenCursor(tokens), syntaxes, parselets).parse()
}
