package level

import buffer.*
import deque.ImmutableDeque
import persistentDeque.DequeSubStack
import persistentDeque.PersistentDeque

internal open class SubStackBottomLevel(override val lhs: ImmutableBuffer,
                                        override val rhs: ImmutableBuffer): ImmutableLevel {
    // MARK: ImmutableLevel
    override val color: Int
        get() {
            return minOf(this.lhs.color, this.rhs.color)
        }

    override val next: ImmutableLevel?
        get() {
            return null
        }

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
        return 0
    }

    override fun addBufferLeafValuesTo(list: MutableList<Any?>, depth: Int) {
        this.lhs.addLeafValuesTo(list, depth)
        this.rhs.addLeafValuesTo(list, depth)
    }

    override fun getBufferLeafValueAt(index: Int, size: Int, depth: Int): Any? {
        assert(index < size)

        val lhsSize = this.lhs.size shl depth
        if (index < lhsSize) {
            return this.lhs.getLeafValueAt(index, depth)
        }
        return this.rhs.getLeafValueAt(index - lhsSize, depth)
    }

    override fun setBufferLeafValueAt(index: Int, value: Any?, size: Int, depth: Int): SubStackBottomLevel {
        assert(index < size)

        val lhsSize = this.lhs.size shl depth
        if (index < lhsSize) {
            val newLhs = this.lhs.setLeafValueAt(index, value, depth)
            return this.withNewLhs(newLhs)
        }
        val newRhs = this.rhs.setLeafValueAt(index - lhsSize, value, depth)
        return this.withNewRhs(newRhs)
    }

    override fun <T> makeImmutableDeque(upperLhs: ImmutableBuffer,
                                        upperRhs: ImmutableBuffer,
                                        thisLhs: ImmutableBuffer,
                                        thisRhs: ImmutableBuffer,
                                        lowerSubStack: DequeSubStack?): ImmutableDeque<T> {
        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)
        assert(lowerSubStack != null)

        val newThis = SubStackBottomLevel(thisLhs, thisRhs)
        if (newThis.color == RED) {
            val newUpper = SubStackBottomLevel(upperLhs, upperRhs)
            return PersistentDeque(newUpper, DequeSubStack(newThis, lowerSubStack))
        }

        val newUpper = NonBottomLevel<T>(upperLhs, upperRhs, newThis)
        return PersistentDeque(newUpper, lowerSubStack!!)
    }
}

