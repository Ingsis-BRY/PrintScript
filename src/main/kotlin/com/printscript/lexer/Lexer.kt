package com.printscript.lexer

import com.printscript.common.Diagnostic
import com.printscript.common.Failure
import com.printscript.common.Position
import com.printscript.common.Result
import com.printscript.common.Span
import com.printscript.common.Success
import com.printscript.lexer.recognizer.RecognizerState
import com.printscript.lexer.recognizer.TokenRecognizer
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.token.Token

/**
 * Turns a [SourceReader] into a lazy stream of tokens using maximal munch.
 *
 * Every recognizer is fed the same lexeme, one character at a time, until all
 * of them are rejected. The longest lexeme any recognizer accepted wins;
 * characters read past that point are pushed back and scanned again as part of
 * the next token. Only that overshoot is ever held in memory, so the source is
 * never buffered whole.
 *
 * Errors are reported as [Failure]; nothing is thrown, including the I/O
 * failures the reader surfaces as [SourceChar.Failed].
 */
class Lexer(
    private val reader: SourceReader,
    private val recognizers: List<TokenRecognizer> = TokenRecognizers.DEFAULT
) {

    private val pending: ArrayDeque<PositionedChar> = ArrayDeque()

    private var unreadFailure: Diagnostic? = null

    private var line: Int = 1
    private var column: Int = 1

    /**
     * lazily scans the source, stopping at the first error
     *
     * the sequence is single-use, since it consumes the underlying reader
     */
    fun tokens(): Sequence<Result<Token>> = sequence {
        while (true) {
            val token = nextToken() ?: break

            yield(token)

            if (token is Failure) {
                break
            }
        }
    }

    /**
     * scans one token, or returns null once the source holds nothing but whitespace
     */
    private fun nextToken(): Result<Token>? {
        skipWhitespace()

        val start = peekChar()?.position
            ?: return takeSourceFailure()

        return scanToken(start)
    }

    /**
     * runs every recognizer from [start] and keeps the longest accepted lexeme
     */
    private fun scanToken(start: Position): Result<Token> {
        val lexeme = StringBuilder()
        val consumed = mutableListOf<PositionedChar>()

        var alive = recognizers
        var best: Match? = null

        while (true) {
            val current = readChar() ?: break

            consumed.add(current)
            lexeme.append(current.value)

            val currentLexeme = lexeme.toString()

            val states = alive.map { recognizer -> recognizer to recognizer.recognize(currentLexeme) }

            val accepted = states.firstOrNull { (_, state) -> state == RecognizerState.Accepted }

            if (accepted != null) {
                best = Match(accepted.first, currentLexeme, current.position)
            }

            alive = states
                .filter { (_, state) -> state != RecognizerState.Rejected }
                .map { (recognizer, _) -> recognizer }

            if (alive.isEmpty()) {
                break
            }
        }

        if (best == null) {
            return malformedToken(lexeme.toString(), consumed, start)
        }

        pushBack(consumed.drop(best.lexeme.length))

        return Success(best.recognizer.tokenOf(best.lexeme, start, best.end))
    }

    /**
     * reports a lexeme no recognizer could accept, over the source it covers
     *
     * a source that broke outranks everything: the leftovers of a truncated read
     * are a consequence of the I/O failure, not a separate lexical problem
     *
     * a recognizer that owns how the lexeme starts gets to name the problem, and
     * the span then covers the whole attempt; otherwise the first character is
     * simply not one a token can begin with, and the span is that character
     *
     * that character stays consumed so the scan always makes progress
     */
    private fun malformedToken(
        lexeme: String,
        consumed: List<PositionedChar>,
        start: Position
    ): Failure {
        takeSourceFailure()?.let { return it }

        pushBack(consumed.drop(1))

        val fault = recognizers.firstNotNullOfOrNull { it.diagnose(lexeme) }
            ?: return Failure(
                Diagnostic.UnexpectedCharacter(
                    character = consumed.first().value,
                    span = Span.at(start)
                )
            )

        return Failure(
            Diagnostic.MalformedLexeme(
                fault = fault,
                span = Span(start, lastCharacterOf(consumed))
            )
        )
    }

    /**
     * the last position the broken lexeme actually covers
     *
     * the character that rejected it can be the line break that ended the line,
     * and a span must not run past the text it points at
     */
    private fun lastCharacterOf(consumed: List<PositionedChar>): Position =
        consumed.lastOrNull { !it.value.isWhitespace() }?.position
            ?: consumed.first().position

    /**
     * drops characters until the next one can start a token
     */
    private fun skipWhitespace() {
        while (true) {
            val current = readChar() ?: return

            if (!current.value.isWhitespace()) {
                pending.addFirst(current)
                return
            }
        }
    }

    /**
     * returns the next character without consuming it
     */
    private fun peekChar(): PositionedChar? {
        val current = readChar() ?: return null

        pending.addFirst(current)

        return current
    }

    /**
     * takes the next character from the pushback buffer, or from the source
     */
    private fun readChar(): PositionedChar? {
        if (pending.isNotEmpty()) {
            return pending.removeFirst()
        }

        return when (val sourceChar = reader.next()) {
            is SourceChar.Character -> {
                val current = PositionedChar(sourceChar.value, Position(line, column))
                advancePosition(sourceChar.value)
                current
            }

            is SourceChar.Failed -> {
                unreadFailure = Diagnostic.SourceUnreadable(
                    detail = sourceChar.message,
                    span = Span.at(Position(line, column))
                )
                null
            }

            SourceChar.EndOfSource -> null
        }
    }

    /**
     * returns characters to the front of the buffer, keeping their order
     *
     * they carry their original position, so re-reading them does not move the cursor
     */
    private fun pushBack(chars: List<PositionedChar>) {
        for (index in chars.indices.reversed()) {
            pending.addFirst(chars[index])
        }
    }

    /**
     * hands over the I/O failure that ended the source, once
     */
    private fun takeSourceFailure(): Failure? {
        val failure = unreadFailure ?: return null

        unreadFailure = null

        return Failure(failure)
    }

    /**
     * moves the cursor past [value], starting a new line on a line feed
     */
    private fun advancePosition(value: Char) {
        if (value == '\n') {
            line++
            column = 1
        } else {
            column++
        }
    }

    /**
     * a character paired with where it was read from
     */
    private data class PositionedChar(
        val value: Char,
        val position: Position
    )

    /**
     * the longest lexeme accepted so far, and who accepted it
     */
    private data class Match(
        val recognizer: TokenRecognizer,
        val lexeme: String,
        val end: Position
    )
}
