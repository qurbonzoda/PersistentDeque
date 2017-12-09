package persistentStack

class PersistentStack<T> private constructor(private val top: Node<T>?) {
    fun isEmpty() = top == null

    fun peek() = top?.value

    fun pop(): PersistentStack<T> {
        if (top == null) {
            throw NoSuchElementException()
        }
        return PersistentStack(top.previous)
    }

    fun push(value: T) = PersistentStack(Node(value, previous = top))

    val size: Int
        get() {
            var size = 0
            var node = this.top

            while (node != null) {
                node = node.previous
                ++size
            }
            return size
        }

    companion object InstanceHolder {
        val emptyStack = PersistentStack<Any>(null)
    }

    private data class Node<T>(val value: T, val previous: Node<T>?)
}

fun <T> emptyStack() = PersistentStack.emptyStack as PersistentStack<T>
fun <T> stackOf(element: T) = emptyStack<T>().push(element)