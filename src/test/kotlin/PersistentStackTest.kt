import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import persistentStack.PersistentStack
import persistentStack.emptyStack
import java.util.*
import kotlin.NoSuchElementException

class PersistentStackTests {
    @Test
    fun newStackShouldBeEmpty() {
        assertTrue(emptyStack<Int>().isEmpty())
    }

    @Test
    fun peekForNewStackShouldBeNull() {
        assertNull(emptyStack<Int>().peek())
    }

    @Test
    fun sizeOfNewStackShouldBeZero() {
        assertEquals(0, emptyStack<Int>().size)
    }

    @Test
    fun popForNewStackShouldThrowEmptyStackException() {
        assertThrows(NoSuchElementException::class.java) {
            emptyStack<Int>().pop()
        }
    }

    @Test
    fun pushShouldNotMutateStackButShouldReturnNewStack() {
        val stack = emptyStack<Int>()
        val newStack = stack.push(0)

        assertTrue(stack.isEmpty())

        assertFalse(newStack.isEmpty())
        assertEquals(0, newStack.peek())
    }

    @Test
    fun popShouldNotMutateStackButShouldReturnNewStack() {
        val stack = emptyStack<Int>().push(0)
        val value = stack.peek()!!
        val newStack = stack.pop()

        assertFalse(stack.isEmpty())
        assertTrue(newStack.isEmpty())

        assertEquals(0, stack.peek()!!)
        assertEquals(0, value)
    }

    @Test
    fun isEmptyTests() {
        val pushCount = 5

        var stack = emptyStack<Int>()
        assertTrue(stack.isEmpty())

        repeat(times = pushCount) {
            stack = stack.push(0)
            assertFalse(stack.isEmpty())
        }

        repeat(times = pushCount - 1) {
            stack = stack.pop()
            assertFalse(stack.isEmpty())
        }

        stack = stack.pop()
        assertTrue(stack.isEmpty())
    }

    @Test
    fun sizeTests() {
        val pushCount = 5

        var stack = emptyStack<Int>()
        assertEquals(0, stack.size)

        repeat(times = pushCount) { index ->
            stack = stack.push(0)
            assertEquals(index + 1, stack.size)
        }

        repeat(times = pushCount - 1) { index ->
            stack = stack.pop()
            assertEquals(pushCount - index - 1, stack.size)
        }

        stack = stack.pop()
        assertEquals(0, stack.size)
    }

    @Test
    fun smallStackTests() {
        fun <T> pushAndTestPopResult(stack: PersistentStack<T>, value: T): PersistentStack<T> {
            val pushResult = stack.push(value)
            val popResult = pushResult.pop()

            assertEquals(stack.peek(), popResult.peek())

            return pushResult
        }

        val empty = emptyStack<String>()

        val a = pushAndTestPopResult(empty, "a")
        val ab = pushAndTestPopResult(a, a.peek() + "b")
        val abc = pushAndTestPopResult(ab, ab.peek() + "c")

        pushAndTestPopResult(abc, abc.peek() + "d")
        pushAndTestPopResult(abc, abc.peek() + "e")

        val abcf = pushAndTestPopResult(abc, abc.peek() + "f")

        pushAndTestPopResult(abcf, abcf.peek() + "g")
        pushAndTestPopResult(abcf, abcf.peek() + "h")

        val x = pushAndTestPopResult(empty, "x")

        pushAndTestPopResult(x, x.peek() + "y")
        pushAndTestPopResult(x, x.peek() + "z")
    }

    @Test
    fun bigStackTests() {
        /*
                                      - - - - - - - d
                                    /
                        - - - - - - - - - - - - - - - - - - - - - - - - c
                      /             \
        0 - - - - - a                 - - - - - - - e
                      \
                        - - - - - - - - - - b
        */

        val random = Random()

        fun pushRandomIntTo(stack: PersistentStack<Int>, times: Int): PersistentStack<Int> {
            var pushed = stack
            repeat(times) {
                pushed = pushed.push(random.nextInt())
            }
            return pushed
        }

        var a = pushRandomIntTo(emptyStack(), times = 100000)
        var b = pushRandomIntTo(a, times = 200000)
        var c = pushRandomIntTo(a, times = 150000)
        var d = pushRandomIntTo(c, times = 100000)
        var e = pushRandomIntTo(c, times = 100000)
        c = pushRandomIntTo(c, times = 250000)

        fun popFrom(stack: PersistentStack<Int>, times: Int): PersistentStack<Int> {
            var popped = stack
            repeat(times) {
                popped = popped.pop()
            }
            return popped
        }

        c = popFrom(c, times = 250000)
        d = popFrom(d, times = 100000)
        e = popFrom(e, times = 100000)

        repeat(times = 150000) {
            assertEquals(1, listOf(c, d, e).map { it.peek() }.distinct().size)

            c = c.pop()
            d = d.pop()
            e = e.pop()
        }

        b = popFrom(b, times = 200000)

        repeat(times = 100000) {
            assertEquals(1, listOf(a, b, c, d, e).map { it.peek() }.distinct().size)

            a = a.pop()
            b = b.pop()
            c = c.pop()
            d = d.pop()
            e = e.pop()
        }
    }

    @Test
    fun randomOperationsTests() {
        val random = Random()
        val lists = mutableListOf<List<Int>>(emptyList())
        val stacks = mutableListOf<PersistentStack<Int>>(emptyStack())

        repeat(times = 100000) {
            val index = random.nextInt(lists.size)
            val list = lists[index]
            val stack = stacks[index]

            assertEquals(list.size, stack.size)
            assertEquals(list.lastOrNull(), stack.peek())

            val shouldPop = random.nextDouble() < 0.1
            if (!list.isEmpty() && shouldPop) {
                stacks.add(stack.pop())
                lists.add(list.dropLast(1))
            }

            val value = random.nextInt()
            stacks.add(stack.push(value))
            lists.add(list + listOf(value))
        }
    }
}