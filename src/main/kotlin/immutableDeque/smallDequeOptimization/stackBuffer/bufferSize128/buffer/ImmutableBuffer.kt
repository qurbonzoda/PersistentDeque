package immutableDeque.smallDequeOptimization.stackBuffer.bufferSize128.buffer

import immutableDeque.ImmutableDeque
import immutableDeque.smallDequeOptimization.stackBuffer.bufferSize128.constants.*

internal abstract class ImmutableBuffer(val top: Any?,
                                        override val size: Int,
                                        open val next: ImmutableBuffer?): ImmutableDeque<Any?> {
    val color: Int
        get() = when (this.size) {
            RED_LOW, RED_HIGH       -> RED
            YELLOW_LOW, YELLOW_HIGH -> YELLOW_LOW
            else                    -> GREEN
        }

    fun pop(count: Int = 1): ImmutableBuffer {
//        assert(count > 0 &&  count <= this.size)

        if (count == 1) {
            return this.next!!
        }
        return this.next!!.pop(count - 1)
    }

    fun removeBottom(count: Int): ImmutableBuffer {
//        assert(count > 0 && count <= this.size)

        if (this.size == count) {
            return this.empty()
        }
        val next = this.next!!.removeBottom(count)
        return next.push(this.top)
    }


    open fun prependSavingOrder(buffer: ImmutableBuffer): ImmutableBuffer {
        val result = this.next!!.prependSavingOrder(buffer)
        return result.push(this.top)
    }

    fun removeBottomAndMoveRestToOppositeSideBuffer(): ImmutableBuffer {
        var result = this.oppositeSideEmpty()

        var buffer = this
        while (buffer.size > 1) {
            result = result.push(buffer.top)
            buffer = buffer.next!!
        }
        return result
    }

    fun moveToOppositeSideBuffer(): ImmutableBuffer {
        var result = this.oppositeSideEmpty()

        var buffer = this
        while (buffer.size > 1) {
            result = result.push(buffer.top)
            buffer = buffer.next!!
        }
        return result.push(buffer.top)
    }

    // MARK: protected
    fun addLeavesOfNode(node: Any?, list: MutableList<Any?>, height: Int) {
        if (height == 0) {
            list.add(node)
            return
        }
        val pair = node as Pair<*, *>
        this.addLeavesOfNode(pair.first, list, height - 1)
        this.addLeavesOfNode(pair.second, list, height - 1)
    }

    fun getLeafOfNodeAt(index: Int, node: Any?, depth: Int): Any? {
        if (depth == 0) {
            return node
        }
        val pair = node as Pair<*, *>
        val lSize = 1 shl (depth - 1)
        if (index < lSize) {
            return this.getLeafOfNodeAt(index, pair.first, depth - 1)
        }
        return this.getLeafOfNodeAt(index - lSize, pair.second, depth - 1)
    }

    fun setLeafOfNodeAt(index: Int, value: Any?, node: Any?, depth: Int): Any? {
        if (depth == 0) {
            return value
        }
        val pair = node as Pair<*, *>
        val lSize = 1 shl (depth - 1)
        if (index < lSize) {
            val newFirst = this.setLeafOfNodeAt(index, value, pair.first, depth - 1)
            return Pair(newFirst, pair.second)
        }
        val newSecond = this.setLeafOfNodeAt(index - lSize, value, pair.second, depth - 1)
        return Pair(pair.first, newSecond)
    }

    abstract fun push(value: Any?): ImmutableBuffer
    abstract fun empty(): ImmutableBuffer
    abstract fun oppositeSideEmpty(): ImmutableBuffer

    abstract fun addLeafValuesTo(list: MutableList<Any?>, depth: Int)
    abstract fun getLeafValueAt(index: Int, depth: Int): Any?
    abstract fun setLeafValueAt(index: Int, value: Any?, depth: Int): ImmutableBuffer

    abstract fun pushAllToNextLevelBuffer(nextLevelBuffer: ImmutableBuffer): ImmutableBuffer
    abstract fun moveToUpperLevelBuffer(count: Int): ImmutableBuffer

    // ImmutableDeque
    override fun isEmpty(): Boolean {
        return false
    }

    override fun toList(): List<Any?> {
        val list = mutableListOf<Any?>()
        this.addLeafValuesTo(list, 0)
        return list.toList()
    }

    override fun get(index: Int): Any? {
//        assert(index < this.size)
        return this.getLeafValueAt(index, 0)
    }

    override fun set(index: Int, value: Any?): ImmutableDeque<Any?> {
//        assert(index < this.size)
        return this.setLeafValueAt(index, value, 0)
    }
}
