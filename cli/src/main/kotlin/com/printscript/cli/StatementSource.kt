package com.printscript.cli

import com.printscript.ast.Statement
import com.printscript.report.Result

interface StatementSource : AutoCloseable {
    fun hasNext(): Boolean

    fun next(): Result<Statement>
}
