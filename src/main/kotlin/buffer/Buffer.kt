package buffer

const val RED = 0
const val YELLOW = 1
const val GREEN = 2

const val MAX_BUFFER_SIZE = 7
const val RED_LOW = 0
const val RED_HIGH = MAX_BUFFER_SIZE
const val YELLOW_LOW = 1
const val YELLOW_HIGH = MAX_BUFFER_SIZE - 1
const val GREEN_LOW = 2
const val GREEN_HIGH = MAX_BUFFER_SIZE - 2

const val YELLOW_RED = 3
const val YELLOW_YELLOW = 4
const val YELLOW_GREEN = 5
const val GREEN_YELLOW = 7
const val GREEN_GREEN = 8

internal fun colorChange(oldColor: Int, newColor: Int): Int {
    return oldColor * 3 + newColor
}

class Buffer private constructor(val top: Any?, val size: Int, private val next: Buffer?) {
    val color: Int
        get() = when (this.size) {
            RED_LOW, RED_HIGH       -> RED
            YELLOW_LOW, YELLOW_HIGH -> YELLOW_LOW
            else                    -> GREEN
        }

    val bottom: Any?
        get() {
            if (this.size == 1) return this.top
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
        return Buffer(this.top, this.size + 1, this.next!!.prepend(element))
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
        if (this.size == 1) return empty
        return Buffer(this.top, this.size - 1, this.next!!.removeBottom())
    }
    fun removeBottomTwo(): Buffer {
        if (this.size == 2) return empty
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
        if (this.size == 2) {
            val pair = if (isLhs) Pair(this.top, this.next!!.top) else Pair(this.next!!.top, this.top)
            return buffer.push(pair)
        }
        return this.next!!.pushPairOfBottomTwoElementsTo(buffer, isLhs)
    }
    fun pushTwoPairsOfBottomFourElementsTo(buffer: Buffer, isLhs: Boolean): Buffer {
        if (this.size == 2) {
            val pair = if (isLhs) Pair(this.top, this.next!!.top) else Pair(this.next!!.top, this.top)
            return buffer.push(pair)
        }
        val result = this.next!!.pushTwoPairsOfBottomFourElementsTo(buffer, isLhs)
        if (this.size == 4) {
            val pair = if (isLhs) Pair(this.top, this.next.top) else Pair(this.next.top, this.top)
            return result.push(pair)
        }
        return result
    }
    fun removeBottomFour(): Buffer {
        if (this.size == 4) return empty
        return Buffer(this.top, this.size - 4, this.next!!.removeBottomFour())
    }
    fun prependFourTopTwoElementsToPrevLevel(buffer: Buffer, isLhs: Boolean): Buffer {
        val (e1, e2) = this.top as Pair<*, *>
        val (e3, e4) = this.next!!.top as Pair<*, *>

        var result = empty
        if (isLhs) {
            result = Buffer(e4, 1, result)
            result = Buffer(e3, 2, result)
            result = Buffer(e2, 3, result)
            result = Buffer(e1, 4, result)
        } else {
            result = Buffer(e3, 1, result)
            result = Buffer(e4, 2, result)
            result = Buffer(e1, 3, result)
            result = Buffer(e2, 4, result)
        }
        return buffer.prependSavingOrder(result)
    }
    private fun prependSavingOrder(buffer: Buffer): Buffer {
        if (this === empty) return buffer
        return Buffer(this.top, this.size + buffer.size, this.next!!.prependSavingOrder(buffer))
    }

    companion object InstanceHolder {
        val empty = Buffer(null, 0, null)
    }
}
