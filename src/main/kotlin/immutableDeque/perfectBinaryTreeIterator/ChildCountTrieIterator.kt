package immutableDeque.perfectBinaryTreeIterator

internal class ChildCountTrieIterator<out T>(private val logChildCount: Int) : ListIterator<T> {
    private var index = 0
    private var size = 0
    private val path = arrayOfNulls<Any?>(32)
    private var height = 0
    private var isInRightEdge = false

    fun setTree(root: Any?, depth: Int, index: Int) {
//        assert(depth >= 0 && index in 0..(1 shl depth))

        this.index = index
        this.size = 1 shl (depth * logChildCount)
        this.path[0] = root
        this.height = depth
        this.isInRightEdge = index == this.size

        fillPath(index - if (isInRightEdge) 1 else 0, 1)
    }

    private fun fillPath(index: Int, startLevel: Int) {
        var shift = (height - startLevel) * logChildCount
        for (i in startLevel until height) {
            path[i] = (path[i - 1] as Array<Any?>)[indexAtShift(index, shift)]
            shift -= logChildCount
        }
    }

    private fun fillPathIfNeeded(indexPredicate: Int) {
        var shift = 0
        while (indexAtShift(index, shift) == indexPredicate) {
            shift += logChildCount
        }

        if (shift > 0) {
            val level = height - 1 - shift / logChildCount
            fillPath(index, level + 1)
        }
    }

    private fun elementAtCurrentIndex(): T {
        if (height == 0) { return path[0] as T }
        val leafBufferIndex = index and childCountMinusOne()
        return (path[height - 1] as Array<T>)[leafBufferIndex]
    }

    private fun indexAtShift(index: Int, shift: Int): Int {
        return (index shr shift) and childCountMinusOne()
    }

    private fun childCountMinusOne(): Int {
        return (1 shl logChildCount) - 1
    }

    override fun hasNext(): Boolean {
        return index < size
    }

    override fun hasPrevious(): Boolean {
        return index > 0
    }

    override fun nextIndex(): Int {
        return index
    }

    override fun previousIndex(): Int {
        return index - 1
    }

    override fun next(): T {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        val result = elementAtCurrentIndex()
        index += 1

        if (index == size) {
            isInRightEdge = true
            return result
        }

        fillPathIfNeeded(0)

        return result
    }

    override fun previous(): T {
        if (!hasPrevious()) {
            throw NoSuchElementException()
        }

        index -= 1

        if (isInRightEdge) {
            isInRightEdge = false
            return elementAtCurrentIndex()
        }

        fillPathIfNeeded(childCountMinusOne())

        return elementAtCurrentIndex()
    }
}