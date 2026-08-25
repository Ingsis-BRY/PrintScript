package com.printscript.pipeline

import com.printscript.ast.Statement
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.token.Token

/**
* composes a token source and a parser into a source that hands over one
* [Statement] at a time. tokens are pulled lazily and grouped up to the
* closing `;`, so the source is never buffered whole: a single statement
* lives in memory at once.
*/
class StatementStream(
    source: TokenSource,
    private val parser: StatementParser
) {

    private val tokens = source.tokens().iterator()

    /**
    * whether the source still holds tokens to form a statement
    */
    fun hasNext(): Boolean = tokens.hasNext()

    /**
    * reads tokens up to and including the next `;` and parses them.
    * a lexical [Failure] is handed over as is, stopping at the first error.
    */
    fun next(): Result<Statement> {
        val batch = mutableListOf<Token>()

        while (tokens.hasNext()) {
            when (val result = tokens.next()) {
                is Failure -> return result

                is Success -> {
                    batch.add(result.value)

                    if (result.value is Token.SemicolonToken) {
                        return parser.parse(batch)
                    }
                }
            }
        }

        // ran out of tokens before a `;`: let the parser report what is missing
        return parser.parse(batch)
    }
}
