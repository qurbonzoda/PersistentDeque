package immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize7.buffer

import immutableDeque.ImmutableDeque
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize7.constants.*
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize7.level.DequeBottomLevel

internal class RhsBuffer(array: Array<Any?>,
                         size: Int): ImmutableBuffer(array, size) {
    // MARK: ImmutableBuffer
    override fun withArray(array: Array<Any?>, size: Int): ImmutableBuffer {
        if (size == 0) {
            return RhsEmptyBuffer
        }
        return RhsBuffer(array, size)
    }

    override fun oppositeSideBufferWithArray(array: Array<Any?>, size: Int): ImmutableBuffer {
        if (size == 0) {
            return LhsEmptyBuffer
        }
        return LhsBuffer(array, size)
    }

    override fun addLeafValuesTo(list: MutableList<Any?>, depth: Int) {
        for (i in 0 until this.size) {
            this.addLeavesOfNode(this.array[i], list, depth)
        }
    }

    override fun getLeafValueAt(index: Int, depth: Int): Any? {
//        assert(index < this.size shl depth)

        val nodePosition = index shr depth
        val indexInNode = index - (nodePosition shl depth)
        return this.getLeafOfNodeAt(indexInNode, this.array[nodePosition], depth)
    }

    override fun setLeafValueAt(index: Int, value: Any?, depth: Int): RhsBuffer {
//        assert(index < this.size shl depth)

        val nodePosition = index shr depth
        val indexInNode = index - (nodePosition shl depth)
        val newNode = this.setLeafOfNodeAt(indexInNode, value, this.array[nodePosition], depth)

        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        System.arraycopy(this.array, 0, newArray, 0, this.size)
        newArray[nodePosition] = newNode

        return RhsBuffer(newArray, this.size)
    }

    override fun popAndPushAllToNextLevelBuffer(count: Int, nextLevelBuffer: ImmutableBuffer): ImmutableBuffer {
//        assert(this.size % 2 == 0)

        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        System.arraycopy(nextLevelBuffer.array, 0, newArray, 0, nextLevelBuffer.size)

        var position = nextLevelBuffer.size

        for (i in 0 until this.size - count step 2) {
            newArray[position++] = Pair(this.array[i], this.array[i + 1])
        }
        return RhsBuffer(newArray, nextLevelBuffer.size + ((this.size - count) shr 1))
    }

    override fun moveToUpperLevelBuffer(count: Int): ImmutableBuffer {
        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)

        var position = 0
        val fromIndex = this.size - count

        for (i in fromIndex until this.size) {
            val pair = this.array[i] as Pair<*, *>
            newArray[position++] = pair.first
            newArray[position++] = pair.second
        }
        return RhsBuffer(newArray, count shl 1)
    }

    // MARK: ImmutableDeque
    override val first: Any?
        get() {
            return this.array[0]
        }

    override val last: Any?
        get() {
            return this.array[this.size - 1]
        }

    override fun addFirst(value: Any?): ImmutableDeque<Any?> {
        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        newArray[0] = value
        val lhs = LhsBuffer(newArray, 1)
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

        val lhsArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        var position = 0
        for (i in toMoveToLhs - 1 downTo 0) {
            lhsArray[position++] = this.array[i]
        }
        val lhs = LhsBuffer(lhsArray, toMoveToLhs)

        val rhsArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        System.arraycopy(this.array, toMoveToLhs, rhsArray, 0, toLeaveForRhs)
        rhsArray[toLeaveForRhs] = value
        val rhs = RhsBuffer(rhsArray, toLeaveForRhs + 1)

        return DequeBottomLevel(lhs, rhs)
    }

    override fun removeLast(): ImmutableDeque<Any?> {
        return this.pop()
    }
}
