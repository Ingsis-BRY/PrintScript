package com.printscript.cli

import com.printscript.report.Result

interface Analyzing : AutoCloseable {
    fun analyze(): Result<Unit>
}
