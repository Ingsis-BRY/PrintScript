package com.printscript.cli

import com.printscript.common.Position

/**
* reports parsing progress as statements come out of the stream.
* writes to a sink separate from the program output, so `println` results
* stay clean. defaults to standard error.
*/
class ProgressPrinter(private val sink: Appendable = System.err) {

    fun statementParsed(position: Position) {
        sink.appendLine("parsed statement at ${position.line}:${position.column}")
    }
}
