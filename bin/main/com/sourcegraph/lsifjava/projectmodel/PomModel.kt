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

import kotlin.collections.HashSet

final class PomModel: ProjectModel {
    override val parent: ProjectModel?
    override lateinit var name: String

    private lateinit var pomFile: File
    private lateinit var model: Model

    constructor(path: Path, parent: PomModel?, project: ProjectRoot) {
        this.parent = parent
        init(path, project)
    }

    private fun init(path: Path, project: ProjectRoot) {
        collectSourceRoots(path, project)
    }

    private fun collectSourceRoots(path: Path, project: ProjectRoot) {
        var pathStr = path.toString()

        if ((!path.endsWith(".xml") && !path.endsWith(".pom") || path.toFile().isDirectory)) {
            pathStr = Paths.get(path.toString(), "pom.xml").toString()
        }

        pomFile = File(pathStr).canonicalFile
        if (!pomFile.exists()) {
            throw IOException("pom file at ${pathStr} does not exist")
        }

        val reader = MavenXpp3Reader();
        FileReader(pomFile).use { 
            model = reader.read(it)!!
            
            val allModules = HashSet<String>()

            for(profile in model.profiles) {
                for(module in profile.modules) {
                    allModules.add(module)
                    PomModel(Paths.get(pomFile.parent, module), this, project)
                }
            }

            for(module in model.getModules()) {
                if(module.contains(module)) continue
                PomModel(Paths.get(pomFile.parent, module), this, project)
            }
        }
    }

    // TODO: interpolate variables from properties etc
    private fun addSourceRoot(model: Model, project: ProjectRoot, parent: PomModel?) {
        val build = model.build
        var sourcePath: String? = null

        if(build != null) {
            sourcePath = build.sourceDirectory
        }

        sourcePath = sourcePath ?: (getSourceFromParent(parent) ?: Paths.get("src/main/java").toString())
    }

    private fun getSourceFromParent(model: PomModel?): String? {
        if(model == null) return null

        val build = model.model.build
        /* val sourceDir = if(build == null) {
            
        } */
        return null
    }
}