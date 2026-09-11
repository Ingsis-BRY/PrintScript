package com.printscript.pipeline

import com.printscript.token.Token

fun interface StatementBoundary {
    fun scan(): StatementScan
}

interface StatementScan {
    fun take(token: Token): StatementScan

    fun endsStatement(next: Token?): Boolean
}
