package immutableDeque.perfectBinaryTreeIterator

internal class PerfectBinaryTreeIterator<out T> : ListIterator<T> {
    private var index = 0
    private var size = 0
    private val path = ArrayList<Any?>()
    private var isInRightEdge = false

    fun setTree(root: Any?, depth: Int, index: Int) {
//        assert(depth >= 0 && index in 0..(1 shl depth))

        this.index = index
        this.size = 1 shl depth
        this.path.clear()
        this.isInRightEdge = index == this.size

        this.fillPath(root, depth, index)
    }

    private fun fillPath(root: Any?, depth: Int, index: Int) {
//        assert(index >= 0 && index < 1 shl depth)

        var node = root
        var nodeDepth = depth
        var indexInNode = index

        path.add(node)
        while (nodeDepth > 0) {
            nodeDepth -= 1

            node = if (indexInNode < 1 shl nodeDepth) {
                (node as Pair<*, *>).first
            } else {
                indexInNode -= 1 shl nodeDepth
                (node as Pair<*, *>).second
            }

            path.add(node)
        }
    }

    override fun hasNext(): Boolean {
        return index < size
    }

    override fun hasPrevious(): Boolean {
        return index > 0
    }

    override fun next(): T {
        if (!hasNext()) throw NoSuchElementException()

        val result = path.last() as T
        index += 1

        if (index < size) {
            movePathToNext()
        } else {
            isInRightEdge = true
        }

        return result
    }

    private fun movePathToNext() {
        val node = path.removeAt(path.size - 1)
        val parent = path.last() as Pair<*, *>

        if (node === parent.first) {
            path.add(parent.second)
        } else {
            movePathToNext()
            path.add((path.last() as Pair<*, *>).first)
        }
    }

    override fun nextIndex(): Int {
        return index
    }

    override fun previous(): T {
        if (!hasPrevious()) throw NoSuchElementException()
        index -= 1

        if (!isInRightEdge) {
            movePathToPrevious()
        }
        isInRightEdge = false

        return path.last() as T
    }

    private fun movePathToPrevious() {
        val node = path.removeAt(path.size - 1)
        val parent = path.last() as Pair<*, *>

        if (node === parent.second) {
            path.add(parent.first)
        } else {
            movePathToPrevious()
            path.add((path.last() as Pair<*, *>).second)
        }
    }

    override fun previousIndex(): Int {
        return index - 1
    }
}