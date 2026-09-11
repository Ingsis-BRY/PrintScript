package com.printscript.formatter

import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.token.Token

private const val SPACE = " "
private const val LINE_BREAK = "\n"
private const val NOTHING = ""
private const val PRINTLN = "println"

private const val FIRST_LINE = 1
private const val BEFORE_FIRST_COLUMN = 0

class Formatter(
    private val config: Config,
) {
    fun format(
        tokens: Sequence<Result<Token>>,
        out: Appendable,
    ): Result<Unit> {
        var cursor = Cursor.START

        for (result in tokens) {
            when (result) {
                is Failure -> return result

                is Success -> {
                    out.append(gapBefore(cursor, result.value))
                    out.append(result.value.lexeme)
                    cursor = cursor.advance(result.value)
                }
            }
        }

        return Success(Unit)
    }

    private fun gapBefore(
        cursor: Cursor,
        next: Token,
    ): String {
        val previous = cursor.previous ?: return indented(leadingGap(next), cursor, next)

        val gap =
            when {
                previous is Token.SemicolonToken ->
                    betweenStatements(previous, next, cursor.printlnStatement)

                touchesBrace(previous, next) ->
                    aroundBraces(previous, next)

                else ->
                    withinStatement(previous, next)
            }

        return indented(gap, cursor, next)
    }

    private fun leadingGap(next: Token): String = originalGap(FIRST_LINE, BEFORE_FIRST_COLUMN, next)

    private fun betweenStatements(
        previous: Token,
        next: Token,
        afterPrintln: Boolean,
    ): String {
        val blanks = config.blankLinesAfterPrintln

        return when {
            afterPrintln && blanks != null -> LINE_BREAK.repeat(blanks + 1)
            config.lineBreakAfterStatement -> LINE_BREAK
            else -> originalGap(previous, next)
        }
    }

    private fun touchesBrace(
        previous: Token,
        next: Token,
    ): Boolean =
        previous is Token.LeftBraceToken ||
            previous is Token.RightBraceToken ||
            next is Token.LeftBraceToken ||
            next is Token.RightBraceToken

    private fun aroundBraces(
        previous: Token,
        next: Token,
    ): String =
        when {
            next !is Token.LeftBraceToken -> originalGap(previous, next)
            config.braceSameLineAsIf -> SPACE
            config.braceBelowIfLine -> LINE_BREAK
            else -> originalGap(previous, next)
        }

    private fun withinStatement(
        previous: Token,
        next: Token,
    ): String =
        when {
            config.singleSpaceSeparation && next is Token.SemicolonToken -> NOTHING
            config.spaceBeforeColon && next is Token.ColonToken -> SPACE
            config.spaceAfterColon && previous is Token.ColonToken -> SPACE
            assignmentRuleApplies(previous, next) -> assignmentGap()
            config.spaceAroundOperators && touchesOperator(previous, next) -> SPACE
            config.singleSpaceSeparation -> SPACE
            else -> originalGap(previous, next)
        }

    private fun indented(
        gap: String,
        cursor: Cursor,
        next: Token,
    ): String {
        val width = config.indentInsideBlock ?: return gap

        val lastBreak = gap.lastIndexOf(LINE_BREAK)

        if (lastBreak < 0) {
            return gap
        }

        val level =
            if (next is Token.RightBraceToken) {
                (cursor.depth - 1).coerceAtLeast(0)
            } else {
                cursor.depth
            }

        return gap.substring(0, lastBreak + 1) + SPACE.repeat(width * level)
    }

    private fun assignmentRuleApplies(
        previous: Token,
        next: Token,
    ): Boolean =
        (config.spaceAroundAssignment || config.noSpaceAroundAssignment) &&
            (previous is Token.AssignToken || next is Token.AssignToken)

    private fun assignmentGap(): String = if (config.spaceAroundAssignment) SPACE else NOTHING

    private fun touchesOperator(
        previous: Token,
        next: Token,
    ): Boolean = isOperator(previous) || isOperator(next)

    private fun isOperator(token: Token): Boolean =
        when (token) {
            is Token.PlusToken,
            is Token.MinusToken,
            is Token.StarToken,
            is Token.SlashToken,
            -> true

            is Token.LetToken,
            is Token.ConstToken,
            is Token.IfToken,
            is Token.ElseToken,
            is Token.IdentifierToken,
            is Token.TypeNameToken,
            is Token.NumberLiteralToken,
            is Token.StringLiteralToken,
            is Token.BooleanLiteralToken,
            is Token.AssignToken,
            is Token.ColonToken,
            is Token.SemicolonToken,
            is Token.LeftParenToken,
            is Token.RightParenToken,
            is Token.LeftBraceToken,
            is Token.RightBraceToken,
            -> false
        }

    private fun originalGap(
        previous: Token,
        next: Token,
    ): String = originalGap(previous.end.line, previous.end.column, next)

    private fun originalGap(
        endLine: Int,
        endColumn: Int,
        next: Token,
    ): String {
        val lines = next.start.line - endLine

        return if (lines == 0) {
            SPACE.repeat((next.start.column - endColumn - 1).coerceAtLeast(0))
        } else {
            LINE_BREAK.repeat(lines) + SPACE.repeat((next.start.column - 1).coerceAtLeast(0))
        }
    }
}

private data class Cursor(
    val previous: Token?,
    val printlnStatement: Boolean,
    val depth: Int,
) {
    fun advance(token: Token): Cursor =
        Cursor(
            previous = token,
            printlnStatement = if (opensStatement()) token.opensPrintln() else printlnStatement,
            depth = depthAfter(token),
        )

    private fun depthAfter(token: Token): Int =
        when (token) {
            is Token.LeftBraceToken -> depth + 1
            is Token.RightBraceToken -> (depth - 1).coerceAtLeast(0)
            else -> depth
        }

    private fun opensStatement(): Boolean =
        previous == null ||
            previous is Token.SemicolonToken ||
            previous is Token.LeftBraceToken ||
            previous is Token.RightBraceToken

    companion object {
        val START = Cursor(previous = null, printlnStatement = false, depth = 0)
    }
}

private fun Token.opensPrintln(): Boolean = this is Token.IdentifierToken && lexeme == PRINTLN
