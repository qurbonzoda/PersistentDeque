package level

import buffer.*
import deque.ImmutableDeque
import persistentDeque.DequeSubStack
import persistentDeque.NextHolder
import persistentDeque.PersistentDeque

internal open class SubStackBottomLevel(lhs: ImmutableBuffer,
                                        rhs: ImmutableBuffer): ImmutableLevel(lhs, rhs, null) {
    // MARK: ImmutableLevel
    override fun withNewLhs(newLhs: ImmutableBuffer): SubStackBottomLevel {
        return SubStackBottomLevel(newLhs, this.rhs)
    }

    override fun withNewRhs(newRhs: ImmutableBuffer): SubStackBottomLevel {
        return SubStackBottomLevel(this.lhs, newRhs)
    }

    override fun subStackSize(depth: Int): Int {
        return (this.lhs.size + this.rhs.size) shl depth
    }

    override fun subStackHeight(): Int {
        return 1
    }

    override fun addBufferLeafValuesTo(list: MutableList<Any?>, depth: Int) {
        this.lhs.addLeafValuesTo(list, depth)
        this.rhs.addLeafValuesTo(list, depth)
    }

    override fun getBufferLeafValueAt(index: Int, size: Int, depth: Int): Any? {
//        assert(index < size)

        val lhsSize = this.lhs.size shl depth
        if (index < lhsSize) {
            return this.lhs.getLeafValueAt(index, depth)
        }
        return this.rhs.getLeafValueAt(index - lhsSize, depth)
    }

    override fun setBufferLeafValueAt(index: Int, value: Any?, size: Int, depth: Int): SubStackBottomLevel {
//        assert(index < size)

        val lhsSize = this.lhs.size shl depth
        if (index < lhsSize) {
            val newLhs = this.lhs.setLeafValueAt(index, value, depth)
            return this.withNewLhs(newLhs)
        }

        val rhsSize = this.rhs.size shl depth
        val newRhs = this.rhs.setLeafValueAt(rhsSize - (size - index), value, depth)
        return this.withNewRhs(newRhs)
    }

    override fun <T> makeImmutableDeque(upperLhs: ImmutableBuffer,
                                        upperRhs: ImmutableBuffer,
                                        thisLhs: ImmutableBuffer,
                                        thisRhs: ImmutableBuffer,
                                        lowerSubStack: DequeSubStack?,
                                        newSize: Int): ImmutableDeque<T> {
//        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)
//        assert(lowerSubStack != null)

        val newThis = SubStackBottomLevel(thisLhs, thisRhs)
        if (newThis.color == RED) {
            val newNext = NextHolder(null, DequeSubStack(newThis, lowerSubStack))
            return PersistentDeque(upperLhs, upperRhs, newNext, newSize)
        }

        val newNext = NextHolder(newThis, lowerSubStack)
        return PersistentDeque(upperLhs, upperRhs, newNext, newSize)
    }

    override fun <T> makeImmutableDeque(topSubStack: ImmutableLevel,
                                        upperLhs: ImmutableBuffer,
                                        upperRhs: ImmutableBuffer,
                                        thisLhs: ImmutableBuffer,
                                        thisRhs: ImmutableBuffer,
                                        lowerSubStack: DequeSubStack?,
                                        newSize: Int): ImmutableDeque<T> {
//        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)
//        assert(lowerSubStack != null)

        val newThis = SubStackBottomLevel(thisLhs, thisRhs)
        val nextSubStack = if (newThis.color == RED) {
            val newUpper = SubStackBottomLevel(upperLhs, upperRhs)
            DequeSubStack(newUpper, DequeSubStack(newThis, lowerSubStack))
        } else {
            val newUpper = NonBottomLevel(upperLhs, upperRhs, newThis)
            DequeSubStack(newUpper, lowerSubStack)
        }

        val newNext = NextHolder(topSubStack.next, nextSubStack)
        return PersistentDeque(topSubStack.lhs, topSubStack.rhs, newNext, newSize)
    }
}

