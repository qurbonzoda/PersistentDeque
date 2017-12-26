import org.junit.Test
import persistentDeque.PersistentDeque
import persistentDeque.emptyDeque
import java.util.*

class PersistentDequeIteratorTest: BaseIteratorTest() {
    @Test
    fun nextTests() {
        testNext(createPersistentDeque(0))
        testNext(createPersistentDeque(4))
        testNext(createPersistentDeque(5))
        testNext(createPersistentDeque(100))
        testNext(createPersistentDeque(1000))
        testNext(createPersistentDeque(10000))
    }

    private fun createPersistentDeque(size: Int): PersistentDeque<Int> {
        var deque = emptyDeque<Int>()
        val random = Random()

        repeat(times = size) { index ->
            deque = if (random.nextBoolean()) {
                deque.addFirst(index)
            } else {
                deque.addLast(index)
            }
        }

        return deque
    }

    private fun testNext(deque: PersistentDeque<Int>) {
        val list = deque.toList()
        baseTestNext(list, { index ->
            deque.listIterator(index)
        })
    }

    @Test
    fun previousTests() {
        testPrevious(createPersistentDeque(0))
        testPrevious(createPersistentDeque(4))
        testPrevious(createPersistentDeque(15))
        testPrevious(createPersistentDeque(100))
        testPrevious(createPersistentDeque(255))
        testPrevious(createPersistentDeque(400))
    }

    private fun testPrevious(deque: PersistentDeque<Int>) {
        val list = deque.toList()
        baseTestPrevious(list, { index ->
            deque.listIterator(index)
        })
    }
}