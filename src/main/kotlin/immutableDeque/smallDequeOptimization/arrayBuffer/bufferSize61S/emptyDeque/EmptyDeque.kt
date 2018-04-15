package immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize61S.emptyDeque

import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize61S.buffer.LhsEmptyBuffer
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize61S.buffer.RhsEmptyBuffer
import immutableDeque.ImmutableDeque

internal object EmptyDeque: ImmutableDeque<Any?> {
    override val size: Int = 0
    override val first: Any? = null
    override val last: Any? = null
    override fun isEmpty(): Boolean = true

    override fun addFirst(value: Any?): ImmutableDeque<Any?> {
        return LhsEmptyBuffer.addFirst(value)
    }

    override fun removeFirst(): ImmutableDeque<Any?> {
        throw NoSuchElementException()
    }

    override fun addLast(value: Any?): ImmutableDeque<Any?> {
        return RhsEmptyBuffer.addLast(value)
    }

    override fun removeLast(): ImmutableDeque<Any?> {
        throw NoSuchElementException()
    }

    override fun toList(): List<Any?> {
        return emptyList()
    }

    override fun get(index: Int): Any? {
        throw IndexOutOfBoundsException()
    }

    override fun set(index: Int, value: Any?): ImmutableDeque<Any?> {
        throw IndexOutOfBoundsException()
    }
}

fun <T> emptyDeque(): ImmutableDeque<T> {
    return EmptyDeque as ImmutableDeque<T>
}
