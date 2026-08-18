package com.printscript.lexer.recognizer

import com.printscript.common.Position
import com.printscript.token.Token

/**
 * Recognizes identifiers: `[a-zA-Z_][a-zA-Z0-9_]*`.
 *
 * The automaton has two states. The first character must open the identifier;
 * every later one must be able to continue it, and the lexeme is accepted at
 * each step because an identifier of any length is complete on its own.
 *
 * The alphabet is ASCII on purpose, so `Char.isLetter` is not used: it would
 * also admit accented and non-latin letters.
 *
 * Reserved words match this shape too. They stay separate recognizers and win
 * the tie by being registered first in [TokenRecognizers].
 */
object IdentifierRecognizer : TokenRecognizer {

    override fun recognize(lexeme: String): RecognizerState =
        when {
            lexeme.isEmpty() -> RecognizerState.Pending
            !opensIdentifier(lexeme.first()) -> RecognizerState.Rejected
            lexeme.drop(1).all { continuesIdentifier(it) } -> RecognizerState.Accepted
            else -> RecognizerState.Rejected
        }

    override fun tokenOf(lexeme: String, start: Position, end: Position): Token =
        Token.IdentifierToken(lexeme, start, end)

    private fun opensIdentifier(value: Char): Boolean =
        value in 'a'..'z' || value in 'A'..'Z' || value == '_'

    private fun continuesIdentifier(value: Char): Boolean =
        opensIdentifier(value) || value in '0'..'9'
}
