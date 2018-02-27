package deque

import buffer.LhsEmptyBuffer
import buffer.RhsEmptyBuffer

internal object EmptyDeque: ImmutableDeque<Any?> {
    override val size: Int = 0
    override val first: Any? = null
    override val last: Any? = null
    override fun isEmpty(): Boolean = true

    override fun addFirst(value: Any?): ImmutableDeque<Any?> {
        return LhsEmptyBuffer.addFirst(value)
    }

    override fun removeFirst(): ImmutableDeque<Any?> {
        throw UnsupportedOperationException()
    }

    override fun addLast(value: Any?): ImmutableDeque<Any?> {
        return RhsEmptyBuffer.addLast(value)
    }

    override fun removeLast(): ImmutableDeque<Any?> {
        throw UnsupportedOperationException()
    }

    override fun toList(): List<Any?> {
        return emptyList()
    }

    override fun get(index: Int): Any? {
        throw UnsupportedOperationException()
    }

    override fun set(index: Int, value: Any?): ImmutableDeque<Any?> {
        throw UnsupportedOperationException()
    }
}