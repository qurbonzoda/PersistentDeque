import immutableDeque.ImmutableDeque
import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize25S.emptyDeque.emptyDeque
import org.junit.Test
import java.util.*

class PersistentDequeIteratorTest: BaseIteratorTest() {
    @Test
    fun nextTests() {
        testNext(makeImmutableDeque(0))
        testNext(makeImmutableDeque(4))
        testNext(makeImmutableDeque(5))
        testNext(makeImmutableDeque(100))
        testNext(makeImmutableDeque(1000))
        testNext(makeImmutableDeque(10000))
    }

    private fun makeImmutableDeque(size: Int): ImmutableDeque<Int> {
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

    private fun testNext(deque: ImmutableDeque<Int>) {
        val list = deque.toList()
        baseTestNext(list, { index ->
            deque.listIterator(index)
        })
    }

    @Test
    fun previousTests() {
        testPrevious(makeImmutableDeque(0))
        testPrevious(makeImmutableDeque(4))
        testPrevious(makeImmutableDeque(15))
        testPrevious(makeImmutableDeque(100))
        testPrevious(makeImmutableDeque(255))
        testPrevious(makeImmutableDeque(400))
    }

    private fun testPrevious(deque: ImmutableDeque<Int>) {
        val list = deque.toList()
        baseTestPrevious(list, { index ->
            deque.listIterator(index)
        })
    }
}