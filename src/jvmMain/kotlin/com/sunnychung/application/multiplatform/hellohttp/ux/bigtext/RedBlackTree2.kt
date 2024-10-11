package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import com.williamfiset.algorithms.datastructures.balancedtree.RedBlackTree
import java.util.Stack

interface DebuggableNode<T : Comparable<T>> {
    fun debugKey(): String
    fun debugLabel(node: RedBlackTree<T>.Node): String
    fun attach(node: RedBlackTree<T>.Node)
    fun detach()
}

interface RedBlackTreeComputations<T : Comparable<T>> {
    fun recomputeFromLeaf(it: RedBlackTree<T>.Node)
    fun computeWhenLeftRotate(x: T, y: T)
    fun computeWhenRightRotate(x: T, y: T)
//    fun transferComputeResultTo(from: T, to: T)
}

open class RedBlackTree2<T>(private val computations: RedBlackTreeComputations<T>) : RedBlackTree<T>() where T : Comparable<T>, T : DebuggableNode<in T> {

    fun getRoot() = root

    fun setRoot(node: RedBlackTree<T>.Node) {
        root = node
        var numNodes = 0
        forEach { ++numNodes }
        nodeCount = numNodes
    }

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
        (`val` as DebuggableNode<T>).attach(z)

        if (y === NIL) {
            root = z
        } else if (z.getValue().compareTo(y.getValue()) < 0) {
            y.left = z
        } else {
            y.right = z
        }
        insertFix(z)
        computations.recomputeFromLeaf(z)

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
        (value as DebuggableNode<T>).attach(z)
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
        computations.recomputeFromLeaf(z)
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
        (value as DebuggableNode<T>).attach(z)
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
        computations.recomputeFromLeaf(z)
        nodeCount++
        return z
    }

    override fun insertFix(z: Node) {
        computations.recomputeFromLeaf(z)
        super.insertFix(z)
        NIL.setParent(NIL)
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
     *
     * The original code is buggy. Override to fix.
     */
    override fun leftRotate(x: Node) {
        val y = x.right
        if (x.isNotNil() && y.isNotNil()) {
            computations.computeWhenLeftRotate(x.value, y.value)
        }

        // leftRotate
        x.setRight(y.getLeft())
        if (y.getLeft() !== NIL) y.getLeft().setParent(x)
        y.setParent(x.getParent())
        if (x.getParent() === NIL) root = y
        else if (x === x.getParent().getLeft()) x.getParent().setLeft(y)
        else x.getParent().setRight(y)
        y.setLeft(x)
        x.setParent(y)
        computations.recomputeFromLeaf(x)
    }

    /**
     *          parent           parent
     *          /                /
     *         y       --->     x
     *        / \              / \
     *       x   c            a   y
     *      / \                  / \
     *     a   b                b   c
     *
     * The original code is buggy. Override to fix.
     */
    override fun rightRotate(y: Node) {
        val x = y.left
        if (x.isNotNil() && y.isNotNil()) {
            computations.computeWhenRightRotate(x.value, y.value)
        }

        // rightRotate
        y.left = x.right
        if (x.right !== NIL) x.right.parent = y
        x.parent = y.parent
        if (y.parent === NIL) root = x
        else if (y === y.parent.left) y.parent.left = x
        else y.parent.right = x
        x.right = y
        y.parent = x
        computations.recomputeFromLeaf(y)
    }

    override fun delete(key: T): Boolean {
        throw UnsupportedOperationException()
    }

    /**
     * The original code is buggy. Override to fix.
     * Reference: microsoft/vscode-textbuffer
     */
    fun delete(node: Node) {
        var z: Node = node
        val x: Node
        var y: Node = z // temporary reference y
        var y_original_color: Boolean = y.getColor()

        if (z.getLeft() === NIL) {
            x = z.getRight()
        } else if (z.getRight() === NIL) {
            x = z.getLeft()
        } else {
            y = successor(z.getRight())
            x = y.getRight()
        }
        if (y === root) {
            root = x
            x.color = BLACK
            z.detach()
            root.parent = NIL
            computations.recomputeFromLeaf(x)
            nodeCount--
            return
        }

        y_original_color = y.getColor()
        if (y === y.parent.left) {
            y.parent.left = x
        } else {
            y.parent.right = x
        }

        if (y === z) {
            // x can be NIL, but it works with changing parent of x. lets set it back later, after deleteFix().
            x.setParent(y.getParent())
            computations.recomputeFromLeaf(x)
        } else {
            val w = y.getParent()
            if (y.getParent() === z) {
                x.setParent(y)
            } else {
                x.setParent(y.getParent())
            }
            computations.recomputeFromLeaf(x)
            y.setLeft(z.getLeft())
            y.setRight(z.getRight())
            y.setParent(z.getParent())
            y.setColor(z.getColor())

            if (z === root) {
                root = y
            } else {
                if (z === z.parent.left) {
                    z.parent.left = y
                } else {
                    z.parent.right = y
                }
            }

            if (y.left !== NIL) {
                y.left.parent = y
                computations.recomputeFromLeaf(y.getLeft())
            }
            if (y.right !== NIL) {
                y.right.parent = y
                computations.recomputeFromLeaf(y.getRight())
            }
            if (w !== NIL && w !== z) {
                computations.recomputeFromLeaf(w)
            }
        }
        computations.recomputeFromLeaf(y)
        z.detach()

        if (y_original_color == BLACK) deleteFix(x)
        NIL.setParent(NIL)
        nodeCount--
    }

    private fun delete_notUsed(node: Node) {
        var z: Node = node
        val x: Node
        var y: Node = z // temporary reference y
        var y_original_color: Boolean = y.getColor()

        if (z.getLeft() === NIL) {
            x = z.getRight()
            transplant(z, z.getRight())

            computations.recomputeFromLeaf(x)
        } else if (z.getRight() === NIL) {
            x = z.getLeft()
            transplant(z, z.getLeft())

            computations.recomputeFromLeaf(x)
        } else {
            y = successor(z.getRight())
            y_original_color = y.getColor()
            x = y.getRight()
            if (y.getParent() === z) {
                x.setParent(y)
                computations.recomputeFromLeaf(x)
            } else {
                transplant(y, y.getRight())
                y.setRight(z.getRight())
                y.getRight().setParent(y)
                computations.recomputeFromLeaf(y.getRight())
            }
            transplant(z, y)
            y.setLeft(z.getLeft())
            y.getLeft().setParent(y)
            y.setColor(z.getColor())

//            computations.transferComputeResultTo(z.getValue(), y.getValue())

            computations.recomputeFromLeaf(y.getLeft())
        }
        if (y_original_color == BLACK) deleteFix(x)
        nodeCount--
    }

    /**
     * The original code is buggy. Override to fix.
     */
    override fun transplant(u: Node, v: Node) {
        if (u.parent === NIL) {
            root = v
        } else if (u === u.parent.left) {
            u.parent.left = v
        } else {
            u.parent.right = v
        }
        if (u.left !== NIL) {
            u.left.parent = v
        }
        if (u.right !== NIL) {
            u.right.parent = v
        }
        if (v !== NIL) {
            v.parent = u.parent
        }
    }

    /**
     * The original code is buggy. Override to fix.
     * References: Bibeknam/algorithmtutorprograms and microsoft/vscode-textbuffer
     */
    override fun deleteFix(x: Node) {
        var x = x
        while (x !== root && x.getColor() == BLACK) {
            if (x === x.getParent().getLeft()) {
                var w: Node = x.getParent().getRight()
                if (w.getColor() == RED) {
                    w.setColor(BLACK)
                    x.getParent().setColor(RED)
                    leftRotate(x.parent)
                    w = x.getParent().getRight()
                }
                if (w.getLeft().getColor() == BLACK && w.getRight().getColor() == BLACK) {
                    w.setColor(RED)
                    x = x.getParent()
                    continue
                } else if (w.getRight().getColor() == BLACK) {
                    w.getLeft().setColor(BLACK)
                    w.setColor(RED)
                    rightRotate(w)
                    w = x.getParent().getRight()
                }
//                if (w.getRight().getColor() == RED) {
                    w.setColor(x.getParent().getColor())
                    x.getParent().setColor(BLACK)
                    w.getRight().setColor(BLACK)
                    leftRotate(x.getParent())
                    x = root
//                }
            } else {
                var w: Node = (x.getParent().getLeft())
                if (w.color == RED) {
                    w.color = BLACK
                    x.getParent().setColor(RED)
                    rightRotate(x.getParent())
                    w = x.getParent().getLeft()
                }
                if (w.right.color == BLACK && w.left.color == BLACK) {
                    w.color = RED
                    x = x.getParent()
                    continue
                } else if (w.left.color == BLACK) {
                    w.right.color = BLACK
                    w.color = RED
                    leftRotate(w)
                    w = (x.getParent().getLeft())
                }
//                if (w.left.color == RED) {
                    w.color = x.getParent().getColor()
                    x.getParent().setColor(BLACK)
                    w.left.color = BLACK
                    rightRotate(x.getParent())
                    x = root
//                }
            }
        }
        x.setColor(BLACK)
    }

    fun prevNode(node: Node): Node? {
        if (node.left.isNotNil()) {
            return rightmost(node.left)
        }
        var node = node
        var parent = node.parent
        while (parent.isNotNil() && parent.left === node) {
            node = parent
            parent = node.parent
        }
        return parent.takeIf { it.isNotNil() }
    }

    fun nextNode(node: Node): Node? {
        if (node.right.isNotNil()) {
            return leftmost(node.right)
        }
        var node = node
        var parent = node.parent
        while (parent.isNotNil() && parent.right === node) {
            node = parent
            parent = node.parent
        }
        return parent.takeIf { it.isNotNil() }
    }

//    fun visitUpwards(node: Node, visitor: (T) -> Unit) {
//        var node = node
//        while (node.isNotNil()) {
//            vi
//        }
//    }

    fun visitInPostOrder(visitor: (Node) -> Unit) {
        fun visit(node: Node) {
            if (node.isNil) return
            visit(node.left)
            visit(node.right)
            visitor(node)
        }
        visit(root)
    }

    fun pathUntilRoot(node: Node): List<Node> = buildList {
        if (node.isNil) {
            throw IllegalArgumentException("Given node does not exist")
        }
        var n = node
        while (n.isNotNil()) {
            add(n)
            n = n.parent
        }
    }

    fun lowestCommonAncestor(node1: Node, node2: Node): Node {
        val path1 = pathUntilRoot(node1)
        val path2 = pathUntilRoot(node2)
        var i1 = path1.lastIndex
        var i2 = path2.lastIndex
        while (i1 >= 0 && i2 >= 0) {
            if (path1[i1] !== path2[i2]) {
                return path1[i1 + 1]
            }
            --i1
            --i2
        }
        throw IllegalArgumentException("One or more given nodes do not belong to this tree")
    }

    fun debugTree(prepend: String = "    "): String = buildString {
        fun visit(node: Node): String {
            val nodeValue = node.value as DebuggableNode<T>?
            val key = nodeValue?.debugKey().toString()
            if (node === root) {
                appendLine("$prepend$key[/\"${nodeValue?.debugLabel(node)}\"\\]")
            } else {
                appendLine("$prepend$key[\"${nodeValue?.debugLabel(node)}\"]")
            }
            node.left.takeIf { it.isNotNil() }?.also { appendLine("$prepend$key--L-->${visit(it)}") }
            node.right.takeIf { it.isNotNil() }?.also { appendLine("$prepend$key--R-->${visit(it)}") }
//            node.parent.takeIf { it.isNotNil() }?.also { appendLine("$prepend$key--P-->${node.parent.value.debugKey()}") }
            return key
        }
        visit(root)
    }

    fun Node.detach() {
        value?.detach()
        parent = NIL
        left = NIL
        right = NIL
    }
}

inline fun <T : Comparable<T>> RedBlackTree<T>.Node.isNotNil(): Boolean = !isNil()
