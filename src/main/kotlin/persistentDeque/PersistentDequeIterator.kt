package persistentDeque

import buffer.Buffer
import buffer.EmptyBuffer

internal class PersistentDequeIterator<T>(
        levelIterator: LevelIterator, private var index: Int, private val size: Int
): ListIterator<T> {
    private val buffersIterator: ListIterator<Buffer>
    private var isBufferIteratorResultOfNext: Boolean = false

    private val buffersCount: Int

    private var bufferIterator: BufferIterator? = null
    private var isTreeIteratorResultOfNext: Boolean = false

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
            val indexOfBuffer = buffersIterator.nextIndex()
            val buffer = buffersIterator.next()
            val depth = minOf(indexOfBuffer, buffersCount - indexOfBuffer - 1)

            if (lIndex < buffer.size shl depth) {
                setIterators(buffer, lIndex, depth)
                isBufferIteratorResultOfNext = true
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

        bufferIteratorWantsToAccessNext()

        if (bufferIterator!!.hasNext()) {
            treeIterator = PerfectBinaryTreeIterator(bufferIterator!!.next(), depthOfCurrentBuffer(), 0)
            return treeIterator!!.next()
        }

        bufferIteratorsWantsToAccessNext()

        skipNextEmptyBuffer()

        val nextBuffer = buffersIterator.next()
        setIterators(nextBuffer, 0, depthOfCurrentBuffer())

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

        bufferIteratorWantsToAccessPrevious()

        if (bufferIterator?.hasPrevious() == true) {
            val depth = depthOfCurrentBuffer()
            treeIterator = PerfectBinaryTreeIterator(bufferIterator!!.previous(), depth, 1 shl depth)
            return treeIterator!!.previous()
        }

        bufferIteratorsWantsToAccessPrevious()

        skipPreviousEmptyBuffer()

        val previousBuffer = buffersIterator.previous()
        setIteratorsToLast(previousBuffer, depthOfCurrentBuffer())

        return treeIterator!!.previous()
    }

    override fun previousIndex(): Int {
        return index - 1
    }

    private fun depthOfCurrentBuffer(): Int {
        val indexOfBuffer = if (isBufferIteratorResultOfNext) buffersIterator.previousIndex() else buffersIterator.nextIndex()
        return minOf(indexOfBuffer, buffersCount - indexOfBuffer - 1)
    }

    private fun setIterators(buffer: Buffer, index: Int, depth: Int) {
        assert(buffer !is EmptyBuffer)
        assert(index >= 0 && buffer.size shl depth > index)

        val indexInBuffer = index shr depth
        val indexInTree = index - (indexInBuffer shl depth)
        bufferIterator = BufferIterator(buffer, indexInBuffer)
        treeIterator = PerfectBinaryTreeIterator(bufferIterator!!.next(), depth, indexInTree)
        isTreeIteratorResultOfNext = true
    }

    private fun setIteratorsToLast(buffer: Buffer, depth: Int) {
        assert(buffer !is  EmptyBuffer)
        bufferIterator = BufferIterator(buffer, buffer.size)
        treeIterator = PerfectBinaryTreeIterator(bufferIterator!!.previous(), depth, 1 shl depth)
        isTreeIteratorResultOfNext = false
    }

    private fun bufferIteratorWantsToAccessNext() {
        if (!isTreeIteratorResultOfNext) {
            bufferIterator?.next()
        }
        isTreeIteratorResultOfNext = true
    }

    private fun bufferIteratorWantsToAccessPrevious() {
        if (isTreeIteratorResultOfNext) {
            bufferIterator?.previous()
        }
        isTreeIteratorResultOfNext = false
    }

    private fun bufferIteratorsWantsToAccessNext() {
        if (!isBufferIteratorResultOfNext) {
            buffersIterator.next()
        }
        isBufferIteratorResultOfNext = true
    }

    private fun bufferIteratorsWantsToAccessPrevious() {
        if (isBufferIteratorResultOfNext) {
            buffersIterator.previous()
        }
        isBufferIteratorResultOfNext = false
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