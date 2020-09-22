package com.sourcegraph.lsifjava.visitor

import com.github.javaparser.Position
import com.github.javaparser.Range
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.AnnotationDeclaration
import com.github.javaparser.ast.body.AnnotationMemberDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.MethodReferenceExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.resolution.declarations.ResolvedDeclaration
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.*
import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration

import com.sourcegraph.lsifjava.Emitter
import com.sourcegraph.lsifjava.DocumentIndexer
import com.sourcegraph.lsifjava.DefinitionMeta

import java.util.Optional

public class LSIFJavaVisitor(val emitter: Emitter, val currIndexer: DocumentIndexer, val indexers: Map<String, DocumentIndexer>): VoidVisitorAdapter<Void>() {

    override fun visit(n: MethodCallExpr, arg: Void) {
        super.visit(n, arg)
        emitUse(n, n.scope.get().calculateResolvedType())
        emitUse(n, n.resolve())
    }

    //override fun 

    private fun emitDefinition(n: Node) {
        val range = getRange(n).orElseThrow {
            RuntimeException("Unexpected range-less AST node: " + n.toString())
        }

        if(!currIndexer.definitions.containsKey(range)) return

        val hoverId = emitter.emitVertex("hoverResult", mapOf(
            "result" to mapOf(
                "contents" to mapOf(
                    "language" to "java",
                    "value" to n.toString()
                )
            )
        ))

        val resultSetId = emitter.emitVertex("resultSet", emptyMap())
        emitter.emitEdge("textDocument/hover", mapOf("outV" to resultSetId, "inV" to hoverId))
        val rangeId = emitter.emitVertex("range", createRange(range))
        emitter.emitEdge("next", mapOf("outV" to rangeId, "inV" to resultSetId))

        currIndexer.rangeIds += rangeId
        currIndexer.definitions[range] = DefinitionMeta(rangeId, resultSetId)
    }

    private fun emitUse(n: Node, type: ResolvedType) {
        val definitionNode = getNode(type) ?: return

        val defPathContainer = definitionNode.findCompilationUnit()
            .flatMap { it.getStorage() }
            .map { it.getPath().toString() }

        if(defPathContainer.isPresent() && defPathContainer.get() == currIndexer.pathname) {
            // emitDefinition
        } else {
            indexers.get(defPathContainer.get())!!.index()
        }

        emitUse(n, definitionNode, defPathContainer.orElse(currIndexer.pathname))
    }

    private fun emitUse(n: Node, decl: ResolvedDeclaration) {
        val definitionNode = getNode(decl) ?: return

        val defPathContainer = definitionNode.findCompilationUnit()
            .flatMap { it.getStorage() }
            .map { it.getPath().toString() }

        if(defPathContainer.isPresent() && defPathContainer.get() == currIndexer.pathname) {
            // emitDefinition
        } else {
            indexers.get(defPathContainer.get())!!.index()
        }

        emitUse(n, definitionNode, defPathContainer.orElse(currIndexer.pathname))        
    }

    private fun emitUse(n: Node, def: Node, defPath: String) {
        val indexer = indexers.get(defPath)!!

        val range = getRange(n).orElseThrow {
            RuntimeException("Unexpected range-less AST node: " + n.toString())
        }
        val defRange = getRange(def).orElseThrow {
            RuntimeException("Unexpected range-less AST node: " + def.toString())
        }

        val meta = indexer.definitions[defRange]!!

        val rangeId = emitter.emitVertex("range", createRange(range))
        emitter.emitEdge("next", mapOf("outV" to rangeId, "inV" to meta.resultSetId))

        meta.definitionResultId = meta.definitionResultId ?: run { 
            val resultId = emitter.emitVertex("definitionResult", emptyMap())
            emitter.emitEdge("textDocument/definition", mapOf("outV" to meta.resultSetId, "inV" to resultId))
            resultId
        }

        emitter.emitEdge("item", mapOf<String, Any>(
            "outV" to meta.definitionResultId!!,
            "inVs" to arrayOf(meta.rangeId),
            "document" to indexer.documentId
        ))

        currIndexer.rangeIds += rangeId

        var refRangesSet = meta.referenceRangeIds.getOrDefault(currIndexer.documentId, mutableSetOf())
        refRangesSet += rangeId
        meta.referenceRangeIds[currIndexer.documentId] = refRangesSet
    }

    private fun getNode(type: ResolvedType): Node? {
        val node = type.asReferenceType().typeDeclaration.orElse(null)?.let { 
            return getNode(it)
        }

        return node
    }

    private fun getNode(decl: ResolvedDeclaration): Node? = when {
        JavaParserClassDeclaration::class.java.isInstance(decl) -> JavaParserClassDeclaration::class.java.cast(decl).getWrappedNode()
        JavaParserConstructorDeclaration::class.java.isInstance(decl) -> JavaParserConstructorDeclaration::class.java.cast(decl).getWrappedNode()
        JavaParserEnumConstantDeclaration::class.java.isInstance(decl) -> JavaParserEnumConstantDeclaration::class.java.cast(decl).getWrappedNode()
        JavaParserEnumDeclaration::class.java.isInstance(decl) -> JavaParserEnumDeclaration::class.java.cast(decl).getWrappedNode()
        JavaParserFieldDeclaration::class.java.isInstance(decl) -> JavaParserFieldDeclaration::class.java.cast(decl).getWrappedNode()
        JavaParserInterfaceDeclaration::class.java.isInstance(decl) -> JavaParserInterfaceDeclaration::class.java.cast(decl).getWrappedNode()
        JavaParserMethodDeclaration::class.java.isInstance(decl) -> JavaParserMethodDeclaration::class.java.cast(decl).getWrappedNode()
        JavaParserParameterDeclaration::class.java.isInstance(decl) -> JavaParserParameterDeclaration::class.java.cast(decl).getWrappedNode()
        JavaParserSymbolDeclaration::class.java.isInstance(decl) -> JavaParserSymbolDeclaration::class.java.cast(decl).getWrappedNode()
        JavaParserTypeVariableDeclaration::class.java.isInstance(decl) -> JavaParserTypeVariableDeclaration::class.java.cast(decl).getWrappedNode()
        JavaParserVariableDeclaration::class.java.isInstance(decl) -> JavaParserVariableDeclaration::class.java.cast(decl).getWrappedNode()
        else -> null
    }

    private fun getRange(n: Node): Optional<Range> {
        var decl = n
        if(decl::class.java.isAssignableFrom(VariableDeclarationExpr::class.java)) {
            decl = (decl as VariableDeclarationExpr).getVariables().get(0)
        }

        if(decl::class.java.isAssignableFrom(FieldDeclaration::class.java)) {
            decl = (decl as FieldDeclaration).getVariables().get(0)
        }

        try {
            return (decl as NodeWithSimpleName<*>).name.range
        } catch(e: ClassCastException) {}

        return decl.range
    }

    private fun createRange(range: Range): Map<String, Any> = mapOf(
        "start" to mapOf(
            "line" to range.begin.line - 1,
            "character" to range.begin.column,
        ),
        "end" to mapOf(
            "line" to range.end.line - 1,
            "character" to range.end.column + 1,
        )
    )
}