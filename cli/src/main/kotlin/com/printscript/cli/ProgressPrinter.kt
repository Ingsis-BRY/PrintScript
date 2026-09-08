package com.printscript.cli

import com.printscript.common.Position

class ProgressPrinter(
    private val sink: Appendable,
) {
    fun statementParsed(position: Position) {
        sink.appendLine("parsed statement at ${position.line}:${position.column}")
    }
}
