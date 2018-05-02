package immutableDeque.childCount4.bufferSize128.level

import immutableDeque.GREEN
import immutableDeque.ImmutableDeque
import immutableDeque.RED
import immutableDeque.childCount4.CHILD_COUNT
import immutableDeque.childCount4.bufferSize128.buffer.ImmutableBuffer
import immutableDeque.childCount4.bufferSize128.buffer.LhsEmptyBuffer
import immutableDeque.childCount4.bufferSize128.buffer.RhsEmptyBuffer
import immutableDeque.childCount4.bufferSize128.constants.*
import immutableDeque.childCount4.bufferSize128.persistentDeque.DequeSubStack
import immutableDeque.childCount4.bufferSize128.persistentDeque.PersistentDeque

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
        val upperLhs = pDeque.lhs.removeBottom((thisLhs.size - this.lhs.size) * CHILD_COUNT).push(valueToPush)

        var upperRhs = pDeque.rhs
        var thisRhs = this.rhs

        if (upperRhs.size == YELLOW_LOW) {
            val thisRhsTop = thisRhs.moveToUpperLevelBuffer(1)
            upperRhs = upperRhs.prependSavingOrder(thisRhsTop)
            thisRhs = thisRhs.pop(1)
        } else if (upperRhs.size == YELLOW_HIGH) {
            thisRhs = upperRhs.pop(upperRhs.size - CHILD_COUNT).pushAllToNextLevelBuffer(thisRhs)
            upperRhs = upperRhs.removeBottom(CHILD_COUNT)
        }

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, pDeque, pDeque.size + 1)
    }

    fun <T> makeGreenUpperLevelPushingRhs(valueToPush: Any?, pDeque: PersistentDeque<*>): ImmutableDeque<T> {
//        assert(upper.rhs.size == YELLOW_HIGH)
//        assert(upper.lhs.color != RED)

        val lowerSubStack = pDeque.next.next

        val delta = if (this.color == GREEN && lowerSubStack != null && lowerSubStack.stack.color == RED) 1 else 0

        val thisRhs = this.fillThisFromUpper(pDeque.rhs, this.rhs, delta)
        val upperRhs = pDeque.rhs.removeBottom((thisRhs.size - this.rhs.size) * CHILD_COUNT).push(valueToPush)

        var upperLhs = pDeque.lhs
        var thisLhs = this.lhs

        if (upperLhs.size == YELLOW_LOW) {
            val thisLhsTop = thisLhs.moveToUpperLevelBuffer(1)
            upperLhs = upperLhs.prependSavingOrder(thisLhsTop)
            thisLhs = thisLhs.pop(1)
        } else if (upperLhs.size == YELLOW_HIGH) {
            thisLhs = upperLhs.pop(upperLhs.size - CHILD_COUNT).pushAllToNextLevelBuffer(thisLhs)
            upperLhs = upperLhs.removeBottom(CHILD_COUNT)
        }

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, pDeque, pDeque.size + 1)
    }

    fun <T> makeGreenUpperLevelPoppingLhs(pDeque: PersistentDeque<*>): ImmutableDeque<T> {
//        assert(upper.lhs.size == RED_LOW)
//        assert(upper.rhs.color != RED)

        val lowerSubStack = pDeque.next.next

        val delta = if (this.color == GREEN && lowerSubStack != null && lowerSubStack.stack.color == RED) 1 else 0

        val upperLhs = this.fillUpperFromThis(LhsEmptyBuffer, this.lhs, delta)
        val thisLhs = this.lhs.pop(upperLhs.size / CHILD_COUNT)

        var upperRhs = pDeque.rhs
        var thisRhs = this.rhs

        if (upperRhs.size == YELLOW_LOW) {
            val thisRhsTop = thisRhs.moveToUpperLevelBuffer(1)
            upperRhs = upperRhs.prependSavingOrder(thisRhsTop)
            thisRhs = thisRhs.pop(1)
        } else if (upperRhs.size == YELLOW_HIGH) {
            thisRhs = upperRhs.pop(upperRhs.size - CHILD_COUNT).pushAllToNextLevelBuffer(thisRhs)
            upperRhs = upperRhs.removeBottom(CHILD_COUNT)
        }

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, pDeque, pDeque.size - 1)
    }

    fun <T> makeGreenUpperLevelPoppingRhs(pDeque: PersistentDeque<*>): ImmutableDeque<T> {
//        assert(upper.rhs.size == RED_LOW)
//        assert(upper.lhs.color != RED)

        val lowerSubStack = pDeque.next.next

        val delta = if (this.color == GREEN && lowerSubStack != null && lowerSubStack.stack.color == RED) 1 else 0

        val upperRhs = this.fillUpperFromThis(RhsEmptyBuffer, this.rhs, delta)
        val thisRhs = this.rhs.pop(upperRhs.size / CHILD_COUNT)

        var upperLhs = pDeque.lhs
        var thisLhs = this.lhs

        if (upperLhs.size == YELLOW_LOW) {
            val thisLhsTop = thisLhs.moveToUpperLevelBuffer(1)
            upperLhs = upperLhs.prependSavingOrder(thisLhsTop)
            thisLhs = thisLhs.pop(1)
        } else if (upperLhs.size == YELLOW_HIGH) {
            thisLhs = upperLhs.pop(upperLhs.size - CHILD_COUNT).pushAllToNextLevelBuffer(thisLhs)
            upperLhs = upperLhs.removeBottom(CHILD_COUNT)
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
            thisLhs = thisLhs.pop((upperLhs.size - upper.lhs.size) / CHILD_COUNT)
        } else if (upperLhs.size >= YELLOW_HIGH) {
            thisLhs = this.fillThisFromUpper(upperLhs, thisLhs, delta)
            upperLhs = upperLhs.removeBottom((thisLhs.size - this.lhs.size) * CHILD_COUNT)
        }

        var upperRhs = upper.rhs
        var thisRhs = this.rhs

        if (upperRhs.size <= YELLOW_LOW) {
            upperRhs = this.fillUpperFromThis(upperRhs, thisRhs, delta)
            thisRhs = thisRhs.pop((upperRhs.size - upper.rhs.size) / CHILD_COUNT)
        } else if (upperRhs.size >= YELLOW_HIGH) {
            thisRhs = this.fillThisFromUpper(upperRhs, thisRhs, delta)
            upperRhs = upperRhs.removeBottom((thisRhs.size - this.rhs.size) * CHILD_COUNT)
        }

        return this.makeDequeSubStack(upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack)
    }

    private fun fillThisFromUpper(upperLevel: ImmutableBuffer, lowerLevel: ImmutableBuffer, delta: Int): ImmutableBuffer {
        val canMoveToLower = MAX_BUFFER_SIZE - lowerLevel.size - delta
        val toMoveToLower = minOf(FULL_UPPER_LEVEL_SHOULD_MOVE_TO_THIS_LEVEL, canMoveToLower * CHILD_COUNT)
        return upperLevel.pop(upperLevel.size - toMoveToLower).pushAllToNextLevelBuffer(lowerLevel)
    }

    private fun fillUpperFromThis(upperLevel: ImmutableBuffer, lowerLevel: ImmutableBuffer, delta: Int): ImmutableBuffer {
        val canMoveFromLower = lowerLevel.size - delta
        val toMoveFromLower = minOf(EMPTY_UPPER_LEVEL_SHOULD_MOVE_FROM_THIS_LEVEL, canMoveFromLower)

        val fromLower = lowerLevel.moveToUpperLevelBuffer(toMoveFromLower)
        return upperLevel.prependSavingOrder(fromLower)
    }
}
