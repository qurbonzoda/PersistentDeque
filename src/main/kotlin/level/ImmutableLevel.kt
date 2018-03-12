package level

import buffer.*
import deque.ImmutableDeque
import persistentDeque.DequeSubStack

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
                                        lowerSubStack: DequeSubStack?,
                                        newSize: Int): ImmutableDeque<T>

    abstract fun <T> makeImmutableDeque(topSubStack: ImmutableLevel,
                                        upperLhs: ImmutableBuffer,
                                        upperRhs: ImmutableBuffer,
                                        thisLhs: ImmutableBuffer,
                                        thisRhs: ImmutableBuffer,
                                        lowerSubStack: DequeSubStack?,
                                        newSize: Int): ImmutableDeque<T>

    fun <T> makeGreenUpperLevelPushingLhs(topLhs: ImmutableBuffer,
                                          topRhs: ImmutableBuffer,
                                          valueToPush: Any?,
                                          lowerSubStack: DequeSubStack?,
                                          newSize: Int): ImmutableDeque<T> {
//        assert(upper.lhs.size == YELLOW_HIGH)
//        assert(upper.rhs.color != RED)

        var upperLhs = topLhs
        var thisLhs = this.lhs

        val delta = if (this.color == GREEN && lowerSubStack != null && lowerSubStack.stack.color == RED) 1 else 0
        val canPushToThisLhs = MAX_BUFFER_SIZE - thisLhs.size - delta
        val toPushToThisLhs = minOf(FULL_UPPER_LEVEL_SHOULD_MOVE_TO_THIS_LEVEL, canPushToThisLhs shl 1)
        val toLeaveForUpperLhs = upperLhs.size - toPushToThisLhs

        thisLhs = upperLhs.pop(toLeaveForUpperLhs).pushAllToNextLevelBuffer(thisLhs)
        upperLhs = upperLhs.removeBottom(toPushToThisLhs).push(valueToPush)

        var upperRhs = topRhs
        var thisRhs = this.rhs

        if (upperRhs.size == YELLOW_LOW) {
            val thisRhsTop = thisRhs.moveToUpperLevelBuffer(1)
            upperRhs = upperRhs.prependSavingOrder(thisRhsTop)
            thisRhs = thisRhs.pop(1)
        } else if (upperRhs.size == YELLOW_HIGH) {
            thisRhs = upperRhs.pop(upperRhs.size - 2).pushAllToNextLevelBuffer(thisRhs)
            upperRhs = upperRhs.removeBottom(2)
        }

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack, newSize)
    }

    fun <T> makeGreenUpperLevelPushingRhs(topLhs: ImmutableBuffer,
                                          topRhs: ImmutableBuffer,
                                          valueToPush: Any?,
                                          lowerSubStack: DequeSubStack?,
                                          newSize: Int): ImmutableDeque<T> {
//        assert(upper.rhs.size == YELLOW_HIGH)
//        assert(upper.lhs.color != RED)

        var thisRhs = this.rhs
        var upperRhs = topRhs

        val delta = if (this.color == GREEN && lowerSubStack != null && lowerSubStack.stack.color == RED) 1 else 0
        val canPushToThisRhs = MAX_BUFFER_SIZE - thisRhs.size - delta
        val toPushToThisRhs = minOf(FULL_UPPER_LEVEL_SHOULD_MOVE_TO_THIS_LEVEL, canPushToThisRhs shl 1)
        val toLeaveForUpperRhs = upperRhs.size - toPushToThisRhs

        thisRhs = upperRhs.pop(toLeaveForUpperRhs).pushAllToNextLevelBuffer(thisRhs)
        upperRhs = upperRhs.removeBottom(toPushToThisRhs).push(valueToPush)

        var upperLhs = topLhs
        var thisLhs = this.lhs

        if (upperLhs.size == YELLOW_LOW) {
            val thisLhsTop = thisLhs.moveToUpperLevelBuffer(1)
            upperLhs = upperLhs.prependSavingOrder(thisLhsTop)
            thisLhs = thisLhs.pop(1)
        } else if (upperLhs.size == YELLOW_HIGH) {
            thisLhs = upperLhs.pop(upperLhs.size - 2).pushAllToNextLevelBuffer(thisLhs)
            upperLhs = upperLhs.removeBottom(2)
        }

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack, newSize)
    }

    fun <T> makeGreenUpperLevelPoppingLhs(topRhs: ImmutableBuffer,
                                          lowerSubStack: DequeSubStack?,
                                          newSize: Int): ImmutableDeque<T> {
//        assert(upper.lhs.size == RED_LOW)
//        assert(upper.rhs.color != RED)

        var thisLhs = this.lhs

        val delta = if (this.color == GREEN && lowerSubStack != null && lowerSubStack.stack.color == RED) 1 else 0
        val canPopFromThisLhs = thisLhs.size - delta
        val toPushToUpperLhs = minOf(EMPTY_UPPER_LEVEL_SHOULD_MOVE_FROM_THIS_LEVEL, canPopFromThisLhs)

        val upperLhs = thisLhs.moveToUpperLevelBuffer(toPushToUpperLhs)
        thisLhs = thisLhs.pop(toPushToUpperLhs)

        var upperRhs = topRhs
        var thisRhs = this.rhs

        if (upperRhs.size == YELLOW_LOW) {
            val thisRhsTop = thisRhs.moveToUpperLevelBuffer(1)
            upperRhs = upperRhs.prependSavingOrder(thisRhsTop)
            thisRhs = thisRhs.pop(1)
        } else if (upperRhs.size == YELLOW_HIGH) {
            thisRhs = upperRhs.pop(upperRhs.size - 2).pushAllToNextLevelBuffer(thisRhs)
            upperRhs = upperRhs.removeBottom(2)
        }

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack, newSize)
    }

    fun <T> makeGreenUpperLevelPoppingRhs(topLhs: ImmutableBuffer,
                                          lowerSubStack: DequeSubStack?,
                                          newSize: Int): ImmutableDeque<T> {
//        assert(upper.rhs.size == RED_LOW)
//        assert(upper.lhs.color != RED)

        var thisRhs = this.rhs

        val delta = if (this.color == GREEN && lowerSubStack != null && lowerSubStack.stack.color == RED) 1 else 0
        val canPopFromThisRhs = thisRhs.size - delta
        val toPushToUpperRhs = minOf(EMPTY_UPPER_LEVEL_SHOULD_MOVE_FROM_THIS_LEVEL, canPopFromThisRhs)

        val upperRhs = thisRhs.moveToUpperLevelBuffer(toPushToUpperRhs)
        thisRhs = thisRhs.pop(toPushToUpperRhs)

        var upperLhs = topLhs
        var thisLhs = this.lhs

        if (upperLhs.size == YELLOW_LOW) {
            val thisLhsTop = thisLhs.moveToUpperLevelBuffer(1)
            upperLhs = upperLhs.prependSavingOrder(thisLhsTop)
            thisLhs = thisLhs.pop(1)
        } else if (upperLhs.size == YELLOW_HIGH) {
            thisLhs = upperLhs.pop(upperLhs.size - 2).pushAllToNextLevelBuffer(thisLhs)
            upperLhs = upperLhs.removeBottom(2)
        }

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack, newSize)
    }

    fun <T> makeGreenUpperLevel(upper: ImmutableLevel,
                                topSubStack: ImmutableLevel,
                                lowerSubStack: DequeSubStack?,
                                newSize: Int): ImmutableDeque<T> {
//        assert(upper.color == RED)
//        assert((this.color != RED) || (this.lhs.size == 0 && this.rhs.size == 0))

        var upperLhs = upper.lhs
        var thisLhs = this.lhs

        val delta = if (this.color == GREEN && lowerSubStack != null && lowerSubStack.stack.color == RED) 1 else 0

        if (upperLhs.size <= YELLOW_LOW) {
            val canPopFromThisLhs = thisLhs.size - delta
            val toMoveToUpperLhs = minOf(EMPTY_UPPER_LEVEL_SHOULD_MOVE_FROM_THIS_LEVEL, canPopFromThisLhs)

            val thisLhsTop = thisLhs.moveToUpperLevelBuffer(toMoveToUpperLhs)
            upperLhs = upperLhs.prependSavingOrder(thisLhsTop)
            thisLhs = thisLhs.pop(toMoveToUpperLhs)
        } else if (upperLhs.size >= YELLOW_HIGH) {
            val canMoveToThisLhs = MAX_BUFFER_SIZE - thisLhs.size - delta
            val toMoveToThisLhs = minOf(FULL_UPPER_LEVEL_SHOULD_MOVE_TO_THIS_LEVEL, canMoveToThisLhs shl 1)

            thisLhs = upperLhs.pop(upperLhs.size - toMoveToThisLhs).pushAllToNextLevelBuffer(thisLhs)
            upperLhs = upperLhs.removeBottom(toMoveToThisLhs)
        }

        var upperRhs = upper.rhs
        var thisRhs = this.rhs

        if (upperRhs.size <= YELLOW_LOW) {
            val canPopFromThisRhs = thisRhs.size - delta
            val toMoveToUpperRhs = minOf(EMPTY_UPPER_LEVEL_SHOULD_MOVE_FROM_THIS_LEVEL, canPopFromThisRhs)

            val thisRhsTop = thisRhs.moveToUpperLevelBuffer(toMoveToUpperRhs)
            upperRhs = upperRhs.prependSavingOrder(thisRhsTop)
            thisRhs = thisRhs.pop(toMoveToUpperRhs)
        } else if (upperRhs.size >= YELLOW_HIGH) {
            val canMoveToThisRhs = MAX_BUFFER_SIZE - thisRhs.size - delta
            val toMoveToThisRhs = minOf(FULL_UPPER_LEVEL_SHOULD_MOVE_TO_THIS_LEVEL, canMoveToThisRhs shl 1)

            thisRhs = upperRhs.pop(upperRhs.size - toMoveToThisRhs).pushAllToNextLevelBuffer(thisRhs)
            upperRhs = upperRhs.removeBottom(toMoveToThisRhs)
        }

        return this.makeImmutableDeque(topSubStack, upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack, newSize)
    }
}