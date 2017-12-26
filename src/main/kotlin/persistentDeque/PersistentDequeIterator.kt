package persistentDeque

import buffer.Buffer
import buffer.EmptyBuffer

internal class PersistentDequeIterator<T>(
        levelIterator: LevelIterator, private var index: Int, private val size: Int
): ListIterator<T> {
    private val buffersIterator: ListIterator<Buffer>
    private val buffersCount: Int

    private var bufferIterator: BufferIterator? = null
    private var treeIterator: PerfectBinaryTreeIterator<T>? = null

    init {
        assert(index in 0..size)

        val lhsBuffers = ArrayList<Buffer>()
        val rhsBuffers = ArrayList<Buffer>()
        while (levelIterator.hasNext()) {
            val (lhs, rhs) = levelIterator.next()

            lhsBuffers.add(lhs)
            rhsBuffers.add(rhs)
        }
        rhsBuffers.reverse()
        lhsBuffers.addAll(rhsBuffers)

        buffersIterator = lhsBuffers.listIterator()
        buffersCount = lhsBuffers.size

        var lIndex = index

        while (buffersIterator.hasNext()) {
            val buffer = buffersIterator.next()
            val depth = depthOfCurrentBuffer()

            if (lIndex < buffer.size shl depth) {
                setIterators(buffer, lIndex, depth)
                break
            }

            lIndex -= buffer.size shl depth
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

        if (treeIterator!!.hasNext()) {
            return treeIterator!!.next()
        }
        if (bufferIterator!!.hasNext()) {
            treeIterator = PerfectBinaryTreeIterator(bufferIterator!!.next(), depthOfCurrentBuffer(), 0)
            return treeIterator!!.next()
        }

        skipNextEmptyBuffer()
        setIterators(buffersIterator.next(), 0, depthOfCurrentBuffer())

        return treeIterator!!.next()
    }

    override fun nextIndex(): Int {
        return index
    }

    override fun previous(): T {
        if (!hasPrevious()) throw NoSuchElementException()

        assert((treeIterator == null && bufferIterator == null)
                || (treeIterator != null && bufferIterator != null))

        index -= 1

        if (treeIterator?.hasPrevious() == true) {
            return treeIterator!!.previous()
        }

        val depth = depthOfCurrentBuffer()

        if (bufferIterator?.hasPrevious() == true) {
            treeIterator = PerfectBinaryTreeIterator(bufferIterator!!.previous(), depth, 1 shl depth)
            return treeIterator!!.previous()
        }

        skipPreviousEmptyBuffer()
        setIteratorsToLast(buffersIterator.previous(), depth)

        return treeIterator!!.previous()
    }

    override fun previousIndex(): Int {
        return index - 1
    }

    private fun depthOfCurrentBuffer(): Int {
        val indexOfBuffer = buffersIterator.nextIndex() - 1
        return minOf(indexOfBuffer, buffersCount - indexOfBuffer - 1)
    }

    private fun setIterators(buffer: Buffer, index: Int, depth: Int) {
        assert(buffer !is EmptyBuffer)
        assert(index >= 0 && buffer.size shl depth > index)

        val indexInBuffer = index shr depth
        val indexInTree = index - (indexInBuffer shl depth)
        bufferIterator = BufferIterator(buffer, indexInBuffer)
        treeIterator = PerfectBinaryTreeIterator(bufferIterator!!.next(), depth, indexInTree)
    }

    private fun setIteratorsToLast(buffer: Buffer, depth: Int) {
        assert(buffer !is  EmptyBuffer)
        bufferIterator = BufferIterator(buffer, buffer.size)
        treeIterator = PerfectBinaryTreeIterator(bufferIterator!!.previous(), depth, 1 shl depth)
    }

    private fun skipNextEmptyBuffer() {
        if (buffersIterator.next() is EmptyBuffer) {
            return
        }
        buffersIterator.previous()
    }

    private fun skipPreviousEmptyBuffer() {
        if (buffersIterator.previous() is EmptyBuffer) {
            return
        }
        buffersIterator.next()
    }
}