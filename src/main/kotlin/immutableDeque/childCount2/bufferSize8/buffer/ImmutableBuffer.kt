package immutableDeque.childCount2.bufferSize8.buffer

import immutableDeque.GREEN
import immutableDeque.ImmutableDeque
import immutableDeque.RED
import immutableDeque.YELLOW
import immutableDeque.childCount2.CHILD_COUNT
import immutableDeque.childCount2.bufferSize8.constants.RED_HIGH
import immutableDeque.childCount2.bufferSize8.constants.RED_LOW
import immutableDeque.childCount2.bufferSize8.constants.YELLOW_HIGH
import immutableDeque.childCount2.bufferSize8.constants.YELLOW_LOW
import immutableDeque.childCount2.childCountToThePow

internal abstract class ImmutableBuffer(val top: Any?,
                                        override val size: Int,
                                        open val next: ImmutableBuffer?) : ImmutableDeque<Any?> {
    val color: Int
        get() = when (this.size) {
            RED_LOW, RED_HIGH -> RED
            YELLOW_LOW, YELLOW_HIGH -> YELLOW
            else -> GREEN
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
        val children = node as Array<*>
        for (child in children) {
            this.addLeavesOfNode(child, list, height - 1)
        }
    }

    fun getLeafOfNodeAt(index: Int, node: Any?, depth: Int): Any? {
        if (depth == 0) {
            return node
        }
        val children = node as Array<*>
        val childSize = childCountToThePow(depth - 1)
        var indexInChild = index
        for (child in children) {
            if (indexInChild < childSize) {
                return this.getLeafOfNodeAt(indexInChild, child, depth - 1)
            }
            indexInChild -= childSize
        }
        throw AssertionError()
    }

    fun setLeafOfNodeAt(index: Int, value: Any?, node: Any?, depth: Int): Any? {
        if (depth == 0) {
            return value
        }

        val children = node as Array<Any?>
        val childSize = childCountToThePow(depth - 1)
        for (i in 0 until CHILD_COUNT) {
            val indexInChild = index - i * childSize
            if (indexInChild < childSize) {
                val newChildren = children.copyOf()
                newChildren[i] = this.setLeafOfNodeAt(indexInChild, value, children[i], depth - 1)
                return newChildren
            }
        }
        throw AssertionError()
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
