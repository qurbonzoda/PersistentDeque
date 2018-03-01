package level

import buffer.*
import deque.ImmutableDeque
import persistentDeque.DequeSubStack
import persistentDeque.PersistentDeque

internal interface ImmutableLevel {
    val color: Int
    val lhs: ImmutableBuffer
    val rhs: ImmutableBuffer
    val next: ImmutableLevel?

    fun withNewLhs(newLhs: ImmutableBuffer): ImmutableLevel
    fun withNewRhs(newRhs: ImmutableBuffer): ImmutableLevel

    // leaky abstraction
    fun subStackSize(depth: Int): Int
    fun subStackHeight(): Int
    fun addBufferLeafValuesTo(list: MutableList<Any?>, depth: Int)
    fun getBufferLeafValueAt(index: Int, size: Int, depth: Int): Any?
    fun setBufferLeafValueAt(index: Int, value: Any?, size: Int, depth: Int): ImmutableLevel

    fun <T> makeGreenUpperLevelPushingLhs(upper: ImmutableLevel, valueToPush: Any?, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        var upperLhs = upper.lhs
        var upperRhs = upper.rhs

        var thisLhs = this.lhs
        var thisRhs = this.rhs

        assert(upperLhs.size == YELLOW_HIGH)
        assert(upperRhs.color != RED)

        if (upperRhs.size == YELLOW_LOW) {
            val thisRhsTop = thisRhs.moveToUpperLevelBuffer(1)
            upperRhs = upperRhs.prependSavingOrder(thisRhsTop)
            thisRhs = thisRhs.pop(1)
        } else if (upperRhs.size == YELLOW_HIGH) {
            thisRhs = upperRhs.pop(upperRhs.size - 2).pushAllToNextLevelBuffer(thisRhs)
            upperRhs = upperRhs.removeBottom(2)
        }

        val delta = if (this.color == GREEN) 1 else 0

        val canPushToThisLhs = MAX_BUFFER_SIZE - thisLhs.size - delta
        val toPushToThisLhs = minOf((MAX_BUFFER_SIZE shr 2) shl 1, canPushToThisLhs shl 1)
        val toLeaveForUpperLhs = upperLhs.size - toPushToThisLhs

        thisLhs = upperLhs.pop(toLeaveForUpperLhs).pushAllToNextLevelBuffer(thisLhs)
        upperLhs = upperLhs.removeBottom(toPushToThisLhs).push(valueToPush)

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack)
    }

    fun <T> makeGreenUpperLevelPushingRhs(upper: ImmutableLevel, valueToPush: Any?, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        var upperLhs = upper.lhs
        var upperRhs = upper.rhs

        var thisLhs = this.lhs
        var thisRhs = this.rhs

        assert(upperRhs.size == YELLOW_HIGH)
        assert(upperLhs.color != RED)

        if (upperLhs.size == YELLOW_LOW) {
            val thisLhsTop = thisLhs.moveToUpperLevelBuffer(1)
            upperLhs = upperLhs.prependSavingOrder(thisLhsTop)
            thisLhs = thisLhs.pop(1)
        } else if (upperLhs.size == YELLOW_HIGH) {
            thisLhs = upperLhs.pop(upperLhs.size - 2).pushAllToNextLevelBuffer(thisLhs)
            upperLhs = upperLhs.removeBottom(2)
        }

        val delta = if (this.color == GREEN) 1 else 0

        val canPushToThisRhs = MAX_BUFFER_SIZE - thisRhs.size - delta
        val toPushToThisRhs = minOf((MAX_BUFFER_SIZE shr 2) shl 1, canPushToThisRhs shl 1)
        val toLeaveForUpperRhs = upperRhs.size - toPushToThisRhs

        thisRhs = upperRhs.pop(toLeaveForUpperRhs).pushAllToNextLevelBuffer(thisRhs)
        upperRhs = upperRhs.removeBottom(toPushToThisRhs).push(valueToPush)

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack)
    }

    fun <T> makeGreenUpperLevelPoppingLhs(upper: ImmutableLevel, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        var upperLhs = upper.lhs.pop()
        var upperRhs = upper.rhs

        var thisLhs = this.lhs
        var thisRhs = this.rhs

        assert(upperLhs.size == RED_LOW)
        assert(upperRhs.color != RED)

        if (upperRhs.size == YELLOW_LOW) {
            val thisRhsTop = thisRhs.moveToUpperLevelBuffer(1)
            upperRhs = upperRhs.prependSavingOrder(thisRhsTop)
            thisRhs = thisRhs.pop(1)
        } else if (upperRhs.size == YELLOW_HIGH) {
            thisRhs = upperRhs.pop(upperRhs.size - 2).pushAllToNextLevelBuffer(thisRhs)
            upperRhs = upperRhs.removeBottom(2)
        }

        val delta = if (this.color == GREEN) 1 else 0

        val canPopFromThisLhs = thisLhs.size - delta
        val toPushToUpperLhs = minOf((MAX_BUFFER_SIZE shr 2), canPopFromThisLhs)

        upperLhs = thisLhs.moveToUpperLevelBuffer(toPushToUpperLhs)
        thisLhs = thisLhs.pop(toPushToUpperLhs)

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack)
    }

    fun <T> makeGreenUpperLevelPoppingRhs(upper: ImmutableLevel, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        var upperLhs = upper.lhs
        var upperRhs = upper.rhs.pop()

        var thisLhs = this.lhs
        var thisRhs = this.rhs

        assert(upperRhs.size == RED_LOW)
        assert(upperLhs.color != RED)

        if (upperLhs.size == YELLOW_LOW) {
            val thisLhsTop = thisLhs.moveToUpperLevelBuffer(1)
            upperLhs = upperLhs.prependSavingOrder(thisLhsTop)
            thisLhs = thisLhs.pop(1)
        } else if (upperLhs.size == YELLOW_HIGH) {
            thisLhs = upperLhs.pop(upperLhs.size - 2).pushAllToNextLevelBuffer(thisLhs)
            upperLhs = upperLhs.removeBottom(2)
        }

        val delta = if (this.color == GREEN) 1 else 0

        val canPopFromThisRhs = thisRhs.size - delta
        val toPushToUpperRhs = minOf((MAX_BUFFER_SIZE shr 2), canPopFromThisRhs)

        upperRhs = thisRhs.moveToUpperLevelBuffer(toPushToUpperRhs)
        thisRhs = thisRhs.pop(toPushToUpperRhs)

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack)
    }

    fun <T> makeGreenUpperLevel(upper: ImmutableLevel, topSubStack: ImmutableLevel, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        assert(upper.color == RED)
        assert((this.color != RED) || (this.lhs.size == 0 && this.rhs.size == 0))

        var upperLhs = upper.lhs
        var upperRhs = upper.rhs

        var thisLhs = this.lhs
        var thisRhs = this.rhs

        if (upperLhs.size <= YELLOW_LOW) {
            val thisLhsTop = thisLhs.moveToUpperLevelBuffer(1)
            upperLhs = upperLhs.prependSavingOrder(thisLhsTop)
            thisLhs = thisLhs.pop(1)
        } else if (upperLhs.size >= YELLOW_HIGH) {
            thisLhs = upperLhs.pop(upperLhs.size - 2).pushAllToNextLevelBuffer(thisLhs)
            upperLhs = upperLhs.removeBottom(2)
        }

        if (upperRhs.size <= YELLOW_LOW) {
            val thisRhsTop = thisRhs.moveToUpperLevelBuffer(1)
            upperRhs = upperRhs.prependSavingOrder(thisRhsTop)
            thisRhs = thisRhs.pop(1)
        } else if (upperRhs.size >= YELLOW_HIGH) {
            thisRhs = upperRhs.pop(upperRhs.size - 2).pushAllToNextLevelBuffer(thisRhs)
            upperRhs = upperRhs.removeBottom(2)
        }

        return this.makeImmutableDeque(topSubStack, upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack)
    }

    fun <T> makeImmutableDeque(upperLhs: ImmutableBuffer,
                               upperRhs: ImmutableBuffer,
                               thisLhs: ImmutableBuffer,
                               thisRhs: ImmutableBuffer,
                               lowerSubStack: DequeSubStack?): ImmutableDeque<T>

    fun <T> makeImmutableDeque(topSubStack: ImmutableLevel,
                               upperLhs: ImmutableBuffer,
                               upperRhs: ImmutableBuffer,
                               thisLhs: ImmutableBuffer,
                               thisRhs: ImmutableBuffer,
                               lowerSubStack: DequeSubStack?): ImmutableDeque<T>
}