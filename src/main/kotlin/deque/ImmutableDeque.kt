package deque

interface ImmutableDeque<T> {
    val size: Int
    val first: T?
    val last: T?
    fun isEmpty(): Boolean
    fun addFirst(value: T): ImmutableDeque<T>
    fun removeFirst(): ImmutableDeque<T>
    fun addLast(value: T): ImmutableDeque<T>
    fun removeLast(): ImmutableDeque<T>
    fun toList(): List<T>
    fun get(index: Int): T
    fun set(index: Int, value: T): ImmutableDeque<T>
}

fun <T> emptyDeque() = EmptyDeque as ImmutableDeque<T>