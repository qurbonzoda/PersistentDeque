package buffer

const val RED = 0
const val YELLOW = 1
const val GREEN = 2

const val MAX_BUFFER_SIZE = 5
const val RED_LOW = 0
const val RED_HIGH = MAX_BUFFER_SIZE
const val YELLOW_LOW = 1
const val YELLOW_HIGH = MAX_BUFFER_SIZE - 1

class Buffer private constructor(val top: Any?, val size: Int, private val next: Buffer?) {
    val color: Int
        get() = when (this.size) {
            RED_LOW, RED_HIGH       -> RED
            YELLOW_LOW, YELLOW_HIGH -> YELLOW_LOW
            else                    -> GREEN
        }

    val bottom: Any?
        get() {
            if (this.next === empty) return this.top
            return this.next!!.bottom
        }
    fun push(element: Any?): Buffer {
        return Buffer(element, this.size + 1, this)
    }
    fun pushTwo(element1: Any?, element2: Any?): Buffer {
        val next = Buffer(element1, this.size + 1, this)
        return Buffer(element2, next.size + 1, next)
    }
    fun prepend(element: Any?): Buffer {
        if (this === empty) return Buffer(element, this.size + 1, this)
        return this.next!!.prepend(element)
    }
    fun prependTwo(element1: Any?, element2: Any?): Buffer {
        if (this === empty) {
            val next = Buffer(element1, this.size + 1, this)
            return Buffer(element2, next.size + 1, next)
        }
        return Buffer(this.top, this.size + 2, this.next!!.prependTwo(element1, element2))
    }
    fun pop(): Buffer {
        return this.next!!
    }
    fun popTwo(): Buffer {
        return this.next!!.next!!
    }
    fun removeBottom(): Buffer {
        if (this.next === empty) return empty
        return Buffer(this.top, this.size - 1, this.next!!.removeBottom())
    }
    fun removeBottomTwo(): Buffer {
        if (this.next!!.next === empty) return empty
        return Buffer(this.top, this.size - 2, this.next!!.removeBottomTwo())
    }
    fun addElementsTo(list: ArrayList<Any?>, reverseOrder: Boolean) {
        if (this === empty) return
        if (reverseOrder) {
            this.next!!.addElementsTo(list, reverseOrder)
            list.add(this.top)
        } else {
            list.add(this.top)
            this.next!!.addElementsTo(list, reverseOrder)
        }
    }
    fun pushPairOfBottomTwoElementsTo(buffer: Buffer, isLhs: Boolean): Buffer {
        if (this.next!!.next === empty) {
            val pair = if (isLhs) Pair(this.top, this.next!!.top) else Pair(this.next!!.top, this.top)
            return buffer.push(pair)
        }
        return this.next!!.pushPairOfBottomTwoElementsTo(buffer, isLhs)
    }

    companion object InstanceHolder {
        val empty = Buffer(null, 0, null)
    }
}
