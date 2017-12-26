import buffer.*
import org.junit.Test
import persistentDeque.BufferIterator
import java.util.*

class BufferIteratorTest: BaseIteratorTest() {
    @Test
    fun nextTests() {
        testNext(EmptyBuffer)
        testNext(BufferOfOne(1))
        testNext(BufferOfTwo(1, 2))
        testNext(BufferOfThree(1, 2, 3))
        testNext(BufferOfFour(1, 2, 3, 4))
        testNext(BufferOfFive(1, 2, 3, 4, 5))
    }

    private fun testNext(buffer: Buffer) {
        val list = asList(buffer)
        baseTestNext(list, { index ->
            BufferIterator(buffer, index)
        })
    }

    private fun asList(buffer: Buffer): List<Any> {
        var b = buffer
        val list = ArrayList<Any>()
        while (b.size > 0) {
            list.add(b.first)
            b = b.removeFirst()
        }
        return list
    }

    @Test
    fun previousTests() {
        testPrevious(EmptyBuffer)
        testPrevious(BufferOfOne(1))
        testPrevious(BufferOfTwo(1, 2))
        testPrevious(BufferOfThree(1, 2, 3))
        testPrevious(BufferOfFour(1, 2, 3, 4))
        testPrevious(BufferOfFive(1, 2, 3, 4, 5))
    }

    private fun testPrevious(buffer: Buffer) {
        val list = asList(buffer)
        baseTestPrevious(list, { index ->
            BufferIterator(buffer, index)
        })
    }
}