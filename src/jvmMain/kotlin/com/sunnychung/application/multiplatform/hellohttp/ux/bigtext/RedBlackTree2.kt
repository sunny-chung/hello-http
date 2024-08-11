package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import com.williamfiset.algorithms.datastructures.balancedtree.RedBlackTree

interface DebuggableNode<T : Comparable<T>> {
    fun debugKey(): String
    fun debugLabel(node: RedBlackTree<T>.Node): String
}

interface RedBlackTreeComputations<T : Comparable<T>> {
    fun recomputeFromLeaf(it: RedBlackTree<T>.Node)
    fun computeWhenLeftRotate(x: T, y: T)
    fun computeWhenRightRotate(x: T, y: T)
}

open class RedBlackTree2<T>(private val computations: RedBlackTreeComputations<T>) : RedBlackTree<T>() where T : Comparable<T>, T : DebuggableNode<T> {

    fun lastNodeOrNull(): Node? {
        var child: Node = root
        while (child.getLeft().isNotNil() || child.getRight().isNotNil()) {
            child = child.getRight() ?: child.getLeft()
        }
        return child.takeIf { it.isNotNil() }
    }

    fun lastOrNull(): T? {
        return lastNodeOrNull()?.getValue()
    }

    fun find(comparison: (T) -> Int): T? {
        return findNode { comparison(it.value) }?.getValue()
    }

    fun findNode(comparison: (Node) -> Int): Node? {
        var child: Node = root
        while (child.isNotNil()) {
            val compareResult = comparison(child)
            if (compareResult == 0) {
                return child.takeIf { it.isNotNil() }
            } else if (compareResult > 0) {
                child = child.getRight()
            } else {
                child = child.getLeft()
            }
        }
        return null
    }

    inline fun Node.isLeaf(): Boolean = left.isNil() && right.isNil()

    @Deprecated("use `insertValue` instead")
    override fun insert(`val`: T): Boolean {
        throw UnsupportedOperationException()
    }

    fun insertValue(`val`: T): Node? {
        requireNotNull(`val`) { "Red-Black tree does not allow null values." }

        var x: Node = root
        var y: Node = NIL

        while (x !== NIL) {
            y = x

            x = if (x.getValue().compareTo(`val`) > 0) {
                x.left
            } else if (x.getValue().compareTo(`val`) < 0) {
                x.right
            } else {
                return null
            }
        }

        val z: Node = Node(`val`, RED, y, NIL, NIL)

        if (y === NIL) {
            root = z
        } else if (z.getValue().compareTo(y.getValue()) < 0) {
            y.left = z
        } else {
            y.right = z
        }
        insertFix(z)

        nodeCount++
        return z
    }

    /**
     *    parent             parent
     *     /  \              /  \
     *    a   b     ---->   a    b
     *                       \
     *                        z
     */
    fun insertLeft(parent: Node, value: T): Node {
        val z: Node = Node(value, RED, parent, NIL, NIL)
        if (root.isNil) {
            TODO("insertLeft root")
        }
        if (parent.left.isNil) {
            parent.left = z
        } else {
            val prevNode = rightmost(parent.left)
            prevNode.right = z
            z.parent = prevNode
        }
        insertFix(z)
        nodeCount++
        return z
    }

    /**
     *    parent             parent
     *     /  \              /  \
     *    a   b     ---->   a    b
     *                          /
     *                         z
     */
    fun insertRight(parent: Node, value: T): Node {
        val z: Node = Node(value, RED, parent, NIL, NIL)
        if (root.isNil) {
            TODO("insertRight root")
        }
        if (parent.right.isNil) {
            parent.right = z
        } else {
            val nextNode = leftmost(parent.right)
            nextNode.left = z
            z.parent = nextNode
        }
        insertFix(z)
        nodeCount++
        return z
    }

    override fun insertFix(z: Node) {
        computations.recomputeFromLeaf(z)
        super.insertFix(z)
    }

    fun leftmost(node: Node): Node {
        var node = node
        while (node.left.isNotNil()) {
            node = node.left
        }
        return node
    }

    fun rightmost(node: Node): Node {
        var node = node
        while (node.right.isNotNil()) {
            node = node.right
        }
        return node
    }

    /**
     *          parent           parent
     *          /                /
     *         y       <---     x
     *        / \              / \
     *       x   c            a   y
     *      / \                  / \
     *     a   b                b   c
     */
    override fun leftRotate(x: Node) {
        val y = x.right
        computations.computeWhenLeftRotate(x.value, y.value)
        super.leftRotate(x)
    }

    /**
     *          parent           parent
     *          /                /
     *         y       --->     x
     *        / \              / \
     *       x   c            a   y
     *      / \                  / \
     *     a   b                b   c
     */
    override fun rightRotate(y: Node) {
        val x = y.left
        computations.computeWhenRightRotate(x.value, y.value)
        super.rightRotate(y)
    }

//    fun visitUpwards(node: Node, visitor: (T) -> Unit) {
//        var node = node
//        while (node.isNotNil()) {
//            vi
//        }
//    }

    fun debugTree(prepend: String = "    "): String = buildString {
        fun visit(node: Node): String {
            val key = node.value?.debugKey().toString()
            if (node === root) {
                appendLine("$prepend$key[/\"${node.value?.debugLabel(node)}\"\\]")
            } else {
                appendLine("$prepend$key[\"${node.value?.debugLabel(node)}\"]")
            }
            node.left.takeIf { it.isNotNil() }?.also { appendLine("$prepend$key--L-->${visit(it)}") }
            node.right.takeIf { it.isNotNil() }?.also { appendLine("$prepend$key--R-->${visit(it)}") }
//            node.parent.takeIf { it.isNotNil() }?.also { appendLine("$prepend$key--P-->${node.parent.value.debugKey()}") }
            return key
        }
        visit(root)
    }
}

inline fun <T : Comparable<T>> RedBlackTree<T>.Node.isNotNil(): Boolean = !isNil()
