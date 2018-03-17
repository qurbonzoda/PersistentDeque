package immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize9S.persistentDeque

import immutableDeque.perfectBinaryTreeIterator.PerfectBinaryTreeIterator
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize9S.buffer.ImmutableBuffer

internal class PersistentDequeIterator<out T>(
        levelIterator: LevelIterator, private var index: Int, private val size: Int
) : ListIterator<T> {
    private val treesIterator: ListIterator<Any?>
    private val depthsIterator: ListIterator<Int>

    private var treeIterator: PerfectBinaryTreeIterator<T>? = null
    private var isTreeIteratorResultOfNext: Boolean = false

    init {
//        assert(index in 0..size)

        val rhsBuffers = ArrayList<ImmutableBuffer>()

        val trees = ArrayList<Any?>()
        val depths = ArrayList<Int>()

        var depth = 0

        while (levelIterator.hasNext()) {
            val level = levelIterator.next()

            rhsBuffers.add(level.rhs)

            level.lhs.addLeafValuesTo(trees, 0)
            addDepths(depths, level.lhs, depth)

            depth += 1
        }
        rhsBuffers.reverse()
        for (rhs in rhsBuffers) {
            depth -= 1

            rhs.addLeafValuesTo(trees, 0)
            addDepths(depths, rhs, depth)
        }

        treesIterator = trees.listIterator()
        depthsIterator = depths.listIterator()

        var lIndex = index

        while (treesIterator.hasNext()) {
            val nextTree = treesIterator.next()
            val nextDepth = depthsIterator.next()

            if (lIndex < 1 shl nextDepth) {
                treeIterator = PerfectBinaryTreeIterator(nextTree, nextDepth, lIndex)
                isTreeIteratorResultOfNext = true
                break
            }

            lIndex -= 1 shl nextDepth
        }
    }

    private fun addDepths(list: ArrayList<Int>, buffer: ImmutableBuffer, depth: Int) {
        repeat(times = buffer.size) {
            list.add(depth)
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

        index += 1

        if (!treeIterator!!.hasNext()) {
            setNextToTreeIterator()
        }
        return treeIterator!!.next()
    }

    override fun nextIndex(): Int {
        return index
    }

    override fun previous(): T {
        if (!hasPrevious()) throw NoSuchElementException()

        index -= 1

        if (treeIterator?.hasPrevious() != true) {
            setPreviousToTreeIterator()
        }
        return treeIterator!!.previous()
    }

    override fun previousIndex(): Int {
        return index - 1
    }

    private fun setNextToTreeIterator() {
        if (!isTreeIteratorResultOfNext) {
            treesIterator.next()
            depthsIterator.next()
        }

        val nextTree = treesIterator.next()
        val nextDepth = depthsIterator.next()
        treeIterator = PerfectBinaryTreeIterator(nextTree, nextDepth, 0)
        isTreeIteratorResultOfNext = true
    }

    private fun setPreviousToTreeIterator() {
        if (isTreeIteratorResultOfNext) {
            treesIterator.previous()
            depthsIterator.previous()
        }

        val previousTree = treesIterator.previous()
        val previousDepth = depthsIterator.previous()
        treeIterator = PerfectBinaryTreeIterator(previousTree, previousDepth, 1 shl previousDepth)
        isTreeIteratorResultOfNext = false
    }
}