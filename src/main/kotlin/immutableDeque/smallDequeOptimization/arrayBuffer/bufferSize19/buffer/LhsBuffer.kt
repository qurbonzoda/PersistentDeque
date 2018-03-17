package immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize19.buffer

import immutableDeque.ImmutableDeque
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize19.constants.*
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize19.level.DequeBottomLevel

internal class LhsBuffer(array: Array<Any?>,
                         size: Int): ImmutableBuffer(array, size) {
    // MARK: ImmutableBuffer
    override fun withArray(array: Array<Any?>, size: Int): ImmutableBuffer {
        if (size == 0) {
            return LhsEmptyBuffer
        }
        return LhsBuffer(array, size)
    }

    override fun oppositeSideBufferWithArray(array: Array<Any?>, size: Int): ImmutableBuffer {
        if (size == 0) {
            return RhsEmptyBuffer
        }
        return RhsBuffer(array, size)
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

        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        System.arraycopy(this.array, 0, newArray, 0, this.size)
        newArray[indexInArray] = newNode

        return LhsBuffer(newArray, this.size)
    }

    override fun popAndPushAllToNextLevelBuffer(count: Int, nextLevelBuffer: ImmutableBuffer): ImmutableBuffer {
//        assert(this.size % 2 == 0)

        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        System.arraycopy(nextLevelBuffer.array, 0, newArray, 0, nextLevelBuffer.size)

        var position = nextLevelBuffer.size

        for (i in 0 until this.size - count step 2) {
            newArray[position++] = Pair(this.array[i + 1], this.array[i])
        }
        return LhsBuffer(newArray, nextLevelBuffer.size + ((this.size - count) shr 1))
    }

    override fun moveToUpperLevelBuffer(count: Int): ImmutableBuffer {
        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)

        var position = 0
        val fromIndex = this.size - count

        for (i in fromIndex until this.size) {
            val pair = this.array[i] as Pair<*, *>
            newArray[position++] = pair.second
            newArray[position++] = pair.first
        }
        return LhsBuffer(newArray, count shl 1)
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

        val rhsArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        var position = 0
        for (i in toMoveToRhs - 1 downTo 0) {
            rhsArray[position++] = this.array[i]
        }
        val rhs = RhsBuffer(rhsArray, toMoveToRhs)

        val lhsArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        System.arraycopy(this.array, toMoveToRhs, lhsArray, 0, toLeaveForLhs)
        lhsArray[toLeaveForLhs] = value
        val lhs = LhsBuffer(lhsArray, toLeaveForLhs + 1)

        return DequeBottomLevel(lhs, rhs)
    }

    override fun removeFirst(): ImmutableDeque<Any?> {
        return this.pop()
    }

    override fun addLast(value: Any?): ImmutableDeque<Any?> {
        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        newArray[0] = value
        val rhs = RhsBuffer(newArray, 1)
        return DequeBottomLevel(this, rhs)
    }

    override fun removeLast(): ImmutableDeque<Any?> {
        return this.removeBottomAndMoveRestToOppositeSideBuffer()
    }
}
