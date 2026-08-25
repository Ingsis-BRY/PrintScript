package com.printscript.pipeline

import com.printscript.report.Result
import com.printscript.token.Token

fun interface TokenSource {
    fun tokens(): Sequence<Result<Token>>
}
