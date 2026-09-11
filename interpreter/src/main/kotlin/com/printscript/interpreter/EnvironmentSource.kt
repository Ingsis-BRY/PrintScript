package com.printscript.interpreter

fun interface EnvironmentSource {
    fun read(name: String): String?
}

object SystemEnvironment : EnvironmentSource {
    override fun read(name: String): String? = System.getenv(name)
}
