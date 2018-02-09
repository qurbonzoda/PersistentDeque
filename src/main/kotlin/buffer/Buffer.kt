package buffer

const val RED = 0
const val YELLOW = 1
const val GREEN = 2


class Buffer private constructor(val size: Int, private val es: Array<Any?>) {
    val isEmpty: Boolean = size == 0

    val color: Int = when (size) {
        0, 5 -> RED
        1, 4 -> YELLOW
        else -> GREEN
    }

    val first: Any? = es[0]

    val last: Any? = if (isEmpty) null else es[size - 1]

    fun addFirst(element: Any?): Buffer {
        return Buffer(size + 1, arrayOf(element, es[0], es[1], es[2], es[3]))
    }

    fun addFirstTwo(element1: Any?, element2: Any?): Buffer {
        return Buffer(size + 2, arrayOf(element1, element2, es[0], es[1], es[2]))
    }

    fun addLast(element: Any?): Buffer {
        return when (size) {
            0 -> Buffer(size + 1, arrayOf(element, null, null, null, null))
            1 -> Buffer(size + 1, arrayOf(es[0], element, null, null, null))
            2 -> Buffer(size + 1, arrayOf(es[0], es[1], element, null, null))
            3 -> Buffer(size + 1, arrayOf(es[0], es[1], es[2], element, null))
            else -> Buffer(size + 1, arrayOf(es[0], es[1], es[2], es[3], element))
        }
    }

    fun addLastTwo(element1: Any?, element2: Any?): Buffer {
        return when (size) {
            0 -> Buffer(size + 2, arrayOf(element1, element2, null, null, null))
            1 -> Buffer(size + 2, arrayOf(es[0], element1, element2, null, null))
            2 -> Buffer(size + 2, arrayOf(es[0], es[1], element1, element2, null))
            else -> Buffer(size + 2, arrayOf(es[0], es[1], es[2], element1, element2))
        }
    }

    fun removeFirst(): Buffer {
        return Buffer(size - 1, arrayOf(es[1], es[2], es[3], es[4], null))
    }

    fun removeFirstTwo(): Buffer {
        return Buffer(size - 2, arrayOf(es[2], es[3], es[4], null, null))
    }

    fun removeLast(): Buffer {
        return when (size) {
            1 -> empty
            2 -> Buffer(size - 1, arrayOf(es[0], null, null, null, null))
            3 -> Buffer(size - 1, arrayOf(es[0], es[1], null, null, null))
            4 -> Buffer(size - 1, arrayOf(es[0], es[1], es[2], null, null))
            else -> Buffer(size - 1, arrayOf(es[0], es[1], es[2], es[3], null))
        }
    }

    fun removeLastTwo(): Buffer {
        return when (size) {
            2 -> empty
            3 -> Buffer(size - 2, arrayOf(es[0], null, null, null, null))
            4 -> Buffer(size - 2, arrayOf(es[0], es[1], null, null, null))
            else -> Buffer(size - 2, arrayOf(es[0], es[1], es[2], null, null))
        }
    }

    fun addElementsTo(l: ArrayList<Any?>) {
        for (index in 0 until size) {
            l.add(es[index])
        }
    }

    fun addFirstPairOfLastTwoElementsTo(buffer: Buffer): Buffer {
        return when (size) {
            2 -> buffer.addFirst(Pair(es[0], es[1]))
            3 -> buffer.addFirst(Pair(es[1], es[2]))
            4 -> buffer.addFirst(Pair(es[2], es[3]))
            else -> buffer.addFirst(Pair(es[3], es[4]))
        }
    }

    fun addLastPairOfFirstTwoElementsTo(buffer: Buffer): Buffer {
        return buffer.addLast(Pair(es[0], es[1]))
    }

    companion object InstanceHolder {
        val empty = Buffer(0, arrayOf(null, null, null, null, null))
    }
}
