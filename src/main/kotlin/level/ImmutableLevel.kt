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
    fun size(depth: Int): Int   // TODO: subStackSize(depth: Int)
    fun subStackHeight(): Int
    fun addBufferLeafValuesTo(list: MutableList<Any?>, depth: Int)
    fun getBufferLeafValueAt(index: Int, size: Int, depth: Int): Any?
    fun setBufferLeafValueAt(index: Int, value: Any?, size: Int, depth: Int): ImmutableLevel

    fun <T> makeGreenUpperLevelPushingLhs(upper: ImmutableLevel, valueToPush: Any?, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        return this.makeGreenUpperLevelPushingLhs(upper, this.lhs, this.rhs, valueToPush, lowerSubStack)
    }

    fun <T> makeGreenUpperLevelPushingRhs(upper: ImmutableLevel, valueToPush: Any?, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        return this.makeGreenUpperLevelPushingRhs(upper, this.lhs, this.rhs, valueToPush, lowerSubStack)
    }

    fun <T> makeGreenUpperLevelPoppingLhs(upper: ImmutableLevel, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        return this.makeGreenUpperLevelPoppingLhs(upper, this.lhs, this.rhs, lowerSubStack)
    }

    fun <T> makeGreenUpperLevelPoppingRhs(upper: ImmutableLevel, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        return this.makeGreenUpperLevelPoppingRhs(upper, this.lhs, this.rhs, lowerSubStack)
    }

    fun <T> makeGreenUpperLevel(upper: ImmutableLevel, topSubStack: ImmutableLevel, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        return this.makeGreenUpperLevel(upper, this.lhs, this.rhs, topSubStack, lowerSubStack)
    }

    fun <T> makeImmutableDeque(upperLhs: ImmutableBuffer,
                               upperRhs: ImmutableBuffer,
                               thisLhs: ImmutableBuffer,
                               thisRhs: ImmutableBuffer,
                               lowerSubStack: DequeSubStack?): ImmutableDeque<T>

    // MARK: protected
    fun <T> makeGreenUpperLevelPushingLhs(upper: ImmutableLevel,
                                          currentLhs: ImmutableBuffer,
                                          currentRhs: ImmutableBuffer,
                                          valueToPush: Any?,
                                          lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        var upperLhs = upper.lhs
        var upperRhs = upper.rhs

        var thisLhs = currentLhs
        var thisRhs = currentRhs

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

        val canPushToThisLhs = MAX_BUFFER_SIZE - thisLhs.size
        val toPushToThisLhs = minOf((MAX_BUFFER_SIZE shr 2) shl 1, canPushToThisLhs shl 1)
        val toLeaveForUpperLhs = upperLhs.size - toPushToThisLhs

        thisLhs = upperLhs.pop(toLeaveForUpperLhs).pushAllToNextLevelBuffer(thisLhs)
        upperLhs = upperLhs.removeBottom(toPushToThisLhs).push(valueToPush)

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack)
    }

    fun <T> makeGreenUpperLevelPushingRhs(upper: ImmutableLevel,
                                          currentLhs: ImmutableBuffer,
                                          currentRhs: ImmutableBuffer,
                                          valueToPush: Any?,
                                          lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        var upperLhs = upper.lhs
        var upperRhs = upper.rhs

        var thisLhs = currentLhs
        var thisRhs = currentRhs

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

        val canPushToThisRhs = MAX_BUFFER_SIZE - thisRhs.size
        val toPushToThisRhs = minOf((MAX_BUFFER_SIZE shr 2) shl 1, canPushToThisRhs shl 1)
        val toLeaveForUpperRhs = upperRhs.size - toPushToThisRhs

        thisRhs = upperRhs.pop(toLeaveForUpperRhs).pushAllToNextLevelBuffer(thisRhs)
        upperRhs = upperRhs.removeBottom(toPushToThisRhs).push(valueToPush)

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack)
    }

    fun <T> makeGreenUpperLevelPoppingLhs(upper: ImmutableLevel,
                                          currentLhs: ImmutableBuffer,
                                          currentRhs: ImmutableBuffer,
                                          lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        var upperLhs = upper.lhs.pop()
        var upperRhs = upper.rhs

        var thisLhs = currentLhs
        var thisRhs = currentRhs

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

        val canPopFromThisLhs = thisLhs.size
        val toPushToUpperLhs = minOf((MAX_BUFFER_SIZE shr 2), canPopFromThisLhs)

        upperLhs = thisLhs.moveToUpperLevelBuffer(toPushToUpperLhs)
        thisLhs = thisLhs.pop(toPushToUpperLhs)

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack)
    }

    fun <T> makeGreenUpperLevelPoppingRhs(upper: ImmutableLevel,
                                          lhs: ImmutableBuffer,
                                          rhs: ImmutableBuffer,
                                          lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        var upperLhs = upper.lhs
        var upperRhs = upper.rhs.pop()

        var thisLhs = lhs
        var thisRhs = rhs

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

        val canPopFromThisRhs = thisRhs.size
        val toPushToUpperRhs = minOf((MAX_BUFFER_SIZE shr 2), canPopFromThisRhs)

        upperRhs = thisRhs.moveToUpperLevelBuffer(toPushToUpperRhs)
        thisRhs = thisRhs.pop(toPushToUpperRhs)

        return this.makeImmutableDeque(upperLhs, upperRhs, thisLhs, thisRhs, lowerSubStack)
    }

    fun <T> makeGreenUpperLevel(upper: ImmutableLevel,
                                lhs: ImmutableBuffer,
                                rhs: ImmutableBuffer,
                                topSubStack: ImmutableLevel,
                                lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        assert(upper.color == RED)
        assert(this.color != RED)

        var upperLhs = upper.lhs
        var upperRhs = upper.rhs

        var thisLhs = lhs
        var thisRhs = rhs

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

        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)

        if (lowerSubStack == null && thisLhs.size == 0 && thisRhs.size == 0) {
            val newUpper = DequeBottomLevel<T>(upperLhs, upperRhs)
            return PersistentDeque(topSubStack, DequeSubStack(newUpper, null))
        }

        val newThis = this.withNewLhs(thisLhs).withNewRhs(thisRhs)  // TODO: optimize
        val nextSubStack = if (newThis.color == RED) {
            val newUpper = SubStackBottomLevel(upperLhs, upperRhs)
            DequeSubStack(newUpper, DequeSubStack(newThis, lowerSubStack))
        } else {
            val newUpper = NonBottomLevel<T>(upperLhs, upperRhs, newThis)
            DequeSubStack(newUpper, lowerSubStack)
        }

        return PersistentDeque(topSubStack, nextSubStack)
    }
}