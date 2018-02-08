package buffer

const val RED = 0
const val YELLOW = 1
const val GREEN = 2

const val MAX_BUFFER_SIZE = 5
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

class Buffer private constructor(val size: Int,
                                 private val next: Buffer?,
                                 private val lhs: Any?,
                                 private val rhs: Any?) {
    val top: Any? = this.lhs
    val color: Int
        get() = when (this.size) {
            RED_LOW, RED_HIGH       -> RED
            YELLOW_LOW, YELLOW_HIGH -> YELLOW_LOW
            else                    -> GREEN
        }

    val bottom: Any? = this.pop(this.size - 1).top

    fun push(element: Any?): Buffer {
        return Buffer(this.size + 1, this, element, null)
    }

    private fun pushBuffer(buffer: Buffer): Buffer {
        return Buffer(this.size + 1, this, buffer.lhs, buffer.rhs)
    }

    private fun pushPrevLevelBuffers(lhs: Any?, rhs: Any?): Buffer {
        return Buffer(this.size + 1, this, lhs, rhs)
    }

    fun pop(count: Int = 1): Buffer {
        var buffer = this
        repeat(count) { buffer = buffer.next!! }
        return buffer
    }

    fun removeBottom(count: Int = 1): Buffer {
        if (this.size == count) return empty
        return Buffer(this.size - count, this.next!!.removeBottom(count), this.lhs, rhs)
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

        var result = if ((this.size and 1) == 1) this.next!! else this
        result = result.pop(result.size - (maxPushCount shl 1))
        return result._pushPairsOfBottomElementsTo(buffer, isLhs)
    }

    private fun _pushPairsOfBottomElementsTo(buffer: Buffer, isLhs: Boolean): Buffer {
        if (this === empty) return buffer

        val result = this.next!!.next!!._pushPairsOfBottomElementsTo(buffer, isLhs)

        return if (isLhs) {
            result.pushPrevLevelBuffers(this, this.next)
        } else {
            result.pushPrevLevelBuffers(this.next, this)
        }
    }

    fun topElementsToPrevLevel(maxPopCount: Int, isLhs: Boolean): Buffer {
        if (maxPopCount == 0) return empty

        val result = this.next!!.topElementsToPrevLevel(maxPopCount - 1, isLhs)

        return if (isLhs) {
            result.pushBuffer(this.rhs as Buffer).pushBuffer(this.lhs as Buffer)
        } else {
            result.pushBuffer(this.lhs as Buffer).pushBuffer(this.rhs as Buffer)
        }
    }

    fun bottomElementsToPrevLevel(maxRemoveBottomCount: Int, isLhs: Boolean): Buffer {
        if (this === empty) return empty

        var result = empty
        var buffer = this.pop(this.size - maxRemoveBottomCount)

        repeat(maxRemoveBottomCount) {
            result = if (isLhs) {
                result.pushBuffer(buffer.rhs as Buffer).pushBuffer(buffer.lhs as Buffer)
            } else {
                result.pushBuffer(buffer.lhs as Buffer).pushBuffer(buffer.rhs as Buffer)
            }
            buffer = buffer.pop()
        }
        return result
    }

    fun prependSavingOrder(buffer: Buffer): Buffer {
        if (this === empty) return buffer
        return Buffer(this.size + buffer.size, this.next!!.prependSavingOrder(buffer), this.lhs, this.rhs)
    }

    fun getLeafValueAt(index: Int, depth: Int): Any? {
        if (depth == 0) { return this.top }

        val lSize = 1 shl (depth - 1)

        if (index < lSize) {
            return (this.lhs as Buffer).getLeafValueAt(index, depth - 1)
        }
        return (this.rhs as Buffer).getLeafValueAt(index - lSize, depth - 1)
    }

    fun setAt(index: Int, value: Any?, depth: Int, isLhs: Boolean): Buffer {
        val leafCount = 1 shl depth
        return when {
            index >= leafCount  -> this.next!!.setAt(index - leafCount, value, depth, isLhs).pushBuffer(this)
            isLhs               -> this.setLeafValueAt(index, value, depth)
            else                -> this.setLeafValueAt(leafCount - index - 1, value, depth)
        }
    }

    private fun setLeafValueAt(index: Int, value: Any?, depth: Int): Buffer {
        if (depth == 0) { return Buffer(this.size, this.next, value, null) }

        val lSize = 1 shl (depth - 1)

        if (index < lSize) {
            val newLhs = (this.lhs as Buffer).setLeafValueAt(index, value, depth - 1)
            return Buffer(this.size, this.next, newLhs, this.rhs)
        }
        val newRhs = (this.rhs as Buffer).setLeafValueAt(index - lSize, value, depth - 1)
        return Buffer(this.size, this.next, this.lhs, newRhs)
    }

    fun fillList(list: MutableList<Any?>, depth: Int, isLhs: Boolean) {
        if (this === empty) return
        if (isLhs) {
            this.fillListWithLeaveValues(list, depth)
            this.next!!.fillList(list, depth, isLhs)
        } else {
            this.next!!.fillList(list, depth, isLhs)
            this.fillListWithLeaveValues(list, depth)
        }
    }

    private fun fillListWithLeaveValues(list: MutableList<Any?>, depth: Int) {
        if (depth == 0) {
            list.add(this.lhs)
        } else {
            (this.lhs as Buffer).fillListWithLeaveValues(list, depth - 1)
            (this.rhs as Buffer).fillListWithLeaveValues(list, depth - 1)
        }
    }

    companion object InstanceHolder {
        val empty = Buffer(0, null, null,null)
    }
}
