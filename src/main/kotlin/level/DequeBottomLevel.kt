package level

import buffer.*
import deque.ImmutableDeque
import persistentDeque.DequeSubStack
import persistentDeque.PersistentDeque

internal class DequeBottomLevel<T>(lhs: ImmutableBuffer,
                                   rhs: ImmutableBuffer): SubStackBottomLevel(lhs, rhs), ImmutableLevelDeque<T> {
    // MARK: ImmutableLevel
    override val color: Int
        get() {
            if (this.lhs.size == 0) return this.rhs.color
            if (this.rhs.size == 0) return this.lhs.color
            return minOf(this.lhs.color, this.rhs.color)
        }

    override fun withNewLhs(newLhs: ImmutableBuffer): DequeBottomLevel<T> {
        return DequeBottomLevel(newLhs, this.rhs)
    }

    override fun withNewRhs(newRhs: ImmutableBuffer): DequeBottomLevel<T> {
        return DequeBottomLevel(this.lhs, newRhs)
    }

    override fun <T> makeGreenUpperLevelPushingLhs(upper: ImmutableLevel, valueToPush: Any?, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        assert(lowerSubStack == null)

        var thisLhs = this.lhs
        var thisRhs = this.rhs

        if (upper.rhs.size == YELLOW_LOW && thisRhs.size == 0) {
            thisRhs = thisLhs.moveToOppositeSideBuffer()
            thisLhs = LhsEmptyBuffer
        }

        return this.makeGreenUpperLevelPushingLhs(upper, thisLhs, thisRhs, valueToPush, lowerSubStack)
    }

    override fun <T> makeGreenUpperLevelPushingRhs(upper: ImmutableLevel, valueToPush: Any?, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        assert(lowerSubStack == null)

        var thisLhs = this.lhs
        var thisRhs = this.rhs

        if (upper.lhs.size == YELLOW_LOW && thisLhs.size == 0) {
            thisLhs = thisRhs.moveToOppositeSideBuffer()
            thisRhs = RhsEmptyBuffer
        }

        return this.makeGreenUpperLevelPushingRhs(upper, thisLhs, thisRhs, valueToPush, lowerSubStack)
    }

    override fun <T> makeGreenUpperLevelPoppingLhs(upper: ImmutableLevel, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        assert(lowerSubStack == null)

        var thisLhs = this.lhs
        var thisRhs = this.rhs

        if (thisLhs.size == 0) {
            thisLhs = thisRhs.pop(1).moveToOppositeSideBuffer()
            thisRhs = thisRhs.removeBottom(thisRhs.size - 1)
        }

        return this.makeGreenUpperLevelPoppingLhs(upper, thisLhs, thisRhs, lowerSubStack)
    }

    override fun <T> makeGreenUpperLevelPoppingRhs(upper: ImmutableLevel, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        assert(lowerSubStack == null)

        var thisLhs = this.lhs
        var thisRhs = this.rhs

        if (thisRhs.size == 0) {
            thisRhs = thisLhs.pop(1).moveToOppositeSideBuffer()
            thisLhs = thisLhs.removeBottom(thisLhs.size - 1)
        }

        return this.makeGreenUpperLevelPoppingRhs(upper, thisLhs, thisRhs, lowerSubStack)
    }

    override fun <T> makeGreenUpperLevel(upper: ImmutableLevel, topSubStack: ImmutableLevel, lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        assert(lowerSubStack == null)

        var thisLhs = this.lhs
        var thisRhs = this.rhs

        if (upper.lhs.size <= YELLOW_LOW && thisLhs.size == 0) {
            thisLhs = thisRhs.pop(1).moveToOppositeSideBuffer()
            thisRhs = thisRhs.removeBottom(thisRhs.size - 1)
        } else if (upper.rhs.size <= YELLOW_LOW && thisRhs.size == 0) {
            thisRhs = thisLhs.pop(1).moveToOppositeSideBuffer()
            thisLhs = thisLhs.removeBottom(thisLhs.size - 1)
        }

        return this.makeGreenUpperLevel(upper, thisLhs, thisRhs, topSubStack, lowerSubStack)
    }

    override fun <T> makeImmutableDeque(upperLhs: ImmutableBuffer,
                                        upperRhs: ImmutableBuffer,
                                        thisLhs: ImmutableBuffer,
                                        thisRhs: ImmutableBuffer,
                                        lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)
        assert(lowerSubStack == null)

        if (thisLhs.size == 0 && thisRhs.size == 0) {
            return DequeBottomLevel(upperLhs, upperRhs)
        }

//        assert(thisLhs.size > 0 && thisRhs.size > 0)
        assert(thisLhs.size + thisRhs.size > 1)

        val newThis = DequeBottomLevel<T>(thisLhs, thisRhs)
        if (newThis.color == RED) {
            val newUpper = SubStackBottomLevel(upperLhs, upperRhs)
            return PersistentDeque(newUpper, DequeSubStack(newThis, null))
        }

        return NonBottomLevel(upperLhs, upperRhs, newThis)
    }

    // MARK: ImmutableDeque
    override val size: Int
        get() {
            return this.subStackSize(0)
        }

    override fun isEmpty(): Boolean {
        return false
    }

    override val first: T
        get() {
            assert(this.lhs.size != 0)
            return this.lhs.top as T
        }

    override val last: T
        get() {
            assert(this.rhs.size != 0)
            return this.rhs.top as T
        }

    override fun addFirst(value: T): ImmutableDeque<T> {
        if (this.lhs.size + 1 < MAX_BUFFER_SIZE) {
            val newLhs = this.lhs.push(value)
            return this.withNewLhs(newLhs)
        }

        val canMoveToRhs = YELLOW_HIGH - this.rhs.size

        if (canMoveToRhs > 0) { // TODO: consider finding optimal value
            val toLeaveForLhs = this.lhs.size - canMoveToRhs
            val fromLhs = this.lhs.pop(toLeaveForLhs).moveToOppositeSideBuffer()
            val newRhs = this.rhs.prependSavingOrder(fromLhs)
            val newLhs = this.lhs.removeBottom(canMoveToRhs).push(value)
            return DequeBottomLevel(newLhs, newRhs)
        }

        val toPushToNextLevel = (MAX_BUFFER_SIZE shr 2) shl 1   // make even number
        val toLeaveForLhs = this.lhs.size - toPushToNextLevel
        val toLeaveForRhs = this.rhs.size - toPushToNextLevel

        val nextLevelLhs = this.lhs.pop(toLeaveForLhs).pushAllToNextLevelBuffer(LhsEmptyBuffer)
        val nextLevelRhs = this.rhs.pop(toLeaveForRhs).pushAllToNextLevelBuffer(RhsEmptyBuffer)
        val newLhs = this.lhs.removeBottom(toPushToNextLevel).push(value)
        val newRhs = this.rhs.removeBottom(toPushToNextLevel)

        assert(newLhs.color == GREEN && newRhs.color == GREEN)

        val nextLevel = DequeBottomLevel<T>(nextLevelLhs, nextLevelRhs)
        return NonBottomLevel(newLhs, newRhs, nextLevel)
    }

    override fun removeFirst(): ImmutableDeque<T> {
        if (this.lhs.size == 1) {
            return this.rhs as ImmutableDeque<T>
        }
        val newLhs = this.lhs.pop()
        return this.withNewLhs(newLhs)
    }

    override fun addLast(value: T): ImmutableDeque<T> {
        if (this.rhs.size + 1 < MAX_BUFFER_SIZE) {
            val newRhs = this.rhs.push(value)
            return this.withNewRhs(newRhs)
        }

        val canMoveToLhs = YELLOW_HIGH - this.lhs.size

        if (canMoveToLhs > 0) { // TODO: consider finding optimal value
            val toLeaveForRhs = this.rhs.size - canMoveToLhs
            val fromRhs = this.rhs.pop(toLeaveForRhs).moveToOppositeSideBuffer()
            val newLhs = this.lhs.prependSavingOrder(fromRhs)
            val newRhs = this.rhs.removeBottom(canMoveToLhs).push(value)
            return DequeBottomLevel(newLhs, newRhs)

        }

        val toPushToNextLevel = (MAX_BUFFER_SIZE shr 2) shl 1   // make even number
        val toLeaveForRhs = this.rhs.size - toPushToNextLevel
        val toLeaveForLhs = this.lhs.size - toPushToNextLevel

        val nextLevelRhs = this.rhs.pop(toLeaveForRhs).pushAllToNextLevelBuffer(RhsEmptyBuffer)
        val nextLevelLhs = this.lhs.pop(toLeaveForLhs).pushAllToNextLevelBuffer(LhsEmptyBuffer)
        val newRhs = this.rhs.removeBottom(toPushToNextLevel).push(value)
        val newLhs = this.lhs.removeBottom(toPushToNextLevel)

        assert(newLhs.color == GREEN && newRhs.color == GREEN)

        val nextLevel = DequeBottomLevel<T>(nextLevelLhs, nextLevelRhs)
        return NonBottomLevel(newLhs, newRhs, nextLevel)
    }

    override fun removeLast(): ImmutableDeque<T> {
        if (this.rhs.size == 1) {
            return this.lhs as ImmutableDeque<T>
        }
        val newRhs = this.rhs.pop()
        return this.withNewRhs(newRhs)
    }

    override fun toList(): List<T> {
        val list = mutableListOf<Any?>()
        this.addBufferLeafValuesTo(list, 0)
        return list.toList() as List<T>
    }

    override fun get(index: Int): T {
        assert(index < this.size)
        return this.getBufferLeafValueAt(index, this.size, 0) as T
    }

    override fun set(index: Int, value: T): ImmutableDeque<T> {
        assert(index < this.size)
        return this.setBufferLeafValueAt(index, value, this.size, 0) as ImmutableDeque<T>
    }
}