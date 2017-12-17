package buffer

const val RED = 0
const val YELLOW = 1
const val GREEN = 2


sealed class Buffer {
    abstract val color: Int
    abstract val size: Int
    abstract val first: Any
    abstract val last: Any
    abstract fun addFirst(element: Any): Buffer
    abstract fun addLast(element: Any): Buffer
    abstract fun removeFirst(): Buffer
    abstract fun removeLast(): Buffer
}

object EmptyBuffer : Buffer() {
    override val color: Int = RED
    override val size: Int = 0

    override val first: Any
        get() = throw UnsupportedOperationException()
    override val last: Any
        get() = throw UnsupportedOperationException()

    override fun removeFirst() = throw UnsupportedOperationException()
    override fun removeLast() = throw UnsupportedOperationException()

    override fun addFirst(element: Any) = BufferOfOne(element)
    override fun addLast(element: Any) = BufferOfOne(element)
}

data class BufferOfOne(val e: Any) : Buffer() {
    override val color: Int = YELLOW
    override val size: Int = 1

    override val first = e
    override val last = e

    override fun addFirst(element: Any) = BufferOfTwo(element, e)
    override fun addLast(element: Any) = BufferOfTwo(e, element)

    override fun removeFirst() = EmptyBuffer
    override fun removeLast() = EmptyBuffer
}

data class BufferOfTwo(val e1: Any, val e2: Any) : Buffer() {
    override val color: Int = GREEN
    override val size: Int = 2

    override val first = e1
    override val last = e2

    override fun addFirst(element: Any) = BufferOfThree(element, e1, e2)
    override fun addLast(element: Any) = BufferOfThree(e1, e2, element)

    override fun removeFirst() = BufferOfOne(e2)
    override fun removeLast() = BufferOfOne(e1)
}

data class BufferOfThree(val e1: Any, val e2: Any, val e3: Any) : Buffer() {
    override val color: Int = GREEN
    override val size: Int = 3

    override val first = e1
    override val last = e3

    override fun addFirst(element: Any) = BufferOfFour(element, e1, e2, e3)
    override fun addLast(element: Any) = BufferOfFour(e1, e2, e3, element)

    override fun removeFirst() = BufferOfTwo(e2, e3)
    override fun removeLast() = BufferOfTwo(e1, e2)
}

data class BufferOfFour(val e1: Any, val e2: Any, val e3: Any, val e4: Any) : Buffer() {
    override val color: Int = YELLOW
    override val size: Int = 4

    override val first = e1
    override val last = e4

    override fun addFirst(element: Any) = BufferOfFive(element, e1, e2, e3, e4)
    override fun addLast(element: Any) = BufferOfFive(e1, e2, e3, e4, element)

    override fun removeFirst() = BufferOfThree(e2, e3, e4)
    override fun removeLast() = BufferOfThree(e1, e2, e3)
}

data class BufferOfFive(val e1: Any, val e2: Any, val e3: Any, val e4: Any, val e5: Any) : Buffer() {
    override val color: Int = RED
    override val size: Int = 5

    override val first = e1
    override val last = e5

    override fun addFirst(element: Any) = throw UnsupportedOperationException()
    override fun addLast(element: Any) = throw UnsupportedOperationException()

    override fun removeFirst() = BufferOfFour(e2, e3, e4, e5)
    override fun removeLast() = BufferOfFour(e1, e2, e3, e4)
}
