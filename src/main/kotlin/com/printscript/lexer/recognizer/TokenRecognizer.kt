package com.printscript.lexer.recognizer

import com.printscript.common.Position
import com.printscript.token.Token

/**
 * A small finite automaton that recognizes a single kind of token.
 *
 * Recognizers know nothing about each other or about the source: they only
 * judge a lexeme. The lexer runs every recognizer from the same position and
 * keeps the longest [RecognizerState.Accepted] lexeme, breaking ties by the
 * order in [TokenRecognizers].
 *
 * Adding a token type means adding a recognizer, never editing an existing one.
 */
interface TokenRecognizer {

    /**
     * walks [lexeme] through the automaton and reports where it ended up
     */
    fun recognize(lexeme: String): RecognizerState

    /**
     * builds the token for a lexeme this recognizer has [RecognizerState.Accepted]
     *
     * [start] and [end] are the positions of the first and last character of
     * [lexeme], both inclusive
     */
    fun tokenOf(lexeme: String, start: Position, end: Position): Token

    /**
     * explains why [lexeme] is a broken attempt at this kind of token.
     *
     * The lexer asks only when no recognizer accepted anything, so a recognizer
     * that owns the way the lexeme starts can replace the generic
     * unexpected-character message with a precise one. Returning null, the
     * default, leaves the lexer with its own diagnosis.
     */
    fun diagnose(lexeme: String): String? = null
}
