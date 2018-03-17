package immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize31S.buffer

import immutableDeque.ImmutableDeque

internal object LhsEmptyBuffer: ImmutableBuffer(arrayOfNulls(0)) {
    // MARK: ImmutableBuffer
    override fun withArray(array: Array<Any?>): ImmutableBuffer {
        if (array.isEmpty()) {
            return this
        }
        return LhsBuffer(array)
    }

    override fun oppositeSideBufferWithArray(array: Array<Any?>): ImmutableBuffer {
        if (array.isEmpty()) {
            return RhsEmptyBuffer
        }
        return RhsBuffer(array)
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

    override fun popAndPushAllToNextLevelBuffer(count: Int, nextLevelBuffer: ImmutableBuffer): ImmutableBuffer {
//        assert(count == 0)
        return nextLevelBuffer
    }

    override fun moveToUpperLevelBuffer(count: Int): ImmutableBuffer {
//        assert(count == 0)
        return this
    }

    override fun prependSavingOrder(buffer: ImmutableBuffer): ImmutableBuffer {
        return buffer
    }

    // MARK: ImmutableDeque
    override fun isEmpty(): Boolean = true
    override val first: Any? = null
    override val last: Any? = null

    override fun addFirst(value: Any?): ImmutableDeque<Any?> {
        return push(value)
    }

    override fun removeFirst(): ImmutableDeque<Any?> {
        throw NoSuchElementException()
    }

    override fun addLast(value: Any?): ImmutableDeque<Any?> {
        val newArray = arrayOf(value)
        return LhsBuffer(newArray)
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
