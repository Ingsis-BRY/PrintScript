package com.printscript.parser.expression

import com.printscript.ast.Expression
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.token.Token

internal inline fun <reified T : Token> parselet(
    token: Token,
    parse: (T) -> Result<Expression>,
): Result<Expression> =
    if (token is T) {
        parse(token)
    } else {
        Failure(Diagnostic.UnexpectedToken(token.lexeme, token.span))
    }
