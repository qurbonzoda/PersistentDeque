package immutableDeque.childCount32.bufferSize512.level

import immutableDeque.ImmutableDeque
import immutableDeque.RED
import immutableDeque.childCount32.CHILD_COUNT
import immutableDeque.childCount32.bufferSize512.buffer.ImmutableBuffer
import immutableDeque.childCount32.bufferSize512.buffer.LhsEmptyBuffer
import immutableDeque.childCount32.bufferSize512.buffer.RhsEmptyBuffer
import immutableDeque.childCount32.bufferSize512.constants.FULL_BOTTOM_LEVEL_DEQUE_SHOULD_MOVE_TO_NEXT_LEVEL
import immutableDeque.childCount32.bufferSize512.constants.MAX_BUFFER_SIZE
import immutableDeque.childCount32.bufferSize512.constants.MIN_COUNT_FULL_BOTTOM_LEVEL_DEQUE_SHOULD_MOVE_TO_OPPOSITE_SIDE
import immutableDeque.childCount32.bufferSize512.constants.YELLOW_HIGH
import immutableDeque.childCount32.bufferSize512.persistentDeque.DequeSubStack
import immutableDeque.childCount32.bufferSize512.persistentDeque.PersistentDeque

internal class DequeBottomLevel<T>(lhs: ImmutableBuffer,
                                   rhs: ImmutableBuffer) : SubStackBottomLevel(lhs, rhs), ImmutableDeque<T> {

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

    override fun <T> makeImmutableDeque(upperLhs: ImmutableBuffer,
                                        upperRhs: ImmutableBuffer,
                                        thisLhs: ImmutableBuffer,
                                        thisRhs: ImmutableBuffer,
                                        pDeque: PersistentDeque<*>,
                                        newSize: Int): ImmutableDeque<T> {
//        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)
//        assert(pDeque.next.stack.next == null && pDeque.next.next == null)

        if (thisLhs.size == 0 || thisRhs.size == 0 || (thisLhs.color != RED && thisRhs.color != RED)) {
            val newUpper = this.makeImmutableLevel(upperLhs, upperRhs, thisLhs, thisRhs)
            if (newUpper.next == null) {
                return newUpper as ImmutableDeque<T>
            }
            val newNext = DequeSubStack(newUpper.next!!, null)
            return PersistentDeque(newUpper.lhs, newUpper.rhs, newNext, newSize)
        }

//        assert(thisLhs.size > 0 && thisRhs.size > 0)

        val newThis = DequeBottomLevel<T>(thisLhs, thisRhs)

//        assert(newThis.color == RED)

        val newNext = DequeSubStack(newThis, null)
        return PersistentDeque(upperLhs, upperRhs, newNext, newSize)
    }

    override fun makeDequeSubStack(upperLhs: ImmutableBuffer,
                                   upperRhs: ImmutableBuffer,
                                   thisLhs: ImmutableBuffer,
                                   thisRhs: ImmutableBuffer,
                                   lowerSubStack: DequeSubStack?): DequeSubStack {
//        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)
//        assert(lowerSubStack == null)

        if (thisLhs.size == 0 || thisRhs.size == 0 || (thisLhs.color != RED && thisRhs.color != RED)) {
            val newUpper = this.makeImmutableLevel(upperLhs, upperRhs, thisLhs, thisRhs)
            return DequeSubStack(newUpper, null)
        }

//        assert(thisLhs.size > 0 && thisRhs.size > 0)

        val newThis = DequeBottomLevel<T>(thisLhs, thisRhs)

//        assert(newThis.color == RED)

        val newUpper = SubStackBottomLevel(upperLhs, upperRhs)
        return DequeSubStack(newUpper, DequeSubStack(newThis, null))
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
                return NonBottomLevel(upperLhs, upperRhs, newThis)
            }
            if (upperLhs.size + CHILD_COUNT < YELLOW_HIGH) {
                val toPrependToUpperLhs = thisRhs.moveToOppositeSideBuffer().moveToUpperLevelBuffer(1)
                val newUpperLhs = upperLhs.prependSavingOrder(toPrependToUpperLhs)
                return DequeBottomLevel<Any?>(newUpperLhs, upperRhs)
            }

//            assert(upperLhs.size - CHILD_COUNT > YELLOW_LOW)
//            assert(thisRhs.size == 1)

            val newThisLhs = upperLhs.pop(upperLhs.size - CHILD_COUNT).pushAllToNextLevelBuffer(thisLhs)
            val newUpperLhs = upperLhs.removeBottom(CHILD_COUNT)
            val newThis = DequeBottomLevel<T>(newThisLhs, thisRhs)
            return NonBottomLevel(newUpperLhs, upperRhs, newThis)
        }
        if (thisRhs.size == 0) {
            if (thisLhs.size > 1) {
                val newRhs = thisLhs.pop().moveToOppositeSideBuffer()
                val newLhs = thisLhs.removeBottom(thisLhs.size - 1)
                val newThis = DequeBottomLevel<T>(newLhs, newRhs)
                return NonBottomLevel(upperLhs, upperRhs, newThis)
            }
            if (upperRhs.size + CHILD_COUNT < YELLOW_HIGH) {
                val toPrependToUpperRhs = thisLhs.moveToOppositeSideBuffer().moveToUpperLevelBuffer(1)
                val newUpperRhs = upperRhs.prependSavingOrder(toPrependToUpperRhs)
                return DequeBottomLevel<Any?>(upperLhs, newUpperRhs)
            }

//            assert(upperRhs.size - CHILD_COUNT > YELLOW_LOW)
//            assert(thisLhs.size == 1)

            val newThisRhs = upperRhs.pop(upperRhs.size - CHILD_COUNT).pushAllToNextLevelBuffer(thisRhs)
            val newUpperRhs = upperRhs.removeBottom(CHILD_COUNT)
            val newThis = DequeBottomLevel<T>(thisLhs, newThisRhs)
            return NonBottomLevel(upperLhs, newUpperRhs, newThis)
        }

        val newThis = DequeBottomLevel<T>(thisLhs, thisRhs)

//        assert(newThis.color != RED)

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
        val next = DequeSubStack(nextLevel, null)
        return PersistentDeque(newLhs, newRhs, next, this.size + 1)
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
        val next = DequeSubStack(nextLevel, null)
        return PersistentDeque(newLhs, newRhs, next, this.size + 1)
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
