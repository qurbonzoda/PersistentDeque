package buffer

import deque.ImmutableDeque
import level.DequeBottomLevel

internal class LhsBuffer(top: Any?,
                         size: Int,
                         override val next: ImmutableBuffer): ImmutableBuffer(top, size, next) {
    // MARK: ImmutableBuffer
    override fun push(value: Any?): LhsBuffer {
        return LhsBuffer(value, this.size + 1, this)
    }

    override fun addLeafValuesTo(list: MutableList<Any?>, depth: Int) {
        this.addLeavesOfNode(this.top, list, depth)
        this.next.addLeafValuesTo(list, depth)
    }

    override fun getLeafValueAt(index: Int, depth: Int): Any? {
//        assert(index < this.size shl depth)

        val leavesCount = 1 shl depth
        if (index < leavesCount) {
            return this.getLeafOfNodeAt(index, this.top, depth)
        }
        return this.next.getLeafValueAt(index - leavesCount, depth)
    }

    override fun setLeafValueAt(index: Int, value: Any?, depth: Int): ImmutableBuffer {
//        assert(index < this.size shl depth)

        val leavesCount = 1 shl depth

        if (index < leavesCount) {
            val newTop = this.setLeafOfNodeAt(index, value, this.top, depth)
            return LhsBuffer(newTop, this.size, this.next)
        }
        val newNext = this.next.setLeafValueAt(index - leavesCount, value, depth)
        return newNext.push(this.top)
    }

    override fun pushAllToNextLevelBuffer(nextLevelBuffer: ImmutableBuffer): ImmutableBuffer {
//        assert(this.size % 2 == 0)

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

    override fun empty(): ImmutableBuffer {
        return LhsEmptyBuffer
    }

    override fun oppositeSideEmpty(): ImmutableBuffer {
        return RhsEmptyBuffer
    }

    // MARK: ImmutableDeque

    override val first: Any?
        get() {
            return this.top
        }

    override val last: Any?
        get() {
            if (this.size == 1) {
                return this.top
            }
            return this.pop(this.size - 1).top
        }

    override fun addFirst(value: Any?): ImmutableDeque<Any?> {
        if (this.size + 1 < MAX_BUFFER_SIZE) {
            return this.push(value)
        }

//        println(this.size + 1 - lastRegularizationSize)
//        lastRegularizationSize = this.size + 1

        val toMoveToRhs = FULL_BUFFER_DEQUE_SHOULD_MOVE_TO_OPPOSITE_SIDE
        val toLeaveForLhs = this.size - toMoveToRhs

        val rhs = this.pop(toLeaveForLhs).moveToOppositeSideBuffer()
        val lhs = this.removeBottom(toMoveToRhs).push(value)

        return DequeBottomLevel(lhs, rhs)
    }

    override fun removeFirst(): ImmutableDeque<Any?> {
        return this.pop()
    }

    override fun addLast(value: Any?): ImmutableDeque<Any?> {
        val rhs = this.oppositeSideEmpty().push(value)
        return DequeBottomLevel(this, rhs)
    }

    override fun removeLast(): ImmutableDeque<Any?> {
        return this.removeBottomAndMoveRestToOppositeSideBuffer()
    }
}