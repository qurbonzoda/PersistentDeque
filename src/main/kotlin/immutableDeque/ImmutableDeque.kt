package immutableDeque

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

    fun listIterator(index: Int): ListIterator<T> {
        return this.toList().listIterator(index)
    }

    fun listIterator(): ListIterator<T> {
        return this.listIterator(0)
    }

    operator fun iterator(): Iterator<T> {
        return listIterator()
    }
}