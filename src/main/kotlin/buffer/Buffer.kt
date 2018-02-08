package buffer

const val RED = 0
const val YELLOW = 1
const val GREEN = 2

const val MAX_BUFFER_SIZE = 15
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

    fun pop(count: Int = 1): Buffer {
        if (count == 1) return this.next!!
        return this.next!!.pop(count - 1)
    }

    fun removeBottom(count: Int = 1): Buffer {
        if (this.size == count) return empty
        return Buffer(this.top, this.size - count, this.next!!.removeBottom(count))
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

    fun pushPairsOfBottomElementsTo(buffer: Buffer, maxPushCount: Int, isLhs: Boolean): Buffer {
        if (this === empty) return buffer

        val result = this.next!!.pushPairsOfBottomElementsTo(buffer, maxPushCount, isLhs)
        if (this.size % 2 == 0 && this.size <= maxPushCount * 2) {
            val pair = if (isLhs) Pair(this.top, this.next.top) else Pair(this.next.top, this.top)
            return result.push(pair)
        }
        return result
    }

    fun topElementsToPrevLevel(maxPopCount: Int, isLhs: Boolean): Buffer {
        if (maxPopCount == 0) return empty

        val result = this.next!!.topElementsToPrevLevel(maxPopCount - 1, isLhs)
        val (e1, e2) = this.top as Pair<*, *>

        return if (isLhs) {
            result.push(e2).push(e1)
        } else {
            result.push(e1).push(e2)
        }
    }
    fun bottomElementsToPrevLevel(buffer: Buffer, maxRemoveBottomCount: Int, isLhs: Boolean): Buffer {
        if (this === empty) return buffer

        if (this.size <= maxRemoveBottomCount) {
            val (e1, e2) = this.top as Pair<*, *>

            val result = if (isLhs) {
                buffer.push(e2).push(e1)
            } else {
                buffer.push(e1).push(e2)
            }
            return this.next!!.bottomElementsToPrevLevel(result, maxRemoveBottomCount, isLhs)
        }
        return this.next!!.bottomElementsToPrevLevel(buffer, maxRemoveBottomCount, isLhs)
    }

    fun prependSavingOrder(buffer: Buffer): Buffer {
        if (this === empty) return buffer
        return Buffer(this.top, this.size + buffer.size, this.next!!.prependSavingOrder(buffer))
    }

    companion object InstanceHolder {
        val empty = Buffer(null, 0, null)
    }
}
