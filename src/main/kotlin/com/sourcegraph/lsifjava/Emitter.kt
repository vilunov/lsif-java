package com.sourcegraph.lsifjava

import java.io.PrintWriter

import com.google.gson.Gson

class Emitter(val writer: PrintWriter) {
    private var nextId: Int = 1

    private val gson: Gson = Gson()

    public fun emitVertex(labelName: String, args: Map<String, Any>) = emit("vertex", labelName, args)

    public fun emitEdge(labelName: String, args: Map<String, Any>) = emit("edge", labelName, args)

    private fun emit(typeName: String, labelName: String, args: Map<String, Any>): String {
        val id = (nextId++).toString()

        val map = mutableMapOf<String, Any>(
            "id" to id,
            "type" to typeName,
            "label" to labelName
        )
        map.putAll(args)

        println(map)

        writer.println(gson.toJson(map))

        return id
    }

    public fun numElements() = nextId
}