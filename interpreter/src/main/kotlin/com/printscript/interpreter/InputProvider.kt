package com.printscript.interpreter

fun interface InputProvider {
    fun read(): String?
}

object NoInput : InputProvider {
    override fun read(): String? = null
}

object ConsoleInput : InputProvider {
    override fun read(): String? = readlnOrNull()
}
