package com.sourcegraph.lsifjava.projectmodel

public interface ProjectModel {
    val parent: ProjectModel?

    val name: String
}