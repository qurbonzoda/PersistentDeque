package immutableDeque.childCount3.bufferSize32.level

import immutableDeque.ImmutableDeque
import immutableDeque.RED
import immutableDeque.childCount3.bufferSize32.buffer.ImmutableBuffer
import immutableDeque.childCount3.bufferSize32.persistentDeque.DequeSubStack
import immutableDeque.childCount3.bufferSize32.persistentDeque.PersistentDeque
import immutableDeque.childCount3.childCountToThePow

internal class NonBottomLevel(lhs: ImmutableBuffer,
                              rhs: ImmutableBuffer,
                              override val next: ImmutableLevel) : ImmutableLevel(lhs, rhs, next) {
    // MARK: ImmutableLevel
    override fun withNewLhs(newLhs: ImmutableBuffer): NonBottomLevel {
        return NonBottomLevel(newLhs, this.rhs, this.next)
    }

    override fun withNewRhs(newRhs: ImmutableBuffer): NonBottomLevel {
        return NonBottomLevel(this.lhs, newRhs, this.next)
    }

    override fun subStackSize(depth: Int): Int {
        return ((this.lhs.size + this.rhs.size) * childCountToThePow(depth)) + this.next.subStackSize(depth + 1)
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

        val lhsSize = this.lhs.size * childCountToThePow(depth)
        val rhsSize = this.rhs.size * childCountToThePow(depth)
        if (index < lhsSize) {
            return this.lhs.getLeafValueAt(index, depth)
        }
        if (size - index <= rhsSize) {
            return this.rhs.getLeafValueAt(rhsSize - (size - index), depth)
        }
        return this.next.getBufferLeafValueAt(index - lhsSize, size - lhsSize - rhsSize, depth + 1)
    }

    override fun setBufferLeafValueAt(index: Int, value: Any?, size: Int, depth: Int): NonBottomLevel {
//        assert(index < size)

        val lhsSize = this.lhs.size * childCountToThePow(depth)
        val rhsSize = this.rhs.size * childCountToThePow(depth)
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

    override fun <T> makeImmutableDeque(upperLhs: ImmutableBuffer,
                                        upperRhs: ImmutableBuffer,
                                        thisLhs: ImmutableBuffer,
                                        thisRhs: ImmutableBuffer,
                                        pDeque: PersistentDeque<*>,
                                        newSize: Int): ImmutableDeque<T> {
//        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)

        val lowerSubStack = pDeque.next.next

        val newThis = NonBottomLevel(thisLhs, thisRhs, this.next)
        val newNext = DequeSubStack(newThis, lowerSubStack)
        return PersistentDeque(upperLhs, upperRhs, newNext, newSize)
    }

    override fun makeDequeSubStack(upperLhs: ImmutableBuffer,
                                   upperRhs: ImmutableBuffer,
                                   thisLhs: ImmutableBuffer,
                                   thisRhs: ImmutableBuffer,
                                   lowerSubStack: DequeSubStack?): DequeSubStack {
//        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)

        val newThis = NonBottomLevel(thisLhs, thisRhs, this.next)

        return if (newThis.color == RED) {
            val newUpper = SubStackBottomLevel(upperLhs, upperRhs)
            DequeSubStack(newUpper, DequeSubStack(newThis, lowerSubStack))
        } else {
            val newUpper = NonBottomLevel(upperLhs, upperRhs, newThis)
            DequeSubStack(newUpper, lowerSubStack)
        }
    }

    // MARK: util
    private fun withNewNext(newNext: ImmutableLevel): NonBottomLevel {
        return NonBottomLevel(this.lhs, this.rhs, newNext)
    }
}
