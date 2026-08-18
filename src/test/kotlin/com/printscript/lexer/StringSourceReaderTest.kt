package com.printscript.lexer

class StringSourceReaderTest : SourceReaderTest() {

    override fun readerOf(source: String): SourceReader {
        return StringSourceReader(source)
    }
}
