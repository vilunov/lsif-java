package com.sourcegraph.lsifjava

import java.nio.file.Paths

import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy
import com.github.javaparser.ParserConfiguration

data class Stats(var numFiles: Int, var numDefs: Int)

/**
 * do the do
 */
fun index(args: Arguments, emitter: Emitter, stats: Stats): Stats {
    val rootPath = Paths.get(args.projectRoot).toFile().canonicalPath

    emitter.emitVertex("metaData", mapOf(
        "version" to "0.4.0",
        "positionEncoding" to "utf-16",
        "projectRoot" to "file://"+rootPath
    ))

    val projectId = emitter.emitVertex("project", mapOf("kind" to "java"))

    val indexers = HashMap<String, DocumentIndexer>()

    val parserConfig = ParserConfiguration().apply {
        languageLevel = ParserConfiguration.LanguageLevel.BLEEDING_EDGE
    }

    val projectRoot = SymbolSolverCollectionStrategy(parserConfig).collect(Paths.get(rootPath))

    for(sr in projectRoot.sourceRoots) {
        try {
            sr.tryToParse()
        } catch(e: Exception) {
            e.printStackTrace()
        }

        for(cu in sr.compilationUnits) {
            val pathname = cu.storage.get().path.toAbsolutePath().toString()
            indexers.put(pathname, DocumentIndexer(
                pathname,
                projectId,
                emitter,
                indexers,
                cu
            ))
        }
    }

    indexers.values.forEach { it.index() }

    indexers.values.forEach {  }

    indexers.values.forEach {
        stats.numFiles++
        stats.numDefs += it.numDefinitions()
    }

    return stats
}