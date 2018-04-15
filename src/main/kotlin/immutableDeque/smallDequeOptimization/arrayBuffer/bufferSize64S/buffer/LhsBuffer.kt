package immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize64S.buffer

import immutableDeque.ImmutableDeque
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize64S.constants.*
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize64S.level.DequeBottomLevel

internal class LhsBuffer(array: Array<Any?>): ImmutableBuffer(array) {
    // MARK: ImmutableBuffer
    override fun withArray(array: Array<Any?>): ImmutableBuffer {
        if (array.isEmpty()) {
            return LhsEmptyBuffer
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
        for (i in this.size - 1 downTo 0) {
            this.addLeavesOfNode(this.array[i], list, depth)
        }
    }

    override fun getLeafValueAt(index: Int, depth: Int): Any? {
//        assert(index < this.size shl depth)

        val nodePosition = index shr depth
        val indexInNode = index - (nodePosition shl depth)
        val indexInArray = this.size - 1 - nodePosition
        return this.getLeafOfNodeAt(indexInNode, this.array[indexInArray], depth)
    }

    override fun setLeafValueAt(index: Int, value: Any?, depth: Int): ImmutableBuffer {
//        assert(index < this.size shl depth)

        val nodePosition = index shr depth
        val indexInNode = index - (nodePosition shl depth)
        val indexInArray = this.size - 1 - nodePosition
        val newNode = this.setLeafOfNodeAt(indexInNode, value, this.array[indexInArray], depth)

        val newArray = arrayOfNulls<Any?>(this.size)
        System.arraycopy(this.array, 0, newArray, 0, this.size)
        newArray[indexInArray] = newNode

        return LhsBuffer(newArray)
    }

    override fun popAndPushAllToNextLevelBuffer(count: Int, nextLevelBuffer: ImmutableBuffer): ImmutableBuffer {
//        assert(this.size % 2 == 0)

        val newArray = arrayOfNulls<Any?>(nextLevelBuffer.size + ((this.size - count) shr 1))
        System.arraycopy(nextLevelBuffer.array, 0, newArray, 0, nextLevelBuffer.size)

        var position = nextLevelBuffer.size

        for (i in 0 until this.size - count step 2) {
            newArray[position++] = Pair(this.array[i + 1], this.array[i])
        }
        return LhsBuffer(newArray)
    }

    override fun moveToUpperLevelBuffer(count: Int): ImmutableBuffer {
        val newArray = arrayOfNulls<Any?>(count shl 1)

        var position = 0
        val fromIndex = this.size - count

        for (i in fromIndex until this.size) {
            val pair = this.array[i] as Pair<*, *>
            newArray[position++] = pair.second
            newArray[position++] = pair.first
        }
        return LhsBuffer(newArray)
    }

    // MARK: ImmutableDeque

    override val first: Any?
        get() {
            return this.array[this.size - 1]
        }

    override val last: Any?
        get() {
            return this.array[0]
        }

    override fun addFirst(value: Any?): ImmutableDeque<Any?> {
        if (this.size + 1 < MAX_BUFFER_SIZE) {
            return this.push(value)
        }

//        println(this.size + 1 - lastRegularizationSize)
//        lastRegularizationSize = this.size + 1

        val toMoveToRhs = FULL_BUFFER_DEQUE_SHOULD_MOVE_TO_OPPOSITE_SIDE
        val toLeaveForLhs = this.size - toMoveToRhs

        val rhsArray = arrayOfNulls<Any?>(toMoveToRhs)
        var position = 0
        for (i in toMoveToRhs - 1 downTo 0) {
            rhsArray[position++] = this.array[i]
        }
        val rhs = RhsBuffer(rhsArray)

        val lhsArray = arrayOfNulls<Any?>(toLeaveForLhs + 1)
        System.arraycopy(this.array, toMoveToRhs, lhsArray, 0, toLeaveForLhs)
        lhsArray[toLeaveForLhs] = value
        val lhs = LhsBuffer(lhsArray)

        return DequeBottomLevel(lhs, rhs)
    }

    override fun removeFirst(): ImmutableDeque<Any?> {
        return this.pop()
    }

    override fun addLast(value: Any?): ImmutableDeque<Any?> {
        val newArray = arrayOf(value)
        val rhs = RhsBuffer(newArray)
        return DequeBottomLevel(this, rhs)
    }

    override fun removeLast(): ImmutableDeque<Any?> {
        return this.removeBottomAndMoveRestToOppositeSideBuffer()
    }
}
