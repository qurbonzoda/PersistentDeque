package level

import buffer.*
import deque.ImmutableDeque
import persistentDeque.DequeSubStack
import persistentDeque.PersistentDeque

internal class NonBottomLevel<T>(lhs: ImmutableBuffer,
                                 rhs: ImmutableBuffer,
                                 override val next: ImmutableLevel): ImmutableLevel(lhs, rhs, next), ImmutableDeque<T> {
    // MARK: ImmutableLevel
    override fun withNewLhs(newLhs: ImmutableBuffer): NonBottomLevel<T> {
        return NonBottomLevel(newLhs, this.rhs, this.next)
    }

    override fun withNewRhs(newRhs: ImmutableBuffer): NonBottomLevel<T> {
        return NonBottomLevel(this.lhs, newRhs, this.next)
    }

    override fun subStackSize(depth: Int): Int {
        return ((this.lhs.size + this.rhs.size) shl depth) + this.next.subStackSize(depth + 1)
    }

    override fun subStackHeight(): Int {
        return this.next.subStackHeight() + 1
    }

    override fun addBufferLeafValuesTo(list: MutableList<Any?>, depth: Int) {
        this.lhs.addLeafValuesTo(list, depth)
        this.next.addBufferLeafValuesTo(list, depth + 1)
        this.rhs.addLeafValuesTo(list, depth)
    }

    override fun getBufferLeafValueAt(index: Int, size: Int, depth: Int): Any? {
//        assert(index < size)

        val lhsSize = this.lhs.size shl depth
        val rhsSize = this.rhs.size shl depth
        if (index < lhsSize) {
            return this.lhs.getLeafValueAt(index, depth)
        }
        if (size - index <= rhsSize) {
            return this.rhs.getLeafValueAt(rhsSize - (size - index), depth)
        }
        return this.next.getBufferLeafValueAt(index - lhsSize, size - lhsSize - rhsSize, depth + 1)
    }

    override fun setBufferLeafValueAt(index: Int, value: Any?, size: Int, depth: Int): NonBottomLevel<T> {
//        assert(index < size)

        val lhsSize = this.lhs.size shl depth
        val rhsSize = this.rhs.size shl depth
        if (index < lhsSize) {
            val newLhs = this.lhs.setLeafValueAt(index, value, depth)
            return this.withNewLhs(newLhs)
        }
        if (size - index <= rhsSize) {
            val newRhs = this.rhs.setLeafValueAt(rhsSize - (size - index), value, depth)
            return this.withNewRhs(newRhs)
        }
        val newNext = this.next.setBufferLeafValueAt(index - lhsSize, value, size - lhsSize - rhsSize, depth + 1)
        return this.withNewNext(newNext)
    }

    override fun makeRhsFreeToPush(count: Int): ImmutableLevel {
        if (this.rhs.size + count <= MAX_BUFFER_SIZE) {
            return this
        }

        val toMakeFreeCount = this.rhs.size + count - MAX_BUFFER_SIZE
        val newEvenCount = ((toMakeFreeCount + 1) shr 1) shl 1

        val nextLevel = this.next.makeRhsFreeToPush((newEvenCount shr 1) + 1)

        val toMoveToNextRhs = minOf(newEvenCount, (YELLOW_HIGH - nextLevel.rhs.size) shl 1)
        val toLeaveForRhs = this.rhs.size - toMoveToNextRhs

        val newNextRhs = this.rhs.pop(toLeaveForRhs).pushAllToNextLevelBuffer(nextLevel.rhs)
        val newRhs = this.rhs.removeBottom(toMoveToNextRhs)

        val newNextLevel = nextLevel.withNewRhs(newNextRhs)

//        assert(newNextLevel.color != RED)

        return NonBottomLevel<Any?>(this.lhs, newRhs, newNextLevel)
    }

    override fun <T> makeImmutableDeque(upperLhs: ImmutableBuffer,
                                        upperRhs: ImmutableBuffer,
                                        thisLhs: ImmutableBuffer,
                                        thisRhs: ImmutableBuffer,
                                        lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
//        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)

        val newThis = NonBottomLevel<T>(thisLhs, thisRhs, this.next)
        if (newThis.color == RED) {
            val newUpper = SubStackBottomLevel(upperLhs, upperRhs)
            return PersistentDeque(newUpper, DequeSubStack(newThis, lowerSubStack))
        }

        val newUpper = NonBottomLevel<T>(upperLhs, upperRhs, newThis)
        if (lowerSubStack == null) {
            return newUpper
        }

        return PersistentDeque(newUpper, lowerSubStack)
    }

    override fun <T> makeImmutableDeque(topSubStack: ImmutableLevel,
                                        upperLhs: ImmutableBuffer,
                                        upperRhs: ImmutableBuffer,
                                        thisLhs: ImmutableBuffer,
                                        thisRhs: ImmutableBuffer,
                                        lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
//        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)

        val newThis = NonBottomLevel<T>(thisLhs, thisRhs, this.next)
        val nextSubStack = if (newThis.color == RED) {
            val newUpper = SubStackBottomLevel(upperLhs, upperRhs)
            DequeSubStack(newUpper, DequeSubStack(newThis, lowerSubStack))
        } else {
            val newUpper = NonBottomLevel<T>(upperLhs, upperRhs, newThis)
            DequeSubStack(newUpper, lowerSubStack)
        }

        return PersistentDeque(topSubStack, nextSubStack)
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

        return this.next.makeGreenUpperLevelPushingLhs(this, value, null)
    }

    override fun removeFirst(): ImmutableDeque<T> {
        if (this.lhs.size > 1) {
            val newLhs = this.lhs.pop()
            return this.withNewLhs(newLhs)
        }

//        println(this.size - 1 - lastRegularizationSize)
//        lastRegularizationSize = this.size - 1

        return this.next.makeGreenUpperLevelPoppingLhs(this, null)
    }

    override fun addLast(value: T): ImmutableDeque<T> {
        if (this.rhs.size + 1 < MAX_BUFFER_SIZE) {
            val newRhs = rhs.push(value)
            return this.withNewRhs(newRhs)
        }

//        println(this.size + 1 - lastRegularizationSize)
//        lastRegularizationSize = this.size + 1

        return this.next.makeGreenUpperLevelPushingRhs(this, value, null)
    }

    override fun removeLast(): ImmutableDeque<T> {
        if (this.rhs.size > 1) {
            val newRhs = this.rhs.pop()
            return this.withNewRhs(newRhs)
        }

//        println(this.size - 1 - lastRegularizationSize)
//        lastRegularizationSize = this.size - 1

        return this.next.makeGreenUpperLevelPoppingRhs(this, null)
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
        return this.setBufferLeafValueAt(index, value, this.size, 0)
    }

    // MARK: util
    private fun withNewNext(newNext: ImmutableLevel): NonBottomLevel<T> {
        return NonBottomLevel(this.lhs, this.rhs, newNext)
    }
}