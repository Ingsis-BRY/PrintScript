package com.printscript.lexer

data class Position(
    val line: Int = 1,
    val column: Int = 1,
    val offset: Int = 0
)
