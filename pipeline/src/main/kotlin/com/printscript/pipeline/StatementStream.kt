package com.printscript.pipeline

import com.printscript.ast.Statement
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.token.Token

class StatementStream(
    source: TokenSource,
    private val parser: StatementParser,
    private val boundary: StatementBoundary,
) {
    private val tokens = source.tokens().iterator()

    private var pending: Result<Token>? = null

    /**
     * whether the source still holds tokens to form a statement
     */
    fun hasNext(): Boolean = pending != null || tokens.hasNext()

    fun next(): Result<Statement> {
        val batch = mutableListOf<Token>()
        var scan = boundary.scan()

        while (true) {
            when (val result = take() ?: break) {
                is Failure -> return result

                is Success -> {
                    batch.add(result.value)
                    scan = scan.take(result.value)

                    if (scan.endsStatement(peek())) {
                        return parser.parse(batch)
                    }
                }
            }
        }

        return parser.parse(batch)
    }

    private fun take(): Result<Token>? {
        pending?.let {
            pending = null
            return it
        }

        return if (tokens.hasNext()) tokens.next() else null
    }

    private fun peek(): Token? {
        if (pending == null && tokens.hasNext()) {
            pending = tokens.next()
        }

        return (pending as? Success)?.value
    }
}
