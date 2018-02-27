package buffer

internal abstract class AbstractBuffer(override val top: Any?,
                                       override val size: Int,
                                       open val next: ImmutableBuffer): ImmutableBuffer {
    override val color: Int
        get() = when (this.size) {
            RED_LOW, RED_HIGH       -> RED
            YELLOW_LOW, YELLOW_HIGH -> YELLOW_LOW
            else                    -> GREEN
        }

    override fun pop(count: Int): ImmutableBuffer {
        assert(count <= this.size)

        if (count == 0) {   // pop(count = 1) is very frequent invocation -> optimize
            return this
        }
        return this.next.pop(count - 1)
    }

    override fun removeBottom(count: Int): ImmutableBuffer {
        assert(count > 0 && count <= this.size)

        if (this.size == count) {
            return this.empty()
        }
        val next = this.next.removeBottom(count)
        return next.push(this.top)
    }


    override fun prependSavingOrder(buffer: ImmutableBuffer): ImmutableBuffer {
        val result = this.next.prependSavingOrder(buffer)
        return result.push(this.top)
    }

    override fun removeBottomAndMoveRestToOppositeSideBuffer(): ImmutableBuffer {
        var result = oppositeSideEmpty()

        var buffer = this
        while (buffer.size > 1) {
            result = result.push(buffer.top)
            buffer = buffer.next as AbstractBuffer
        }
        return result
    }

    override fun moveToOppositeSideBuffer(): ImmutableBuffer {
        var result = this.oppositeSideEmpty()

        var buffer = this
        while (buffer.size > 1) {
            result = result.push(buffer.top)
            buffer = buffer.next as AbstractBuffer
        }
        return result.push(buffer.top)
    }

    // MARK: protected
    fun getLeafOfNodeAt(index: Int, node: Any?, depth: Int): Any? {
        if (depth == 0) {
            return node
        }
        val pair = node as Pair<*, *>
        val lSize = 1 shl (depth - 1)
        if (index < lSize) {
            return getLeafOfNodeAt(index, pair.first, depth - 1)
        }
        return getLeafOfNodeAt(index - lSize, pair.second, depth - 1)
    }

    fun setLeafOfNodeAt(index: Int, value: Any?, node: Any?, depth: Int): Any? {
        if (depth == 0) {
            return value
        }
        val pair = node as Pair<*, *>
        val lSize = 1 shl (depth - 1)
        if (index < lSize) {
            val newFirst = setLeafOfNodeAt(index, value, pair.first, depth - 1)
            return Pair(newFirst, pair.second)
        }
        val newSecond = setLeafOfNodeAt(index - lSize, value, pair.second, depth - 1)
        return Pair(pair.first, newSecond)
    }

    abstract fun empty(): ImmutableBuffer
    abstract fun oppositeSideEmpty(): ImmutableBuffer
}