package buffer

import deque.ImmutableDeque
import level.DequeBottomLevel

internal class RhsBuffer<T>(top: Any?,
                            size: Int,
                            override val next: ImmutableBufferDeque<T>): AbstractBuffer(top, size, next), ImmutableBufferDeque<T> {
    // MARK: ImmutableBuffer
    override fun push(value: Any?): RhsBuffer<T> {
        return RhsBuffer(value, this.size + 1, this)
    }

    override fun addLeafValuesTo(list: MutableList<Any?>, depth: Int) {
        this.pop().addLeafValuesTo(list, depth)
        this.addLeavesOfNode(this.top, list, depth)
    }

    private fun addLeavesOfNode(node: Any?, list: MutableList<Any?>, height: Int) {
        if (height == 0) {
            list.add(node)
            return
        }
        val pair = node as Pair<*, *>
        this.addLeavesOfNode(pair.second, list, height - 1)
        this.addLeavesOfNode(pair.first, list, height - 1)
    }

    override fun getLeafValueAt(index: Int, depth: Int): Any? {
        assert(index < this.size shl depth)

        val leavesAtNext = this.next.size shl depth

        if (index >= leavesAtNext) {
            return this.getLeafOfNodeAt(index - leavesAtNext, this.top, depth)
        }
        return this.next.getLeafValueAt(index, depth)
    }

    override fun setLeafValueAt(index: Int, value: Any?, depth: Int): LhsBuffer<T> {
        assert(index < this.size shl depth)

        val leavesAtNext = this.next.size shl depth

        if (index >= leavesAtNext) {
            val newTop = this.setLeafOfNodeAt(index - leavesAtNext, value, this.top, depth)
            return LhsBuffer(newTop, this.size, this.next)
        }
        val newNext = this.next.setLeafValueAt(index, value, depth) as LhsBuffer<T>
        return newNext.push(this.top)
    }

    override fun pushAllToNextLevelBuffer(nextLevelBuffer: ImmutableBuffer): ImmutableBuffer {
        assert(this.size % 2 == 0)

        val result = (this.next as RhsBuffer).next.pushAllToNextLevelBuffer(nextLevelBuffer)
        val pair = Pair(this.next.top, this.top)
        return result.push(pair)
    }

    override fun moveToUpperLevelBuffer(count: Int): ImmutableBuffer {
        if (count == 0) {
            return RhsEmptyBuffer
        }
        val result = this.next.moveToUpperLevelBuffer(count - 1)
        val pair = this.top as Pair<*, *>
        return result.push(pair.first).push(pair.second)
    }

    // MARK: AbstractBuffer
    override fun empty(): ImmutableBuffer {
        return RhsEmptyBuffer
    }

    override fun oppositeSideEmpty(): ImmutableBuffer {
        return LhsEmptyBuffer
    }

    // MARK: ImmutableDeque
    override fun isEmpty(): Boolean {
        return false
    }

    override val first: T
        get() {
            return this.pop(this.size - 1).top as T
        }

    override val last: T
        get() {
            return this.top as T
        }

    override fun addFirst(value: T): ImmutableDeque<T> {
        val lhs = LhsEmptyBuffer.push(value)
        return DequeBottomLevel(lhs, this)
    }

    override fun removeFirst(): ImmutableDeque<T> {
        return this.removeBottomAndMoveRestToOppositeSideBuffer() as ImmutableDeque<T>
    }

    override fun addLast(value: T): ImmutableDeque<T> {
        if (this.size + 1 < MAX_BUFFER_SIZE) {
            return this.push(value)
        }

        val toMoveToLhs = MAX_BUFFER_SIZE shr 1
        val toLeaveForRhs = this.size - toMoveToLhs

        val lhs = this.pop(toLeaveForRhs).moveToOppositeSideBuffer()
        val rhs = this.removeBottom(toMoveToLhs).push(value)

        return DequeBottomLevel(lhs, rhs)
    }

    override fun removeLast(): ImmutableDeque<T> {
        return this.next
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