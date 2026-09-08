package com.printscript.cli

import com.printscript.report.Result

interface Formatting : AutoCloseable {
    fun format(): Result<Unit>
}
