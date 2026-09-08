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
        val previous = cursor.previous ?: return leadingGap(next)

        return if (previous is Token.SemicolonToken) {
            betweenStatements(previous, next, cursor.printlnStatement)
        } else {
            withinStatement(previous, next)
        }
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
            is Token.IdentifierToken,
            is Token.TypeNameToken,
            is Token.NumberLiteralToken,
            is Token.StringLiteralToken,
            is Token.AssignToken,
            is Token.ColonToken,
            is Token.SemicolonToken,
            is Token.LeftParenToken,
            is Token.RightParenToken,
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
) {
    fun advance(token: Token): Cursor =
        Cursor(
            previous = token,
            printlnStatement = if (opensStatement()) token.opensPrintln() else printlnStatement,
        )

    private fun opensStatement(): Boolean = previous == null || previous is Token.SemicolonToken

    companion object {
        val START = Cursor(previous = null, printlnStatement = false)
    }
}

private fun Token.opensPrintln(): Boolean = this is Token.IdentifierToken && lexeme == PRINTLN
