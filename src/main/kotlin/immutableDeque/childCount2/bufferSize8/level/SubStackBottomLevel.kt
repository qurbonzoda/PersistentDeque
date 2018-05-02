package immutableDeque.childCount2.bufferSize8.level

import immutableDeque.ImmutableDeque
import immutableDeque.RED
import immutableDeque.childCount2.bufferSize8.buffer.ImmutableBuffer
import immutableDeque.childCount2.bufferSize8.persistentDeque.DequeSubStack
import immutableDeque.childCount2.bufferSize8.persistentDeque.PersistentDeque
import immutableDeque.childCount2.childCountToThePow

internal open class SubStackBottomLevel(lhs: ImmutableBuffer,
                                        rhs: ImmutableBuffer) : ImmutableLevel(lhs, rhs, null) {
    // MARK: ImmutableLevel
    override fun withNewLhs(newLhs: ImmutableBuffer): SubStackBottomLevel {
        return SubStackBottomLevel(newLhs, this.rhs)
    }

    override fun withNewRhs(newRhs: ImmutableBuffer): SubStackBottomLevel {
        return SubStackBottomLevel(this.lhs, newRhs)
    }

    override fun subStackSize(depth: Int): Int {
        return (this.lhs.size + this.rhs.size) * childCountToThePow(depth)
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

        val lhsSize = this.lhs.size * childCountToThePow(depth)
        if (index < lhsSize) {
            return this.lhs.getLeafValueAt(index, depth)
        }
        return this.rhs.getLeafValueAt(index - lhsSize, depth)
    }

    override fun setBufferLeafValueAt(index: Int, value: Any?, size: Int, depth: Int): SubStackBottomLevel {
//        assert(index < size)

        val lhsSize = this.lhs.size * childCountToThePow(depth)
        if (index < lhsSize) {
            val newLhs = this.lhs.setLeafValueAt(index, value, depth)
            return this.withNewLhs(newLhs)
        }

        val rhsSize = this.rhs.size * childCountToThePow(depth)
        val newRhs = this.rhs.setLeafValueAt(rhsSize - (size - index), value, depth)
        return this.withNewRhs(newRhs)
    }

    override fun <T> makeImmutableDeque(upperLhs: ImmutableBuffer,
                                        upperRhs: ImmutableBuffer,
                                        thisLhs: ImmutableBuffer,
                                        thisRhs: ImmutableBuffer,
                                        pDeque: PersistentDeque<*>,
                                        newSize: Int): ImmutableDeque<T> {
//        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)
//        assert(pDeque.next.stack.next != null && pDeque.next.next != null)

        val lowerSubStack = pDeque.next.next

        val newThis = SubStackBottomLevel(thisLhs, thisRhs)
        val newNext = DequeSubStack(newThis, lowerSubStack)
        return PersistentDeque(upperLhs, upperRhs, newNext, newSize)
    }

    override fun makeDequeSubStack(upperLhs: ImmutableBuffer,
                                   upperRhs: ImmutableBuffer,
                                   thisLhs: ImmutableBuffer,
                                   thisRhs: ImmutableBuffer,
                                   lowerSubStack: DequeSubStack?): DequeSubStack {
//        assert(upperLhs.color == GREEN && upperRhs.color == GREEN)
//        assert(lowerSubStack != null)

        val newThis = SubStackBottomLevel(thisLhs, thisRhs)

        return if (newThis.color == RED) {
            val newUpper = SubStackBottomLevel(upperLhs, upperRhs)
            DequeSubStack(newUpper, DequeSubStack(newThis, lowerSubStack))
        } else {
            val newUpper = NonBottomLevel(upperLhs, upperRhs, newThis)
            DequeSubStack(newUpper, lowerSubStack)
        }
    }
}

