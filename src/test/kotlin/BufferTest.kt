import buffer.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test

class BufferTest {
    @Test
    fun topTests() {
        var buffer = Buffer.empty

        val elementsToAdd = 1000
        repeat(elementsToAdd) { index ->
            buffer = buffer.push(index)
            assertEquals(index, buffer.top)
        }
        repeat(elementsToAdd) { index ->
            assertEquals(elementsToAdd - index - 1, buffer.top)
            buffer = buffer.pop()
        }
    }

    @Test
    fun bottomTests() {
        var buffer = Buffer.empty

        val elementsToAdd = 1000
        repeat(elementsToAdd) { index ->
            buffer = buffer.push(index)
            assertEquals(0, buffer.bottom)
        }
        repeat(elementsToAdd) {
            assertEquals(0, buffer.bottom)
            buffer = buffer.pop()
        }
    }

    @Test
    fun pushTests() {
        var buffer = Buffer.empty

        val elementsToAdd = 1000
        repeat(elementsToAdd) { index ->
            buffer = buffer.push(index)
            assertEquals(index, buffer.top)
            assertEquals(index + 1, buffer.size)
        }
    }

    @Test
    fun popTests() {
        var buffer = Buffer.empty

        val elementsToAdd = 100
        repeat(elementsToAdd) { index ->
            buffer = buffer.push(index)
        }

        var poppedCount = 0
        repeat(elementsToAdd) { index ->
            if (poppedCount == elementsToAdd) return

            assertEquals(elementsToAdd - poppedCount - 1, buffer.top)
            assertEquals(elementsToAdd - poppedCount, buffer.size)
            val popCount = if (index > buffer.size) 1 else index
            buffer = buffer.pop(popCount)
            poppedCount += popCount
        }
    }

    @Test
    fun removeBottomTests() {
        var buffer = Buffer.empty

        val elementsToAdd = 1000
        repeat(elementsToAdd) { index ->
            buffer = buffer.push(index)
        }
        var removedCount = 0
        repeat(elementsToAdd) { index ->
            if (removedCount == elementsToAdd) return

            assertEquals(removedCount, buffer.bottom)
            assertEquals(elementsToAdd - removedCount, buffer.size)
            val removeCount = if (index > buffer.size) 1 else index
            buffer = buffer.removeBottom(removeCount)
            removedCount += removeCount
        }
    }

    @Test
    fun addElementsToTests() {
        val buffer = Buffer.empty.push(0).push(1).push(2).push(3)

        val emptyList = ArrayList<Any?>()
        buffer.addElementsTo(emptyList, reverseOrder = true)
        assertEquals(listOf(0, 1, 2, 3), emptyList)

        val nonEmptyList = ArrayList<Any?>()
        nonEmptyList.add(-1)
        buffer.addElementsTo(nonEmptyList, reverseOrder = false)
        assertEquals(listOf(-1, 3, 2, 1, 0), nonEmptyList)
    }

    @Test
    fun pushPairsOfBottomElementsToTests() {
        val buffer = Buffer.empty.push(0).push(1).push(2).push(3).push(4)

        val lhs = buffer.pushPairsOfBottomElementsTo(Buffer.empty, 2, true)
        assertEquals(2, lhs.size)
        assertEquals(Pair(3, 2), lhs.top)
        assertEquals(Pair(1, 0), lhs.pop().top)

        val rhs = buffer.pushPairsOfBottomElementsTo(Buffer.empty.push(-1), 2, false)
        assertEquals(3, rhs.size)
        assertEquals(Pair(2, 3), rhs.top)
        assertEquals(Pair(0, 1), rhs.pop().top)
        assertEquals(-1, rhs.pop(2).top)
    }

    @Test
    fun topElementsToPrevLevelTests() {
        val buffer = Buffer.empty.push(Pair(0, 1)).push(Pair(2, 3)).push(Pair(4, 5))

        val lhs = buffer.topElementsToPrevLevel(2, true)
        assertEquals(4, lhs.size)
        assertEquals(4, lhs.top)
        assertEquals(5, lhs.pop().top)
        assertEquals(2, lhs.pop(2).top)
        assertEquals(3, lhs.pop(3).top)

        val rhs = buffer.topElementsToPrevLevel(2, false)
        assertEquals(4, rhs.size)
        assertEquals(5, rhs.top)
        assertEquals(4, rhs.pop().top)
        assertEquals(3, rhs.pop(2).top)
        assertEquals(2, rhs.pop(3).top)
    }

    @Test
    fun bottomElementsToPrevLevelTests() {
        val buffer = Buffer.empty.push(Pair(0, 1)).push(Pair(2, 3)).push(Pair(4, 5))

        val lhs = buffer.bottomElementsToPrevLevel(2, true)
        assertEquals(4, lhs.size)
        assertEquals(0, lhs.top)
        assertEquals(1, lhs.pop().top)
        assertEquals(2, lhs.pop(2).top)
        assertEquals(3, lhs.pop(3).top)

        val rhs = buffer.bottomElementsToPrevLevel(2, false)
        assertEquals(4, rhs.size)
        assertEquals(1, rhs.top)
        assertEquals(0, rhs.pop().top)
        assertEquals(3, rhs.pop(2).top)
        assertEquals(2, rhs.pop(3).top)
    }

    @Test
    fun prependSavingOrderTests() {
        val buffer = Buffer.empty.push(0).push(1).push(2)

        val lhs = buffer.prependSavingOrder(Buffer.empty.push(-2).push(-1))
        assertEquals(5, lhs.size)
        assertEquals(2, lhs.top)
        assertEquals(1, lhs.pop().top)
        assertEquals(0, lhs.pop(2).top)
        assertEquals(-1, lhs.pop(3).top)
        assertEquals(-2, lhs.pop(4).top)
    }
}
