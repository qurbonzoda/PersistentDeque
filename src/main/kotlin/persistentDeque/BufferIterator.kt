package persistentDeque

import buffer.*

internal class BufferIterator(buffer: Buffer, index: Int): ListIterator<Any> {
    private val iterator: ListIterator<Any>

    init {
        assert(index in 0..buffer.size)

        val list = when(buffer) {
            is EmptyBuffer -> arrayListOf()
            is BufferOfOne -> arrayListOf(buffer.e)
            is BufferOfTwo -> arrayListOf(buffer.e1, buffer.e2)
            is BufferOfThree -> arrayListOf(buffer.e1, buffer.e2, buffer.e3)
            is BufferOfFour -> arrayListOf(buffer.e1, buffer.e2, buffer.e3, buffer.e4)
            is BufferOfFive -> arrayListOf(buffer.e1, buffer.e2, buffer.e3, buffer.e4, buffer.e5)
        }
        iterator = list.listIterator(index)
    }

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun hasPrevious(): Boolean {
        return iterator.hasPrevious()
    }

    override fun next(): Any {
        return iterator.next()
    }

    override fun nextIndex(): Int {
        return iterator.nextIndex()
    }

    override fun previous(): Any {
        return iterator.previous()
    }

    override fun previousIndex(): Int {
        return iterator.previousIndex()
    }
}