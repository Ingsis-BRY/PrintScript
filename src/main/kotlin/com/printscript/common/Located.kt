package com.printscript.common

/**
 * Anything that remembers where in the source it came from.
 *
 * Tokens, expressions and statements all already carried a [start] and an
 * [end]; naming that pair gives every one of them a [span] to hand to an
 * error, so no producer has to assemble one by hand.
 */
interface Located {

    val start: Position

    val end: Position

    val span: Span
        get() = Span(start, end)
}
