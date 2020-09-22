package com.sourcegraph.lsifjava

public data class DefinitionMeta(val rangeId: String, val resultSetId: String) {
    public var definitionResultId: String? = null
    public val referenceRangeIds: MutableMap<String, Set<String>> = HashMap()
}