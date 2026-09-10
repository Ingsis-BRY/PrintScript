package com.printscript.token

import com.printscript.common.Located
import com.printscript.common.Position

sealed interface Token : Located {
    val lexeme: String

    data class LetToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class IdentifierToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class TypeNameToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class NumberLiteralToken(
        override val lexeme: String,
        val value: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class StringLiteralToken(
        override val lexeme: String,
        val value: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class AssignToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class ColonToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class SemicolonToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class PlusToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class MinusToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class StarToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class SlashToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class LeftParenToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class RightParenToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class ConstToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class IfToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class ElseToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class BooleanLiteralToken(
        override val lexeme: String,
        val value: Boolean,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class LeftBraceToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token

    data class RightBraceToken(
        override val lexeme: String,
        override val start: Position,
        override val end: Position,
    ) : Token
}
