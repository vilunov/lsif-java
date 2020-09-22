package com.sourcegraph.lsifjava

import com.sourcegraph.lsifjava.visitor.LSIFJavaVisitor

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.Range

import java.nio.file.Paths

val extensionToLang = mapOf(
    "kt" to "kotlin",
    "java" to "java",
    "scala" to "scala",
    "groovy" to "groovy"
)

/* val extensionToVisitor = mapOf(
    ".java" to LSIFJavaVisitor::class
)
 */
class DocumentIndexer {
    public val pathname: String
    private val projectId: String
    private val emitter: Emitter
    private val indexers: Map<String, DocumentIndexer>
    private val cu: CompilationUnit

    public val definitions: MutableMap<Range, DefinitionMeta> = HashMap()
    public val rangeIds: MutableSet<String> = HashSet()
    private var indexed = false
    public lateinit var documentId: String

    constructor(pathname: String, projectId: String, emitter: Emitter, indexers: Map<String, DocumentIndexer>, cu: CompilationUnit) {
        this.pathname = pathname
        this.projectId = projectId
        this.emitter = emitter
        this.indexers = indexers
        this.cu = cu
    }

    public fun numDefinitions() = definitions.size

    public fun index() {
        if(indexed) return

        doIndex()
        indexed = true
    }

    private fun doIndex() {
        documentId = emitter.emitVertex("document", mapOf(
            "languageId" to extensionToLang[Paths.get(pathname).toFile().extension]!!,
            "uri" to "file://"+Paths.get(pathname).toFile().canonicalPath
        ))

        cu.accept(LSIFJavaVisitor(emitter, this, indexers), null)
    }

    public fun postIndex() {
        for(meta in definitions.values) {

        }
    }

    private fun linkUses(meta: DefinitionMeta, documentId: String) {
        val resultId = emitter.emitVertex("referenceResult", emptyMap())

        emitter.emitEdge("textDocument/references", mapOf("outV" to meta.resultSetId, "inV" to resultId))
    }
}