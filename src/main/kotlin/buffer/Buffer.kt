package buffer

const val RED = 0
const val YELLOW = 1
const val GREEN = 2


sealed class Buffer {
    abstract val color: Int
    abstract val size: Int
    abstract val first: Any?
    abstract val last: Any?
    abstract fun addFirst(element: Any?): Buffer
    abstract fun addFirstTwo(element1: Any?, element2: Any?): Buffer
    abstract fun addLast(element: Any?): Buffer
    abstract fun addLastTwo(element1: Any?, element2: Any?): Buffer
    abstract fun removeFirst(): Buffer
    abstract fun removeFirstTwo(): Buffer
    abstract fun removeLast(): Buffer
    abstract fun removeLastTwo(): Buffer
    abstract fun addElementsTo(l: ArrayList<Any?>)
    abstract fun addFirstPairOfLastTwoElementsTo(buffer: Buffer): Buffer
    abstract fun addLastPairOfFirstTwoElementsTo(buffer: Buffer): Buffer
    abstract fun addLastFirstTwoElementsTo(buffer: Buffer): Buffer
    abstract fun addFirstLastTwoElementsTo(buffer: Buffer): Buffer
}

object EmptyBuffer : Buffer() {
    override val color: Int = RED
    override val size: Int = 0

    override val first: Any?
        get() = throw UnsupportedOperationException()
    override val last: Any?
        get() = throw UnsupportedOperationException()

    override fun addFirst(element: Any?) = BufferOfOne(element)
    override fun addFirstTwo(element1: Any?, element2: Any?) = BufferOfTwo(element1, element2)
    override fun addLast(element: Any?) = BufferOfOne(element)
    override fun addLastTwo(element1: Any?, element2: Any?) = BufferOfTwo(element1, element2)

    override fun removeFirst() = throw UnsupportedOperationException()
    override fun removeFirstTwo() = throw UnsupportedOperationException()
    override fun removeLast() = throw UnsupportedOperationException()
    override fun removeLastTwo() = throw UnsupportedOperationException()

    override fun addElementsTo(l: ArrayList<Any?>) { /* Do nothing */ }
    override fun addFirstPairOfLastTwoElementsTo(buffer: Buffer) = throw UnsupportedOperationException()
    override fun addLastPairOfFirstTwoElementsTo(buffer: Buffer) = throw UnsupportedOperationException()
    override fun addLastFirstTwoElementsTo(buffer: Buffer) = throw UnsupportedOperationException()
    override fun addFirstLastTwoElementsTo(buffer: Buffer) = throw UnsupportedOperationException()
}

data class BufferOfOne(val e: Any?) : Buffer() {
    override val color: Int = YELLOW
    override val size: Int = 1

    override val first = e
    override val last = e

    override fun addFirst(element: Any?) = BufferOfTwo(element, e)
    override fun addFirstTwo(element1: Any?, element2: Any?) = BufferOfThree(element1, element2, e)
    override fun addLast(element: Any?) = BufferOfTwo(e, element)
    override fun addLastTwo(element1: Any?, element2: Any?) = BufferOfThree(e, element1, element2)

    override fun removeFirst() = EmptyBuffer
    override fun removeFirstTwo() = throw UnsupportedOperationException()
    override fun removeLast() = EmptyBuffer
    override fun removeLastTwo() = throw UnsupportedOperationException()

    override fun addElementsTo(l: ArrayList<Any?>) { l.add(e) }
    override fun addFirstPairOfLastTwoElementsTo(buffer: Buffer) = throw UnsupportedOperationException()
    override fun addLastPairOfFirstTwoElementsTo(buffer: Buffer) = throw UnsupportedOperationException()
    override fun addLastFirstTwoElementsTo(buffer: Buffer) = throw UnsupportedOperationException()
    override fun addFirstLastTwoElementsTo(buffer: Buffer) = throw UnsupportedOperationException()
}

data class BufferOfTwo(val e1: Any?, val e2: Any?) : Buffer() {
    override val color: Int = GREEN
    override val size: Int = 2

    override val first = e1
    override val last = e2

    override fun addFirst(element: Any?) = BufferOfThree(element, e1, e2)
    override fun addFirstTwo(element1: Any?, element2: Any?) = BufferOfFour(element1, element2, e1, e2)
    override fun addLast(element: Any?) = BufferOfThree(e1, e2, element)
    override fun addLastTwo(element1: Any?, element2: Any?) = BufferOfFour(e1, e2, element1, element2)

    override fun removeFirst() = BufferOfOne(e2)
    override fun removeFirstTwo() = EmptyBuffer
    override fun removeLast() = BufferOfOne(e1)
    override fun removeLastTwo() = EmptyBuffer

    override fun addElementsTo(l: ArrayList<Any?>) { l.add(e1); l.add(e2) }
    override fun addFirstPairOfLastTwoElementsTo(buffer: Buffer) = buffer.addFirst(Pair(e1, e2))
    override fun addLastPairOfFirstTwoElementsTo(buffer: Buffer) = buffer.addLast(Pair(e1, e2))
    override fun addLastFirstTwoElementsTo(buffer: Buffer) = buffer.addLastTwo(e1, e2)
    override fun addFirstLastTwoElementsTo(buffer: Buffer) = buffer.addFirstTwo(e1, e2)
}

data class BufferOfThree(val e1: Any?, val e2: Any?, val e3: Any?) : Buffer() {
    override val color: Int = GREEN
    override val size: Int = 3

    override val first = e1
    override val last = e3

    override fun addFirst(element: Any?) = BufferOfFour(element, e1, e2, e3)
    override fun addFirstTwo(element1: Any?, element2: Any?) = BufferOfFive(element1, element2, e1, e2, e3)
    override fun addLast(element: Any?) = BufferOfFour(e1, e2, e3, element)
    override fun addLastTwo(element1: Any?, element2: Any?) = BufferOfFive(e1, e2, e3, element1, element2)

    override fun removeFirst() = BufferOfTwo(e2, e3)
    override fun removeFirstTwo() = BufferOfOne(e3)
    override fun removeLast() = BufferOfTwo(e1, e2)
    override fun removeLastTwo() = BufferOfOne(e1)

    override fun addElementsTo(l: ArrayList<Any?>) { l.add(e1); l.add(e2); l.add(e3) }
    override fun addFirstPairOfLastTwoElementsTo(buffer: Buffer) = buffer.addFirst(Pair(e2, e3))
    override fun addLastPairOfFirstTwoElementsTo(buffer: Buffer) = buffer.addLast(Pair(e1, e2))
    override fun addLastFirstTwoElementsTo(buffer: Buffer) = buffer.addLastTwo(e1, e2)
    override fun addFirstLastTwoElementsTo(buffer: Buffer) = buffer.addFirstTwo(e2, e3)
}

data class BufferOfFour(val e1: Any?, val e2: Any?, val e3: Any?, val e4: Any?) : Buffer() {
    override val color: Int = YELLOW
    override val size: Int = 4

    override val first = e1
    override val last = e4

    override fun addFirst(element: Any?) = BufferOfFive(element, e1, e2, e3, e4)
    override fun addFirstTwo(element1: Any?, element2: Any?) = throw UnsupportedOperationException()
    override fun addLast(element: Any?) = BufferOfFive(e1, e2, e3, e4, element)
    override fun addLastTwo(element1: Any?, element2: Any?) = throw UnsupportedOperationException()

    override fun removeFirst() = BufferOfThree(e2, e3, e4)
    override fun removeFirstTwo() = BufferOfTwo(e3, e4)
    override fun removeLast() = BufferOfThree(e1, e2, e3)
    override fun removeLastTwo() = BufferOfTwo(e1, e2)

    override fun addElementsTo(l: ArrayList<Any?>) { l.add(e1); l.add(e2); l.add(e3); l.add(e4) }
    override fun addFirstPairOfLastTwoElementsTo(buffer: Buffer) = buffer.addFirst(Pair(e3, e4))
    override fun addLastPairOfFirstTwoElementsTo(buffer: Buffer) = buffer.addLast(Pair(e1, e2))
    override fun addLastFirstTwoElementsTo(buffer: Buffer) = buffer.addLastTwo(e1, e2)
    override fun addFirstLastTwoElementsTo(buffer: Buffer) = buffer.addFirstTwo(e3, e4)
}

data class BufferOfFive(val e1: Any?, val e2: Any?, val e3: Any?, val e4: Any?, val e5: Any?) : Buffer() {
    override val color: Int = RED
    override val size: Int = 5

    override val first = e1
    override val last = e5

    override fun addFirst(element: Any?) = throw UnsupportedOperationException()
    override fun addFirstTwo(element1: Any?, element2: Any?) = throw UnsupportedOperationException()
    override fun addLast(element: Any?) = throw UnsupportedOperationException()
    override fun addLastTwo(element1: Any?, element2: Any?) = throw UnsupportedOperationException()

    override fun removeFirst() = BufferOfFour(e2, e3, e4, e5)
    override fun removeFirstTwo() = BufferOfThree(e3, e4, e5)
    override fun removeLast() = BufferOfFour(e1, e2, e3, e4)
    override fun removeLastTwo() = BufferOfThree(e1, e2, e3)

    override fun addElementsTo(l: ArrayList<Any?>) { l.add(e1); l.add(e2); l.add(e3); l.add(e4); l.add(e5) }
    override fun addFirstPairOfLastTwoElementsTo(buffer: Buffer) = buffer.addFirst(Pair(e4, e5))
    override fun addLastPairOfFirstTwoElementsTo(buffer: Buffer) = buffer.addLast(Pair(e1, e2))
    override fun addLastFirstTwoElementsTo(buffer: Buffer) = buffer.addLastTwo(e1, e2)
    override fun addFirstLastTwoElementsTo(buffer: Buffer) = buffer.addFirstTwo(e4, e5)
}
