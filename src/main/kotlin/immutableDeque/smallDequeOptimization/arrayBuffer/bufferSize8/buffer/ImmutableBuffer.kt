package immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize8.buffer

import immutableDeque.ImmutableDeque
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize8.constants.*

internal abstract class ImmutableBuffer(val array: Array<Any?>,
                                        override val size: Int): ImmutableDeque<Any?> {
    val top: Any?
        get() {
            return array[this.size - 1]
        }

    val color: Int
        get() = when (this.size) {
            RED_LOW, RED_HIGH       -> RED
            YELLOW_LOW, YELLOW_HIGH -> YELLOW_LOW
            else                    -> GREEN
        }

    fun push(value: Any?): ImmutableBuffer {
        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        System.arraycopy(this.array, 0, newArray, 0, this.size)
        newArray[this.size] = value
        return this.withArray(newArray, this.size + 1)
    }

    fun pop(count: Int = 1): ImmutableBuffer {
//        assert(count > 0 &&  count <= this.size)

        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        System.arraycopy(this.array, 0, newArray, 0, this.size - count)
        return this.withArray(newArray, this.size - count)
    }

    fun removeBottom(count: Int): ImmutableBuffer {
//        assert(count > 0 && count <= this.size)

        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        System.arraycopy(this.array, count, newArray, 0, this.size - count)
        return this.withArray(newArray, this.size - count)
    }


    open fun prependSavingOrder(buffer: ImmutableBuffer): ImmutableBuffer {
        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        System.arraycopy(buffer.array, 0, newArray, 0, buffer.size)
        System.arraycopy(this.array, 0, newArray, buffer.size, this.size)
        return this.withArray(newArray, buffer.size + this.size)
    }

    fun removeBottomAndMoveRestToOppositeSideBuffer(): ImmutableBuffer {
        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        var position = 0

        for (i in this.size - 1 downTo 1) {
            newArray[position++] = this.array[i]
        }
        return this.oppositeSideBufferWithArray(newArray, this.size - 1)
    }

    fun moveToOppositeSideBuffer(): ImmutableBuffer {
        val newArray = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        var position = 0

        for (i in this.size - 1 downTo 0) {
            newArray[position++] = this.array[i]
        }
        return this.oppositeSideBufferWithArray(newArray, this.size)
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

    abstract fun withArray(array: Array<Any?>, size: Int): ImmutableBuffer
    abstract fun oppositeSideBufferWithArray(array: Array<Any?>, size: Int): ImmutableBuffer

    abstract fun addLeafValuesTo(list: MutableList<Any?>, depth: Int)
    abstract fun getLeafValueAt(index: Int, depth: Int): Any?
    abstract fun setLeafValueAt(index: Int, value: Any?, depth: Int): ImmutableBuffer

    abstract fun popAndPushAllToNextLevelBuffer(count: Int, nextLevelBuffer: ImmutableBuffer): ImmutableBuffer
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
