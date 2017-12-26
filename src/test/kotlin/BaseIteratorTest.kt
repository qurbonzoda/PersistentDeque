import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import java.util.*

open class BaseIteratorTest {
    protected fun baseTestNext(list: List<Any>, iteratorProducer: (Int) -> ListIterator<Any>) {
        for (index in 0..list.size) {
            val listIterator = list.listIterator(index)
            val treeIterator = iteratorProducer(index)

            iterateNextAndTest(listIterator, treeIterator, list.size)
        }
    }

    protected fun baseTestPrevious(list: List<Any>, iteratorProducer: (Int) -> ListIterator<Any>) {
        for (index in 0..list.size) {
            val listIterator = list.listIterator(index)
            val treeIterator = iteratorProducer(index)

            for (iterationCount in 0..list.size) {
                if (iterationCount % 2 == 0) {
                    iterateNextAndTest(listIterator, treeIterator, iterationCount)
                } else {
                    iteratePreviousAndTest(listIterator, treeIterator, iterationCount)
                }
            }
        }
    }

    private fun iterateNextAndTest(i1: ListIterator<Any>, i2: ListIterator<Any>, maxIterationCount: Int) {
        var iterationCount = 0
        while (i1.hasNext() && iterationCount < maxIterationCount) {
            assertTrue(i2.hasNext())
            assertEquals(i1.nextIndex(), i2.nextIndex())
            assertTrue(i1.next() === i2.next())
            iterationCount += 1
        }
        assertEquals(i1.hasNext(), i2.hasNext())
        assertEquals(i1.nextIndex(), i2.nextIndex())

        if (!i1.hasNext()) {
            assertThrows(NoSuchElementException::class.java) { i1.next() }
            assertThrows(NoSuchElementException::class.java) { i2.next() }
        }
    }

    private fun iteratePreviousAndTest(i1: ListIterator<Any>, i2: ListIterator<Any>, maxIterationCount: Int) {
        var iterationCount = 0
        while (i1.hasPrevious() && iterationCount < maxIterationCount) {
            assertTrue(i2.hasPrevious())
            assertEquals(i1.previousIndex(), i2.previousIndex())
            assertTrue(i1.previous() === i2.previous())
            iterationCount += 1
        }
        assertEquals(i1.hasPrevious(), i2.hasPrevious())
        assertEquals(i1.previousIndex(), i2.previousIndex())

        if (!i1.hasPrevious()) {
            assertThrows(NoSuchElementException::class.java) { i1.previous() }
            assertThrows(NoSuchElementException::class.java) { i2.previous() }
        }
    }
}
