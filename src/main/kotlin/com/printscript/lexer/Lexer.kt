package com.printscript.lexer

import com.printscript.common.Diagnostic
import com.printscript.common.Failure
import com.printscript.common.Position
import com.printscript.common.Result
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
 * Errors are reported as [Failure]; nothing is thrown.
 */
class Lexer(
    private val reader: SourceReader,
    private val recognizers: List<TokenRecognizer> = TokenRecognizers.DEFAULT
) {

    private val pending: ArrayDeque<PositionedChar> = ArrayDeque()

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

        val start = peekChar()?.position ?: return null

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

            val states = alive.map { recognizer -> recognizer to recognizer.recognize(lexeme.toString()) }

            val accepted = states.firstOrNull { (_, state) -> state == RecognizerState.Accepted }

            if (accepted != null) {
                best = Match(accepted.first, lexeme.toString(), current.position)
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
     * reports a lexeme no recognizer could accept, at the position it started on
     *
     * a recognizer that owns how the lexeme starts gets to name the problem;
     * otherwise the first character is simply not one a token can begin with
     *
     * that character stays consumed so the scan always makes progress
     */
    private fun malformedToken(
        lexeme: String,
        consumed: List<PositionedChar>,
        start: Position
    ): Failure {
        pushBack(consumed.drop(1))

        val diagnosis = recognizers.firstNotNullOfOrNull { it.diagnose(lexeme) }
            ?: "Unexpected character '${consumed.first().value}'"

        return Failure(
            Diagnostic(
                message = diagnosis,
                position = start
            )
        )
    }

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
