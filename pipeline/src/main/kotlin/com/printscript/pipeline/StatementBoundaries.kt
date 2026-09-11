package com.printscript.pipeline

import com.printscript.token.Token

object StatementBoundaries {
    val V1_0: StatementBoundary = StatementBoundary { SemicolonScan(last = null) }

    val V1_1: StatementBoundary = StatementBoundary { BlockScan(depth = 0, last = null) }
}

private data class SemicolonScan(
    val last: Token?,
) : StatementScan {
    override fun take(token: Token): StatementScan = SemicolonScan(last = token)

    override fun endsStatement(next: Token?): Boolean = last is Token.SemicolonToken
}

private data class BlockScan(
    val depth: Int,
    val last: Token?,
) : StatementScan {
    override fun take(token: Token): StatementScan =
        BlockScan(depth = depthAfter(token), last = token)

    override fun endsStatement(next: Token?): Boolean =
        when (last) {
            is Token.SemicolonToken ->
                depth == 0

            is Token.RightBraceToken ->
                depth <= 0 && next !is Token.ElseToken

            else ->
                false
        }

    private fun depthAfter(token: Token): Int =
        when (token) {
            is Token.LeftBraceToken -> depth + 1
            is Token.RightBraceToken -> depth - 1
            else -> depth
        }
}
