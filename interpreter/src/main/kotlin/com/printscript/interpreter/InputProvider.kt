package com.printscript.interpreter

fun interface InputProvider {
    fun read(prompt: String): String?
}

object NoInput : InputProvider {
    override fun read(prompt: String): String? = null
}

object ConsoleInput : InputProvider {
    override fun read(prompt: String): String? = readlnOrNull()
}
