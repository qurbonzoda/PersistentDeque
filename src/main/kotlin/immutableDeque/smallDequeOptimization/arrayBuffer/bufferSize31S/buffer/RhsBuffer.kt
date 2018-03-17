package immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize31S.buffer

import immutableDeque.ImmutableDeque
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize31S.constants.*
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize31S.level.DequeBottomLevel

internal class RhsBuffer(array: Array<Any?>): ImmutableBuffer(array) {
    // MARK: ImmutableBuffer
    override fun withArray(array: Array<Any?>): ImmutableBuffer {
        if (array.isEmpty()) {
            return RhsEmptyBuffer
        }
        return RhsBuffer(array)
    }

    override fun oppositeSideBufferWithArray(array: Array<Any?>): ImmutableBuffer {
        if (array.isEmpty()) {
            return LhsEmptyBuffer
        }
        return LhsBuffer(array)
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

        val newArray = arrayOfNulls<Any?>(this.size)
        System.arraycopy(this.array, 0, newArray, 0, this.size)
        newArray[nodePosition] = newNode

        return RhsBuffer(newArray)
    }

    override fun popAndPushAllToNextLevelBuffer(count: Int, nextLevelBuffer: ImmutableBuffer): ImmutableBuffer {
//        assert(this.size % 2 == 0)

        val newArray = arrayOfNulls<Any?>(nextLevelBuffer.size + ((this.size - count) shr 1))
        System.arraycopy(nextLevelBuffer.array, 0, newArray, 0, nextLevelBuffer.size)

        var position = nextLevelBuffer.size

        for (i in 0 until this.size - count step 2) {
            newArray[position++] = Pair(this.array[i], this.array[i + 1])
        }
        return RhsBuffer(newArray)
    }

    override fun moveToUpperLevelBuffer(count: Int): ImmutableBuffer {
        val newArray = arrayOfNulls<Any?>(count shl 1)

        var position = 0
        val fromIndex = this.size - count

        for (i in fromIndex until this.size) {
            val pair = this.array[i] as Pair<*, *>
            newArray[position++] = pair.first
            newArray[position++] = pair.second
        }
        return RhsBuffer(newArray)
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
        val newArray = arrayOf(value)
        val lhs = LhsBuffer(newArray)
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

        val lhsArray = arrayOfNulls<Any?>(toMoveToLhs)
        var position = 0
        for (i in toMoveToLhs - 1 downTo 0) {
            lhsArray[position++] = this.array[i]
        }
        val lhs = LhsBuffer(lhsArray)

        val rhsArray = arrayOfNulls<Any?>(toLeaveForRhs + 1)
        System.arraycopy(this.array, toMoveToLhs, rhsArray, 0, toLeaveForRhs)
        rhsArray[toLeaveForRhs] = value
        val rhs = RhsBuffer(rhsArray)

        return DequeBottomLevel(lhs, rhs)
    }

    override fun removeLast(): ImmutableDeque<Any?> {
        return this.pop()
    }
}
