package com.printscript.interpreter

/**
* where the interpreter sends the output of `println`.
* kept behind an interface so tests never touch process-global state.
*/
interface OutputEmitter {
    fun emit(line: String)
}

/**
* emits to the console, the real output used outside tests
*/
class ConsoleOutput : OutputEmitter {
    override fun emit(line: String) {
        println(line)
    }
}

/**
* accumulates every emitted line so tests can assert on them
*/
class CollectingOutput : OutputEmitter {
    private val emitted: MutableList<String> = mutableListOf()

    override fun emit(line: String) {
        emitted.add(line)
    }

    fun lines(): List<String> = emitted.toList()
}
