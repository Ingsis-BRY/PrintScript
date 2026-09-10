package com.printscript.parser.expression

import com.printscript.ast.Expression
import com.printscript.language.NumberCodec
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.map
import com.printscript.token.Token

object NumberLiteralParselet : PrefixParselet {
    override fun matches(token: Token): Boolean = token is Token.NumberLiteralToken

    override fun parse(
        token: Token,
        context: ExpressionContext,
    ): Result<Expression> =
        parselet<Token.NumberLiteralToken>(token) { literal ->
            NumberCodec
                .parse(text = literal.value, span = literal.span)
                .map { value ->
                    Expression.NumberLiteral(value, literal.start, literal.end)
                }
        }
}

object StringLiteralParselet : PrefixParselet {
    override fun matches(token: Token): Boolean = token is Token.StringLiteralToken

    override fun parse(
        token: Token,
        context: ExpressionContext,
    ): Result<Expression> =
        parselet<Token.StringLiteralToken>(token) { literal ->
            Success(Expression.StringLiteral(literal.value, literal.start, literal.end))
        }
}

object BooleanLiteralParselet : PrefixParselet {
    override fun matches(token: Token): Boolean = token is Token.BooleanLiteralToken

    override fun parse(
        token: Token,
        context: ExpressionContext,
    ): Result<Expression> =
        parselet<Token.BooleanLiteralToken>(token) { literal ->
            Success(Expression.BooleanLiteral(literal.value, literal.start, literal.end))
        }
}

object VariableReferenceParselet : PrefixParselet {
    override fun matches(token: Token): Boolean = token is Token.IdentifierToken

    override fun parse(
        token: Token,
        context: ExpressionContext,
    ): Result<Expression> =
        parselet<Token.IdentifierToken>(token) { name ->
            Success(Expression.VariableReference(name.lexeme, name.start, name.end))
        }
}
