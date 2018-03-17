package immutableDeque.smallDequeOptimization.stackBuffer.bufferSize32.buffer

import immutableDeque.ImmutableDeque
import immutableDeque.smallDequeOptimization.stackBuffer.bufferSize32.constants.*
import immutableDeque.smallDequeOptimization.stackBuffer.bufferSize32.level.DequeBottomLevel

internal class RhsBuffer(top: Any?,
                         size: Int,
                         override val next: ImmutableBuffer): ImmutableBuffer(top, size, next) {
    // MARK: ImmutableBuffer
    override fun push(value: Any?): RhsBuffer {
        return RhsBuffer(value, this.size + 1, this)
    }

    override fun addLeafValuesTo(list: MutableList<Any?>, depth: Int) {
        this.next.addLeafValuesTo(list, depth)
        this.addLeavesOfNode(this.top, list, depth)
    }

    override fun getLeafValueAt(index: Int, depth: Int): Any? {
//        assert(index < this.size shl depth)

        val leavesAtNext = this.next.size shl depth
        if (index >= leavesAtNext) {
            return this.getLeafOfNodeAt(index - leavesAtNext, this.top, depth)
        }
        return this.next.getLeafValueAt(index, depth)
    }

    override fun setLeafValueAt(index: Int, value: Any?, depth: Int): RhsBuffer {
//        assert(index < this.size shl depth)

        val leavesAtNext = this.next.size shl depth
        if (index >= leavesAtNext) {
            val newTop = this.setLeafOfNodeAt(index - leavesAtNext, value, this.top, depth)
            return RhsBuffer(newTop, this.size, this.next)
        }

        val newNext = this.next.setLeafValueAt(index, value, depth) as RhsBuffer
        return newNext.push(this.top)
    }

    override fun pushAllToNextLevelBuffer(nextLevelBuffer: ImmutableBuffer): ImmutableBuffer {
//        assert(this.size % 2 == 0)

        val result = (this.next as RhsBuffer).next.pushAllToNextLevelBuffer(nextLevelBuffer)
        val pair = Pair(this.next.top, this.top)
        return result.push(pair)
    }

    override fun moveToUpperLevelBuffer(count: Int): ImmutableBuffer {
        if (count == 0) {
            return this.empty()
        }
        val result = this.next.moveToUpperLevelBuffer(count - 1)
        val pair = this.top as Pair<*, *>
        return result.push(pair.first).push(pair.second)
    }

    override fun empty(): ImmutableBuffer {
        return RhsEmptyBuffer
    }

    override fun oppositeSideEmpty(): ImmutableBuffer {
        return LhsEmptyBuffer
    }

    // MARK: ImmutableDeque
    override val first: Any?
        get() {
            if (this.size == 1) {
                return this.top
            }
            return this.pop(this.size - 1).top
        }

    override val last: Any?
        get() {
            return this.top
        }

    override fun addFirst(value: Any?): ImmutableDeque<Any?> {
        val lhs = this.oppositeSideEmpty().push(value)
        return DequeBottomLevel(lhs, this)
    }

    override fun removeFirst(): ImmutableDeque<Any?> {
        return this.removeBottomAndMoveRestToOppositeSideBuffer()
    }

    override fun addLast(value: Any?): ImmutableDeque<Any?> {
        if (this.size + 1 < MAX_BUFFER_SIZE) {
            return this.push(value)
        }

//        println(this.size + 1 - lastRegularizationSize)
//        lastRegularizationSize = this.size + 1

        val toMoveToLhs = FULL_BUFFER_DEQUE_SHOULD_MOVE_TO_OPPOSITE_SIDE
        val toLeaveForRhs = this.size - toMoveToLhs

        val lhs = this.pop(toLeaveForRhs).moveToOppositeSideBuffer()
        val rhs = this.removeBottom(toMoveToLhs).push(value)

        return DequeBottomLevel(lhs, rhs)
    }

    override fun removeLast(): ImmutableDeque<Any?> {
        return this.pop()
    }
}
