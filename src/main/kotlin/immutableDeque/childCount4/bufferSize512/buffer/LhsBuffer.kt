package immutableDeque.childCount4.bufferSize512.buffer

import immutableDeque.ImmutableDeque
import immutableDeque.childCount4.CHILD_COUNT
import immutableDeque.childCount4.bufferSize512.constants.FULL_BUFFER_DEQUE_SHOULD_MOVE_TO_OPPOSITE_SIDE
import immutableDeque.childCount4.bufferSize512.constants.MAX_BUFFER_SIZE
import immutableDeque.childCount4.bufferSize512.level.DequeBottomLevel
import immutableDeque.childCount4.childCountToThePow

internal class LhsBuffer(top: Any?,
                         size: Int,
                         override val next: ImmutableBuffer) : ImmutableBuffer(top, size, next) {
    // MARK: ImmutableBuffer
    override fun push(value: Any?): LhsBuffer {
        return LhsBuffer(value, this.size + 1, this)
    }

    override fun addLeafValuesTo(list: MutableList<Any?>, depth: Int) {
        this.addLeavesOfNode(this.top, list, depth)
        this.next.addLeafValuesTo(list, depth)
    }

    override fun getLeafValueAt(index: Int, depth: Int): Any? {
//        assert(index < this.size * childCountToThePow(depth))

        val leavesCount = childCountToThePow(depth)
        if (index < leavesCount) {
            return this.getLeafOfNodeAt(index, this.top, depth)
        }
        return this.next.getLeafValueAt(index - leavesCount, depth)
    }

    override fun setLeafValueAt(index: Int, value: Any?, depth: Int): ImmutableBuffer {
//        assert(index < this.size * childCountToThePow(depth))

        val leavesCount = childCountToThePow(depth)

        if (index < leavesCount) {
            val newTop = this.setLeafOfNodeAt(index, value, this.top, depth)
            return LhsBuffer(newTop, this.size, this.next)
        }
        val newNext = this.next.setLeafValueAt(index - leavesCount, value, depth)
        return newNext.push(this.top)
    }

    override fun pushAllToNextLevelBuffer(nextLevelBuffer: ImmutableBuffer): ImmutableBuffer {
//        assert(this.size % 2 == 0)

        val nextLevelTop = arrayOfNulls<Any?>(CHILD_COUNT)
        var node: ImmutableBuffer = this
        repeat(CHILD_COUNT) {
            nextLevelTop[it] = node.top
            node = node.next!!
        }
        val result = node.pushAllToNextLevelBuffer(nextLevelBuffer)
        return result.push(nextLevelTop)
    }

    override fun moveToUpperLevelBuffer(count: Int): ImmutableBuffer {
        if (count == 0) {
            return this.empty()
        }
        val thisTop = this.top as Array<*>
        var result = this.next.moveToUpperLevelBuffer(count - 1)
        for (i in CHILD_COUNT - 1 downTo 0) {
            result = result.push(thisTop[i])
        }
        return result
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
