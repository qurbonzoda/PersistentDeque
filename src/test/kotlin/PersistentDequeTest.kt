import org.junit.Assert.*
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertThrows
import persistentDeque.PersistentDeque
import persistentDeque.emptyDeque
import java.util.*

class PersistentDequeTest {
    @Test
    fun isEmptyTests() {
        var deque = emptyDeque<String>()

        assertTrue(deque.isEmpty())
        assertFalse(deque.addFirst("first").isEmpty())
        assertFalse(deque.addLast("last").isEmpty())

        val elementsToAdd = 10
        repeat(times = elementsToAdd) { index ->
            deque = deque.addLast(index.toString())
            assertFalse(deque.isEmpty())
        }
        repeat(times = elementsToAdd - 1) {
            deque = deque.removeLast()
            assertFalse(deque.isEmpty())
        }
        deque = deque.removeLast()
        assertTrue(deque.isEmpty())
    }

    @Test
    fun sizeTests() {
        var deque = emptyDeque<Int>()

        assertTrue(deque.size == 0)
        assertEquals(1, deque.addFirst(1).size)
        assertEquals(1, deque.addLast(1).size)

        val elementsToAdd = 100
        repeat(times = elementsToAdd) { index ->
            deque = deque.addFirst(index)
            assertEquals(index + 1, deque.size)
        }
        repeat(times = elementsToAdd) { index ->
            deque = deque.removeLast()
            assertEquals(elementsToAdd - index - 1, deque.size)
        }
    }

    @Test
    fun toListTest() {
        var deque = emptyDeque<Int>()

        assertEquals(emptyList<Int>(), deque.toList())
        assertEquals(listOf(1), deque.addFirst(1).toList())
        assertEquals(listOf(1), deque.addLast(1).toList())

        assertEquals(
                listOf(1, 2, 3, 4, 5, 6),
                deque
                        .addLast(1).addLast(2).addLast(3).addLast(4).addLast(5)
                        .addLast(6)
                        .toList()
        )

        assertEquals(
                listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20),
                deque
                        .addLast(1).addLast(2).addLast(3).addLast(4).addLast(5)
                        .addLast(6).addLast(7).addLast(8).addLast(9).addLast(10)
                        .addLast(11).addLast(12).addLast(13).addLast(14).addLast(15)
                        .addLast(16).addLast(17).addLast(18).addLast(19).addLast(20)
                        .toList()
        )

        val elementsToAdd = 100
        val list = LinkedList<Int>()
        repeat(times = elementsToAdd) { index ->
            val shouldAddFirst = index % 2 == 0
            deque = if (shouldAddFirst) {
                list.addFirst(index)
                deque.addFirst(index)
            } else {
                list.addLast(index)
                deque.addLast(index)
            }
            assertEquals(list, deque.toList())
        }
        repeat(times = elementsToAdd) { index ->
            val shouldRemoveFirst = index % 2 == 1
            deque = if (shouldRemoveFirst) {
                list.removeFirst()
                deque.removeFirst()
            } else {
                list.removeLast()
                deque.removeLast()
            }
            assertEquals(list, deque.toList())
        }
    }

    @Test
    fun firstTests() {
        var deque = emptyDeque<Int>()

        assertNull(deque.first)
        assertEquals(1, deque.addFirst(1).first)
        assertEquals(1, deque.addLast(1).first)

        val elementsToAdd = 100
        repeat(times = elementsToAdd) { index ->
            deque = deque.addFirst(index)
            assertEquals(index, deque.first)
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - index - 1, deque.first)
            deque = deque.removeFirst()
        }
        assertNull(deque.first)
    }

    @Test
    fun lastTests() {
        var deque = emptyDeque<Int>()

        assertNull(deque.last)
        assertEquals(1, deque.addFirst(1).last)
        assertEquals(1, deque.addLast(1).last)

        val elementsToAdd = 100
        repeat(times = elementsToAdd) { index ->
            deque = deque.addLast(index)
            assertEquals(index, deque.last)
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - index - 1, deque.last)
            deque = deque.removeLast()
        }
        assertNull(deque.last)
    }

    @Test
    fun addFirstTests() {
        var deque = emptyDeque<Int>()

        assertNull(deque.first)
        assertEquals(1, deque.addFirst(1).first)
        assertEquals(1, deque.addFirst(1).last)

        val elementsToAdd = 100
        repeat(times = elementsToAdd) { index ->
            deque = deque.addFirst(index)

            assertEquals(index, deque.first)
            assertEquals(0, deque.last)
            assertEquals(index + 1, deque.size)
            assertEquals(List(index + 1) { index - it }, deque.toList())
        }
    }

    @Test
    fun addLastTests() {
        var deque = emptyDeque<Int>()

        assertNull(deque.first)
        assertEquals(1, deque.addLast(1).first)
        assertEquals(1, deque.addLast(1).last)

        val elementsToAdd = 100
        repeat(times = elementsToAdd) { index ->
            deque = deque.addLast(index)

            assertEquals(0, deque.first)
            assertEquals(index, deque.last)
            assertEquals(index + 1, deque.size)
            assertEquals(List(index + 1) { it }, deque.toList())
        }
    }

    @Test
    fun removeFirstTests() {
        var deque = emptyDeque<Int>()

        assertThrows(NoSuchElementException::class.java) {
            deque.removeFirst()
        }
        assertTrue(deque.addLast(1).removeFirst().isEmpty())
        assertTrue(deque.addFirst(1).removeFirst().isEmpty())

        val elementsToAdd = 100
        repeat(times = elementsToAdd) { index ->
            deque = deque.addLast(index)
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - 1, deque.last)
            assertEquals(index, deque.first)
            assertEquals(elementsToAdd - index, deque.size)
            assertEquals(List(elementsToAdd - index) { it + index }, deque.toList())

            deque = deque.removeFirst()
        }
    }

    @Test
    fun removeLastTests() {
        var deque = emptyDeque<Int>()

        assertThrows(NoSuchElementException::class.java) {
            deque.removeLast()
        }
        assertTrue(deque.addLast(1).removeLast().isEmpty())
        assertTrue(deque.addFirst(1).removeLast().isEmpty())

        val elementsToAdd = 100
        repeat(times = elementsToAdd) { index ->
            deque = deque.addFirst(index)
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(index, deque.last)
            assertEquals(elementsToAdd - 1, deque.first)
            assertEquals(elementsToAdd - index, deque.size)
            assertEquals(List(elementsToAdd - index) { elementsToAdd - 1 - it }, deque.toList())

            deque = deque.removeLast()
        }
    }

    @Test
    fun getTests() {
        var deque = emptyDeque<Int>()

        assertThrows(IndexOutOfBoundsException::class.java) {
            deque.get(0)
        }
        assertEquals(1, deque.addLast(1).get(0))
        assertEquals(1, deque.addFirst(1).get(0))

        val elementsToAdd = 100
        repeat(times = elementsToAdd) { index ->
            deque = deque.addLast(index)

            for (i in 0..index) {
                assertEquals(i, deque.get(i))
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in index until elementsToAdd) {
                assertEquals(i, deque.get(i - index))
            }

            deque = deque.removeFirst()
        }
    }

    @Test
    fun setTests() {
        var deque = emptyDeque<Int>()

        assertThrows(IndexOutOfBoundsException::class.java) {
            deque.set(0, 0)
        }
        assertEquals(2, deque.addLast(1).set(0, 2).get(0))
        assertEquals(2, deque.addFirst(1).set(0, 2).get(0))

        val elementsToAdd = 100
        repeat(times = elementsToAdd) { index ->
            deque = deque.addLast(index * 2)

            for (i in 0..index) {
                assertEquals(i + index, deque.get(i))
                deque = deque.set(i, i + index + 1)
                assertEquals(i + index + 1, deque.get(i))
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in 0..(elementsToAdd - index - 1)) {
                val expected = elementsToAdd + i

                assertEquals(expected, deque.get(i))
                deque = deque.set(i, expected - 1)
                assertEquals(expected - 1, deque.get(i))
            }

            deque = deque.removeFirst()
        }
    }

    @Test
    fun randomOperationsTests() {
        val random = Random()
        val lists = mutableListOf(emptyList<Int>())
        val deques = mutableListOf(emptyDeque<Int>())

        repeat(times = 100000) {
            val quarter = (lists.size + 3) / 4

            val index = (lists.size - quarter) + random.nextInt(quarter)
            val list = lists[index]
            val deque = deques[index]

            val operationType = random.nextDouble()

            val shouldRemoveFirst = operationType < 0.2
            val shouldRemoveLast = operationType > 0.2 && operationType < 0.4
            val shouldAddFirst = operationType > 0.4 &&  operationType < 0.70

            val newList: List<Int>
            val newDeque: PersistentDeque<Int>

            if (!list.isEmpty() && shouldRemoveFirst) {
                newList = list.drop(1)
                newDeque = deque.removeFirst()
            }
            else if (!list.isEmpty() && shouldRemoveLast) {
                newList = list.dropLast(1)
                newDeque = deque.removeLast()
            }
            else if (shouldAddFirst) {
                val value = random.nextInt()
                newList = listOf(value) + list
                newDeque = deque.addFirst(value)
            }
            else {
                val value = random.nextInt()
                newList = list + listOf(value)
                newDeque = deque.addLast(value)
            }

            assertEquals(newList, newDeque.toList())

            deques.add(newDeque)
            lists.add(newList)
        }

//        println(lists.maxBy { it.size }?.size)

        lists.forEachIndexed { index, listAtIndex ->
            var deque = deques[index]
            var list = listAtIndex

            while (!list.isEmpty()) {
                assertEquals(list, deque.toList())

                val shouldRemoveFirst = random.nextBoolean()
                if (shouldRemoveFirst) {
                    list = list.drop(1)
                    deque = deque.removeFirst()
                } else {
                    list = list.dropLast(1)
                    deque = deque.removeLast()
                }
            }
        }
    }

    @Test
    fun randomOperationsFastTests() {
        repeat(times = 10) {

            val random = Random()
            val lists = List(20) { LinkedList<Int>() }
            val deques = MutableList(20) { emptyDeque<Int>() }

            repeat(times = 1000000) {
                val index = random.nextInt(lists.size)
                val list = lists[index]
                val deque = deques[index]

                val operationType = random.nextDouble()

                val shouldRemoveFirst = operationType < 0.15
                val shouldRemoveLast = operationType > 0.15 && operationType < 0.3
                val shouldAddFirst = operationType > 0.3 && operationType < 0.65

                val newDeque = if (!list.isEmpty() && shouldRemoveFirst) {
                    list.removeFirst()
                    deque.removeFirst()
                } else if (!list.isEmpty() && shouldRemoveLast) {
                    list.removeLast()
                    deque.removeLast()
                } else if (shouldAddFirst) {
                    val value = random.nextInt()
                    list.addFirst(value)
                    deque.addFirst(value)
                } else {
                    val value = random.nextInt()
                    list.addLast(value)
                    deque.addLast(value)
                }

                assertEquals(list.isEmpty(), newDeque.isEmpty())
                assertEquals(list.firstOrNull(), newDeque.first)
                assertEquals(list.lastOrNull(), newDeque.last)
                assertEquals(list.size, newDeque.size)

                deques[index] = newDeque
            }

//            println(lists.maxBy { it.size }?.size)

            lists.forEachIndexed { index, listAtIndex ->
                var deque = deques[index]
                val list = listAtIndex

                while (!list.isEmpty()) {
                    assertEquals(list.isEmpty(), deque.isEmpty())
                    assertEquals(list.first, deque.first)
                    assertEquals(list.last, deque.last)
                    assertEquals(list.size, deque.size)

                    val shouldRemoveFirst = random.nextBoolean()
                    deque = if (shouldRemoveFirst) {
                        list.removeFirst()
                        deque.removeFirst()
                    } else {
                        list.removeLast()
                        deque.removeLast()
                    }
                }
            }
        }
    }
}