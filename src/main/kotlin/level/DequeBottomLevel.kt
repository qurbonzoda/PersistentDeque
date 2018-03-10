package level

import buffer.*
import deque.ImmutableDeque
import persistentDeque.DequeSubStack
import persistentDeque.PersistentDeque

internal class DequeBottomLevel<T>(lhs: ImmutableBuffer,
                                   rhs: ImmutableBuffer): SubStackBottomLevel(lhs, rhs), ImmutableDeque<T> {

//    init {
//        assert((lhs.size > 0 && rhs.size > 0) || (lhs.size == 0 && rhs.size == 0))
//    }

    // MARK: ImmutableLevel
    override fun withNewLhs(newLhs: ImmutableBuffer): DequeBottomLevel<T> {
        return DequeBottomLevel(newLhs, this.rhs)
    }

    override fun withNewRhs(newRhs: ImmutableBuffer): DequeBottomLevel<T> {
        return DequeBottomLevel(this.lhs, newRhs)
    }

    override fun makeRhsFreeToPush(count: Int): ImmutableLevel {
        if (this.rhs.size + count <= MAX_BUFFER_SIZE) {
            return this
        }

        val toMakeFreeCount = this.rhs.size + count - MAX_BUFFER_SIZE
        if (this.lhs.size + toMakeFreeCount <= GREEN_HIGH) {
            val fromRhs = this.rhs.pop(this.rhs.size - toMakeFreeCount).moveToOppositeSideBuffer()
            val newLhs = this.lhs.prependSavingOrder(fromRhs)
            val newRhs = this.rhs.removeBottom(toMakeFreeCount)
            return DequeBottomLevel<Any?>(newLhs, newRhs)
        }

        val toMoveToNextRhs = ((toMakeFreeCount + 1) shr 1) shl 1

        val toLeaveForLhs = this.lhs.size - 2
        val toLeaveForRhs = this.rhs.size - toMoveToNextRhs

        val nextLevelLhs = this.lhs.pop(toLeaveForLhs).pushAllToNextLevelBuffer(LhsEmptyBuffer)
        val nextLevelRhs = this.rhs.pop(toLeaveForRhs).pushAllToNextLevelBuffer(RhsEmptyBuffer)
        val newLhs = this.lhs.removeBottom(2)
        val newRhs = this.rhs.removeBottom(toMoveToNextRhs)

        val nextLevel = DequeBottomLevel<Any?>(nextLevelLhs, nextLevelRhs)
        return NonBottomLevel<Any?>(newLhs, newRhs, nextLevel)
    }

    override fun <T> makeImmutableDeque(upperLhs: ImmutableBuffer,
                                        upperRhs: ImmutableBuffer,
                                        thisLhs: ImmutableBuffer,
                                        thisRhs: ImmutableBuffer,
                                        lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
//        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)
//        assert(lowerSubStack == null)

        if (thisLhs.size == 0 || thisRhs.size == 0 || (thisLhs.color != RED && thisRhs.color != RED)) {
            return this.makeImmutableLevel(upperLhs, upperRhs, thisLhs, thisRhs) as ImmutableDeque<T>
        }

//        assert(thisLhs.size > 0 && thisRhs.size > 0)

        val newThis = DequeBottomLevel<T>(thisLhs, thisRhs)

//        assert(newThis.color == RED)

        val newUpper = SubStackBottomLevel(upperLhs, upperRhs)
        return PersistentDeque(newUpper, DequeSubStack(newThis, null))
    }

    override fun <T> makeImmutableDeque(topSubStack: ImmutableLevel,
                                        upperLhs: ImmutableBuffer,
                                        upperRhs: ImmutableBuffer,
                                        thisLhs: ImmutableBuffer,
                                        thisRhs: ImmutableBuffer,
                                        lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
//        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)
//        assert(lowerSubStack == null)

        if (thisLhs.size == 0 || thisRhs.size == 0 || (thisLhs.color != RED && thisRhs.color != RED)) {
            val newUpper = this.makeImmutableLevel(upperLhs, upperRhs, thisLhs, thisRhs)
            return PersistentDeque(topSubStack, DequeSubStack(newUpper, null))
        }

//        assert(thisLhs.size > 0 && thisRhs.size > 0)

        val newThis = DequeBottomLevel<T>(thisLhs, thisRhs)

//        assert(newThis.color == RED)

        val newUpper = SubStackBottomLevel(upperLhs, upperRhs)
        val nextSubStack = DequeSubStack(newUpper, DequeSubStack(newThis, null))
        return PersistentDeque(topSubStack, nextSubStack)
    }

    private fun makeImmutableLevel(upperLhs: ImmutableBuffer,
                                   upperRhs: ImmutableBuffer,
                                   thisLhs: ImmutableBuffer,
                                   thisRhs: ImmutableBuffer): ImmutableLevel {
        if (thisLhs.size == 0 && thisRhs.size == 0) {
            return DequeBottomLevel<Any?>(upperLhs, upperRhs)
        }

        if (thisLhs.size == 0) {
            if (thisRhs.size > 1) {
                val newLhs = thisRhs.pop().moveToOppositeSideBuffer()
                val newRhs = thisRhs.removeBottom(thisRhs.size - 1)
                val newThis = DequeBottomLevel<T>(newLhs, newRhs)
                return NonBottomLevel<Any?>(upperLhs, upperRhs, newThis)
            }

//            assert(upperLhs.size + 2 < YELLOW_HIGH)
//            assert(thisRhs.size == 1)

            val toPrependToUpperLhs = thisRhs.moveToOppositeSideBuffer().moveToUpperLevelBuffer(1)
            val newUpperLhs = upperLhs.prependSavingOrder(toPrependToUpperLhs)

            return DequeBottomLevel<Any?>(newUpperLhs, upperRhs)
        }
        if (thisRhs.size == 0) {
            if (thisLhs.size > 1) {
                val newRhs = thisLhs.pop().moveToOppositeSideBuffer()
                val newLhs = thisLhs.removeBottom(thisLhs.size - 1)
                val newThis = DequeBottomLevel<T>(newLhs, newRhs)
                return NonBottomLevel<Any?>(upperLhs, upperRhs, newThis)
            }

//            assert(upperRhs.size + 2 < YELLOW_HIGH)
//            assert(thisLhs.size == 1)

            val toPrependToUpperRhs = thisLhs.moveToOppositeSideBuffer().moveToUpperLevelBuffer(1)
            val newUpperRhs = upperRhs.prependSavingOrder(toPrependToUpperRhs)

            return DequeBottomLevel<Any?>(upperLhs, newUpperRhs)
        }

        val newThis = DequeBottomLevel<T>(thisLhs, thisRhs)

//        assert(newThis.color != RED)

        return NonBottomLevel<Any?>(upperLhs, upperRhs, newThis)
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
//            assert(this.lhs.size != 0)
            return this.lhs.top as T
        }

    override val last: T
        get() {
//            assert(this.rhs.size != 0)
            return this.rhs.top as T
        }

    override fun addFirst(value: T): ImmutableDeque<T> {
        if (this.lhs.size + 1 < MAX_BUFFER_SIZE) {
            val newLhs = this.lhs.push(value)
            return this.withNewLhs(newLhs)
        }

//        println(this.size + 1 - lastRegularizationSize)
//        lastRegularizationSize = this.size + 1

        val canMoveToRhs = YELLOW_HIGH - this.rhs.size

        if (canMoveToRhs >= MIN_COUNT_FULL_BOTTOM_LEVEL_DEQUE_SHOULD_MOVE_TO_OPPOSITE_SIDE) {
            val toLeaveForLhs = this.lhs.size - canMoveToRhs
            val fromLhs = this.lhs.pop(toLeaveForLhs).moveToOppositeSideBuffer()
            val newRhs = this.rhs.prependSavingOrder(fromLhs)
            val newLhs = this.lhs.removeBottom(canMoveToRhs).push(value)
            return DequeBottomLevel(newLhs, newRhs)
        }

        val toMoveToNextLevel = FULL_BOTTOM_LEVEL_DEQUE_SHOULD_MOVE_TO_NEXT_LEVEL
        val toLeaveForLhs = this.lhs.size - toMoveToNextLevel
        val toLeaveForRhs = this.rhs.size - toMoveToNextLevel

        val nextLevelLhs = this.lhs.pop(toLeaveForLhs).pushAllToNextLevelBuffer(LhsEmptyBuffer)
        val nextLevelRhs = this.rhs.pop(toLeaveForRhs).pushAllToNextLevelBuffer(RhsEmptyBuffer)
        val newLhs = this.lhs.removeBottom(toMoveToNextLevel).push(value)
        val newRhs = this.rhs.removeBottom(toMoveToNextLevel)

//        assert(newLhs.color == GREEN && newRhs.color == GREEN)

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

//        println(this.size + 1 - lastRegularizationSize)
//        lastRegularizationSize = this.size + 1

        val canMoveToLhs = YELLOW_HIGH - this.lhs.size

        if (canMoveToLhs >= MIN_COUNT_FULL_BOTTOM_LEVEL_DEQUE_SHOULD_MOVE_TO_OPPOSITE_SIDE) {
            val toLeaveForRhs = this.rhs.size - canMoveToLhs
            val fromRhs = this.rhs.pop(toLeaveForRhs).moveToOppositeSideBuffer()
            val newLhs = this.lhs.prependSavingOrder(fromRhs)
            val newRhs = this.rhs.removeBottom(canMoveToLhs).push(value)
            return DequeBottomLevel(newLhs, newRhs)
        }

        val toMoveToNextLevel = FULL_BOTTOM_LEVEL_DEQUE_SHOULD_MOVE_TO_NEXT_LEVEL
        val toLeaveForRhs = this.rhs.size - toMoveToNextLevel
        val toLeaveForLhs = this.lhs.size - toMoveToNextLevel

        val nextLevelRhs = this.rhs.pop(toLeaveForRhs).pushAllToNextLevelBuffer(RhsEmptyBuffer)
        val nextLevelLhs = this.lhs.pop(toLeaveForLhs).pushAllToNextLevelBuffer(LhsEmptyBuffer)
        val newRhs = this.rhs.removeBottom(toMoveToNextLevel).push(value)
        val newLhs = this.lhs.removeBottom(toMoveToNextLevel)

//        assert(newLhs.color == GREEN && newRhs.color == GREEN)

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
//        assert(index < this.size)
        return this.getBufferLeafValueAt(index, this.size, 0) as T
    }

    override fun set(index: Int, value: T): ImmutableDeque<T> {
//        assert(index < this.size)
        return this.setBufferLeafValueAt(index, value, this.size, 0) as ImmutableDeque<T>
    }
}