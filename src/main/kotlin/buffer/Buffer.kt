package buffer

const val RED = 0
const val YELLOW = 1
const val GREEN = 2


class Buffer private constructor(val size: Int,
                                 private val e1: Any?,
                                 private val e2: Any?,
                                 private val e3: Any?,
                                 private val e4: Any?,
                                 private val e5: Any?) {
    val isEmpty: Boolean = size == 0

    val color: Int = when (size) {
        0, 5 -> RED
        1, 4 -> YELLOW
        else -> GREEN
    }

    val first: Any? = e1

    val last: Any?
        get() {
            return when (size) {
                1 -> e1
                2 -> e2
                3 -> e3
                4 -> e4
                else -> e5
            }
        }

    fun addFirst(element: Any?): Buffer {
        return Buffer(size + 1, element, e1, e2, e3, e4)
    }

    fun addFirstTwo(element1: Any?, element2: Any?): Buffer {
        return Buffer(size + 2, element1, element2, e1, e2, e3)
    }

    fun addLast(element: Any?): Buffer {
        return when (size) {
            0 -> Buffer(size + 1, element, e1, e2, e3, e4)
            1 -> Buffer(size + 1, e1, element, e2, e3, e4)
            2 -> Buffer(size + 1, e1, e2, element, e3, e4)
            3 -> Buffer(size + 1, e1, e2, e3, element, e4)
            else -> Buffer(size + 1, e1, e2, e3, e4, element)
        }
    }

    fun addLastTwo(element1: Any?, element2: Any?): Buffer {
        return when (size) {
            0 -> Buffer(size + 2, element1, element2, e1, e2, e3)
            1 -> Buffer(size + 2, e1, element1, element2, e2, e3)
            2 -> Buffer(size + 2, e1, e2, element1, element2, e3)
            else -> Buffer(size + 2, e1, e2, e3, element1, element2)
        }
    }

    fun removeFirst(): Buffer {
        return Buffer(size - 1, e2, e3, e4, e5, null)
    }

    fun removeFirstTwo(): Buffer {
        return Buffer(size - 2, e3, e4, e5, null, null)
    }

    fun removeLast(): Buffer {
        return when (size) {
            1 -> Buffer(size - 1, null, e2, e3, e4, e5)
            2 -> Buffer(size - 1, e1, null, e3, e4, e5)
            3 -> Buffer(size - 1, e1, e2, null, e4, e5)
            4 -> Buffer(size - 1, e1, e2, e3, null, e5)
            else -> Buffer(size - 1, e1, e2, e3, e4, null)
        }
    }

    fun removeLastTwo(): Buffer {
        return when (size) {
            2 -> Buffer(size - 2, null, null, e3, e4, e5)
            3 -> Buffer(size - 2, e1, null, null, e4, e5)
            4 -> Buffer(size - 2, e1, e2, null, null, e5)
            else -> Buffer(size - 2, e1, e2, e3, null, null)
        }
    }

    fun addElementsTo(l: ArrayList<Any?>) {
        if (size >= 1) {
            l.add(e1)
        }
        if (size >= 2) {
            l.add(e2)
        }
        if (size >= 3) {
            l.add(e3)
        }
        if (size >= 4) {
            l.add(e4)
        }
        if (size >= 5) {
            l.add(e5)
        }
    }

    fun addFirstPairOfLastTwoElementsTo(buffer: Buffer): Buffer {
        return when (size) {
            2 -> buffer.addFirst(Pair(e1, e2))
            3 -> buffer.addFirst(Pair(e2, e3))
            4 -> buffer.addFirst(Pair(e3, e4))
            else -> buffer.addFirst(Pair(e4, e5))
        }
    }

    fun addLastPairOfFirstTwoElementsTo(buffer: Buffer): Buffer {
        return buffer.addLast(Pair(e1, e2))
    }

    companion object InstanceHolder {
        val empty = Buffer(0, null, null, null, null, null)
    }
}
