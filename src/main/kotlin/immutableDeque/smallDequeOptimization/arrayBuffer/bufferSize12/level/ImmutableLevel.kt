package immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize12.level

import immutableDeque.ImmutableDeque
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize12.buffer.ImmutableBuffer
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize12.buffer.LhsEmptyBuffer
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize12.buffer.RhsEmptyBuffer
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize12.constants.*
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize12.persistentDeque.DequeSubStack
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize12.persistentDeque.PersistentDeque

internal abstract class ImmutableLevel(val lhs: ImmutableBuffer,
                                       val rhs: ImmutableBuffer,
                                       open val next: ImmutableLevel?) {
    val color: Int
        get() {
            return minOf(this.lhs.color, this.rhs.color)
        }

    abstract fun withNewLhs(newLhs: ImmutableBuffer): ImmutableLevel
    abstract fun withNewRhs(newRhs: ImmutableBuffer): ImmutableLevel

    abstract fun subStackSize(depth: Int): Int
    abstract fun subStackHeight(): Int
    abstract fun addBufferLeafValuesTo(list: MutableList<Any?>, depth: Int)
    abstract fun getBufferLeafValueAt(index: Int, size: Int, depth: Int): Any?
    abstract fun setBufferLeafValueAt(index: Int, value: Any?, size: Int, depth: Int): ImmutableLevel

    abstract fun <T> makeImmutableDeque(upperLhs: ImmutableBuffer,
                                        upperRhs: ImmutableBuffer,
                                        thisLhs: ImmutableBuffer,
                                        thisRhs: ImmutableBuffer,
                                        pDeque: PersistentDeque<*>,
                                        newSize: Int): ImmutableDeque<T>

    abstract fun makeDequeSubStack(upperLhs: ImmutableBuffer,
                                   upperRhs: ImmutableBuffer,
                                   thisLhs: ImmutableBuffer,
                                   thisRhs: ImmutableBuffer,
                                   lowerSubStack: DequeSubStack?): DequeSubStack

    fun <T> makeGreenUpperLevelPushingLhs(valueToPush: Any?, pDeque: PersistentDeque<*>): ImmutableDeque<T> {
//        assert(upper.lhs.size == YELLOW_HIGH)
//        assert(upper.rhs.color != RED)

        val lowerSubStack = pDeque.next.next

        val delta = if (this.color == GREEN && lowerSubStack != null && lowerSubStack.stack.color == RED) 1 else 0

        val thisLhs = this.fillThisFromUpper(pDeque.lhs, this.lhs, delta)
        val upperLhs = this.removeBottomAndPush(pDeque.lhs, (thisLhs.size - this.lhs.size) shl 1, valueToPush)

        var upperRhs = pDeque.rhs
        var thisRhs = this.rhs

        if (upperRhs.size == YELLOW_LOW) {
            val thisRhsTop = thisRhs.moveToUpperLevelBuffer(1)
            upperRhs = upperRhs.prependSavingOrder(thisRhsTop)
            thisRhs = thisRhs.pop(1)
        } else if (upperRhs.size == YELLOW_HIGH) {
            thisRhs = upperRhs.popAndPushAllToNextLevelBuffer(upperRhs.size - 2, thisRhs)
            upperRhs = upperRhs.removeBottom(2)
        }

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, pDeque, pDeque.size + 1)
    }

    fun <T> makeGreenUpperLevelPushingRhs(valueToPush: Any?, pDeque: PersistentDeque<*>): ImmutableDeque<T> {
//        assert(upper.rhs.size == YELLOW_HIGH)
//        assert(upper.lhs.color != RED)

        val lowerSubStack = pDeque.next.next

        val delta = if (this.color == GREEN && lowerSubStack != null && lowerSubStack.stack.color == RED) 1 else 0

        val thisRhs = this.fillThisFromUpper(pDeque.rhs, this.rhs, delta)
        val upperRhs = this.removeBottomAndPush(pDeque.rhs, (thisRhs.size - this.rhs.size) shl 1, valueToPush)

        var upperLhs = pDeque.lhs
        var thisLhs = this.lhs

        if (upperLhs.size == YELLOW_LOW) {
            val thisLhsTop = thisLhs.moveToUpperLevelBuffer(1)
            upperLhs = upperLhs.prependSavingOrder(thisLhsTop)
            thisLhs = thisLhs.pop(1)
        } else if (upperLhs.size == YELLOW_HIGH) {
            thisLhs = upperLhs.popAndPushAllToNextLevelBuffer(upperLhs.size - 2, thisLhs)
            upperLhs = upperLhs.removeBottom(2)
        }

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, pDeque, pDeque.size + 1)
    }

    fun <T> makeGreenUpperLevelPoppingLhs(pDeque: PersistentDeque<*>): ImmutableDeque<T> {
//        assert(upper.lhs.size == RED_LOW)
//        assert(upper.rhs.color != RED)

        val lowerSubStack = pDeque.next.next

        val delta = if (this.color == GREEN && lowerSubStack != null && lowerSubStack.stack.color == RED) 1 else 0

        val upperLhs = this.fillUpperFromThis(LhsEmptyBuffer, this.lhs, delta)
        val thisLhs = this.lhs.pop(upperLhs.size shr 1)

        var upperRhs = pDeque.rhs
        var thisRhs = this.rhs

        if (upperRhs.size == YELLOW_LOW) {
            val thisRhsTop = thisRhs.moveToUpperLevelBuffer(1)
            upperRhs = upperRhs.prependSavingOrder(thisRhsTop)
            thisRhs = thisRhs.pop(1)
        } else if (upperRhs.size == YELLOW_HIGH) {
            thisRhs = upperRhs.popAndPushAllToNextLevelBuffer(upperRhs.size - 2, thisRhs)
            upperRhs = upperRhs.removeBottom(2)
        }

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, pDeque, pDeque.size - 1)
    }

    fun <T> makeGreenUpperLevelPoppingRhs(pDeque: PersistentDeque<*>): ImmutableDeque<T> {
//        assert(upper.rhs.size == RED_LOW)
//        assert(upper.lhs.color != RED)

        val lowerSubStack = pDeque.next.next

        val delta = if (this.color == GREEN && lowerSubStack != null && lowerSubStack.stack.color == RED) 1 else 0

        val upperRhs = this.fillUpperFromThis(RhsEmptyBuffer, this.rhs, delta)
        val thisRhs = this.rhs.pop(upperRhs.size shr 1)

        var upperLhs = pDeque.lhs
        var thisLhs = this.lhs

        if (upperLhs.size == YELLOW_LOW) {
            val thisLhsTop = thisLhs.moveToUpperLevelBuffer(1)
            upperLhs = upperLhs.prependSavingOrder(thisLhsTop)
            thisLhs = thisLhs.pop(1)
        } else if (upperLhs.size == YELLOW_HIGH) {
            thisLhs = upperLhs.popAndPushAllToNextLevelBuffer(upperLhs.size - 2, thisLhs)
            upperLhs = upperLhs.removeBottom(2)
        }

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, pDeque, pDeque.size - 1)
    }

    fun makeGreenUpperLevel(upper: ImmutableLevel, lowerSubStack: DequeSubStack?): DequeSubStack {
//        assert(upper.color == RED)
//        assert((this.color != RED) || (this.lhs.size == 0 && this.rhs.size == 0))

        var upperLhs = upper.lhs
        var thisLhs = this.lhs

        val delta = if (this.color == GREEN && lowerSubStack != null && lowerSubStack.stack.color == RED) 1 else 0

        if (upperLhs.size <= YELLOW_LOW) {
            upperLhs = this.fillUpperFromThis(upperLhs, thisLhs, delta)
            thisLhs = thisLhs.pop((upperLhs.size - upper.lhs.size) shr 1)
        } else if (upperLhs.size >= YELLOW_HIGH) {
            thisLhs = this.fillThisFromUpper(upperLhs, thisLhs, delta)
            upperLhs = upperLhs.removeBottom((thisLhs.size - this.lhs.size) shl 1)
        }

        var upperRhs = upper.rhs
        var thisRhs = this.rhs

        if (upperRhs.size <= YELLOW_LOW) {
            upperRhs = this.fillUpperFromThis(upperRhs, thisRhs, delta)
            thisRhs = thisRhs.pop((upperRhs.size - upper.rhs.size) shr 1)
        } else if (upperRhs.size >= YELLOW_HIGH) {
            thisRhs = this.fillThisFromUpper(upperRhs, thisRhs, delta)
            upperRhs = upperRhs.removeBottom((thisRhs.size - this.rhs.size) shl 1)
        }

        return this.makeDequeSubStack(upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack)
    }

    private fun fillThisFromUpper(upperLevel: ImmutableBuffer, lowerLevel: ImmutableBuffer, delta: Int): ImmutableBuffer {
        val canMoveToLower = MAX_BUFFER_SIZE - lowerLevel.size - delta
        val toMoveToLower = minOf(FULL_UPPER_LEVEL_SHOULD_MOVE_TO_THIS_LEVEL, canMoveToLower shl 1)

        return upperLevel.popAndPushAllToNextLevelBuffer(upperLevel.size - toMoveToLower, lowerLevel)
    }

    private fun fillUpperFromThis(upperLevel: ImmutableBuffer, lowerLevel: ImmutableBuffer, delta: Int): ImmutableBuffer {
        val canMoveFromLower = lowerLevel.size - delta
        val toMoveFromLower = minOf(EMPTY_UPPER_LEVEL_SHOULD_MOVE_FROM_THIS_LEVEL, canMoveFromLower)

        val fromLower = lowerLevel.moveToUpperLevelBuffer(toMoveFromLower)
        return upperLevel.prependSavingOrder(fromLower)
    }

    private fun removeBottomAndPush(buffer: ImmutableBuffer, count: Int, valueToPush: Any?): ImmutableBuffer {
        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        System.arraycopy(buffer.array, count, newArray, 0, buffer.size - count)
        newArray[buffer.size - count] = valueToPush
        return buffer.withArray(newArray, buffer.size - count + 1)
    }
}
