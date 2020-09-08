package com.sourcegraph.lsifjava.projectmodel

import com.github.javaparser.utils.CollectionStrategy
import com.github.javaparser.utils.ProjectRoot
import com.github.javaparser.ParserConfiguration

import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.model.Model

import java.nio.file.Paths
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.Optional
import java.io.File
import java.io.IOException
import java.io.FileReader

public class PomCollectionStrategy: CollectionStrategy {
    private val parserConfig: ParserConfiguration

    constructor(config: ParserConfiguration) {
        this.parserConfig = config
    }

    override fun getParserConfiguration() = parserConfig

    override fun collect(path: Path): ProjectRoot? {
        val root = ProjectRoot(path, parserConfig)
        PomModel(path, null, root)

        return root
    }


    override fun getRoot(file: Path): Optional<Path> {
        return Optional.empty()
    }

    override fun getPathMatcher(pattern: String): PathMatcher? {
        return null
    }
}