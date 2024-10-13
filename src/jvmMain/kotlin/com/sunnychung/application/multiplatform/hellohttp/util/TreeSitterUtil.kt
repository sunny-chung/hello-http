package com.sunnychung.application.multiplatform.hellohttp.util

import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Point

fun Pair<Int, Int>.toPoint(): Point = Point(first.toUInt(), second.toUInt())

class VisitScope(private val node: Node, private val visitor: VisitScope.(Node) -> Unit) {
    fun visit(anotherNode: Node) = anotherNode.visit(visitor)

    fun visitChildrens() {
        node.children.forEach {
            visit(it)
        }
    }
}

fun Node.visit(visitor: VisitScope.(Node) -> Unit) {
    val scope = VisitScope(this, visitor)
    scope.visitor(this)
}
