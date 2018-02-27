package buffer

internal interface ImmutableBuffer {
    val top: Any?
    val size: Int
    val color: Int
    fun push(value: Any?): ImmutableBuffer
    fun pop(count: Int = 1): ImmutableBuffer
    fun removeBottom(count: Int = 1): ImmutableBuffer

    // leaky abstractions
    fun addLeafValuesTo(list: MutableList<Any?>, depth: Int)
    fun getLeafValueAt(index: Int, depth: Int): Any?
    fun setLeafValueAt(index: Int, value: Any?, depth: Int): ImmutableBuffer

    fun pushAllToNextLevelBuffer(nextLevelBuffer: ImmutableBuffer): ImmutableBuffer

    fun moveToUpperLevelBuffer(count: Int): ImmutableBuffer
    fun prependSavingOrder(buffer: ImmutableBuffer): ImmutableBuffer

    fun removeBottomAndMoveRestToOppositeSideBuffer(): ImmutableBuffer
    fun moveToOppositeSideBuffer(): ImmutableBuffer
}