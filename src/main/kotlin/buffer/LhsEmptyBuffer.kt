package buffer

import deque.ImmutableDeque

internal object LhsEmptyBuffer: ImmutableBufferDeque<Any?> {
    // MARK: ImmutableBuffer
    override val top: Any?
        get() = throw UnsupportedOperationException()
    override val color: Int = RED
    override val size: Int = 0

    override fun push(value: Any?): ImmutableBuffer {
        return LhsBuffer<Any?>(value, 1, this)
    }

    override fun pop(count: Int): ImmutableBuffer {
        assert(count == 0)
        return this
    }

    override fun removeBottom(count: Int): ImmutableBuffer {
        assert(count == 0)
        return this
    }

    override fun addLeafValuesTo(list: MutableList<Any?>, depth: Int) {
        // do nothing
    }

    override fun getLeafValueAt(index: Int, depth: Int): Any? {
        throw IndexOutOfBoundsException()
    }

    override fun setLeafValueAt(index: Int, value: Any?, depth: Int): ImmutableBuffer {
        throw IndexOutOfBoundsException()
    }

    override fun pushAllToNextLevelBuffer(nextLevelBuffer: ImmutableBuffer): ImmutableBuffer {
        return nextLevelBuffer
    }

    override fun moveToUpperLevelBuffer(count: Int): ImmutableBuffer {
        assert(count == 0)
        return this
    }

    override fun prependSavingOrder(buffer: ImmutableBuffer): ImmutableBuffer {
        return buffer
    }

    override fun removeBottomAndMoveRestToOppositeSideBuffer(): ImmutableBuffer {
        throw UnsupportedOperationException()
    }

    override fun moveToOppositeSideBuffer(): ImmutableBuffer {
        return RhsEmptyBuffer
    }

    // MARK: ImmutableDeque
    override fun isEmpty(): Boolean = true
    override val first: Any? = null
    override val last: Any? = null

    override fun addFirst(value: Any?): ImmutableDeque<Any?> {
        return LhsBuffer(value, 1, this)
    }

    override fun removeFirst(): ImmutableDeque<Any?> {
        throw NoSuchElementException()
    }

    override fun addLast(value: Any?): ImmutableDeque<Any?> {
        return LhsBuffer(value, 1, this)
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