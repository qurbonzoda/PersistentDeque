package buffer

import deque.ImmutableDeque
import level.DequeBottomLevel

internal class LhsBuffer<T>(top: Any?,
                            size: Int,
                            override val next: ImmutableBufferDeque<T>): AbstractBuffer(top, size, next), ImmutableBufferDeque<T> {
    // MARK: ImmutableBuffer
    override fun push(value: Any?): LhsBuffer<T> {
        return LhsBuffer(value, this.size + 1, this)
    }

    override fun addLeafValuesTo(list: MutableList<Any?>, depth: Int) {
        this.addLeavesOfNode(this.top, list, depth)
        this.pop().addLeafValuesTo(list, depth)
    }

    override fun getLeafValueAt(index: Int, depth: Int): Any? {
        assert(index < this.size shl depth)

        val leavesCount = 1 shl depth

        if (index < leavesCount) {
            return this.getLeafOfNodeAt(index, this.top, depth)
        }
        return this.next.getLeafValueAt(index - leavesCount, depth)
    }

    override fun setLeafValueAt(index: Int, value: Any?, depth: Int): LhsBuffer<T> {
        assert(index < this.size shl depth)

        val leavesCount = 1 shl depth

        if (index < leavesCount) {
            val newTop = this.setLeafOfNodeAt(index, value, this.top, depth)
            return LhsBuffer(newTop, this.size, this.next)
        }
        val newNext = this.next.setLeafValueAt(index - leavesCount, value, depth) as LhsBuffer<T>
        return newNext.push(this.top)
    }

    override fun pushAllToNextLevelBuffer(nextLevelBuffer: ImmutableBuffer): ImmutableBuffer {
        assert(this.size % 2 == 0)

        val result = (this.next as LhsBuffer).next.pushAllToNextLevelBuffer(nextLevelBuffer)
        val pair = Pair(this.top, this.next.top)
        return result.push(pair)
    }

    override fun moveToUpperLevelBuffer(count: Int): ImmutableBuffer {
        if (count == 0) {
            return this.empty()
        }
        val result = this.next.moveToUpperLevelBuffer(count - 1)
        val pair = this.top as Pair<*, *>
        return result.push(pair.second).push(pair.first)
    }

    // MARK: AbstractBuffer
    override fun empty(): ImmutableBuffer {
        return LhsEmptyBuffer
    }

    override fun oppositeSideEmpty(): ImmutableBuffer {
        return RhsEmptyBuffer
    }

    // MARK: ImmutableDeque
    override fun isEmpty(): Boolean {
        return false
    }

    override val first: T
        get() {
            return this.top as T
        }

    override val last: T
        get() {
            return this.pop(this.size - 1).top as T
        }

    override fun addFirst(value: T): ImmutableDeque<T> {
        if (this.size + 1 < MAX_BUFFER_SIZE) {
            return this.push(value)
        }

        val toMoveToRhs = MAX_BUFFER_SIZE shr 1
        val toLeaveForLhs = this.size - toMoveToRhs

        val rhs = this.pop(toLeaveForLhs).moveToOppositeSideBuffer()
        val lhs = this.removeBottom(toMoveToRhs).push(value)

        return DequeBottomLevel(lhs, rhs)
    }

    override fun removeFirst(): ImmutableDeque<T> {
        return this.pop() as ImmutableDeque<T>
    }

    override fun addLast(value: T): ImmutableDeque<T> {
        val rhs = this.oppositeSideEmpty().push(value)
        return DequeBottomLevel(this, rhs)
    }

    override fun removeLast(): ImmutableDeque<T> {
        return this.removeBottomAndMoveRestToOppositeSideBuffer() as ImmutableDeque<T>
    }

    override fun toList(): List<T> {
        val list = mutableListOf<Any?>()
        this.addLeafValuesTo(list, 0)
        return list.toList() as List<T>
    }

    override fun get(index: Int): T {
        assert(index < this.size)
        return this.getLeafValueAt(index, 0) as T
    }

    override fun set(index: Int, value: T): ImmutableDeque<T> {
        assert(index < this.size)
        return this.setLeafValueAt(index, value, 0)
    }
}