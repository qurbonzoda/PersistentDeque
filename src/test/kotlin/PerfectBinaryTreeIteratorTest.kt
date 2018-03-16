import immutableDeque.perfectBinaryTreeIterator.PerfectBinaryTreeIterator
import org.junit.Test

class PerfectBinaryTreeIteratorTest: BaseIteratorTest() {
    @Test
    fun nextTests() {
        testNext(createPerfectBinaryTree(0, 0), 0)
        testNext(createPerfectBinaryTree(1, 0), 1)
        testNext(createPerfectBinaryTree(4, 0), 4)
        testNext(createPerfectBinaryTree(7, 0), 7)
        testNext(createPerfectBinaryTree(9, 0), 9)
        testNext(createPerfectBinaryTree(12, 0), 12)
    }

    private fun createPerfectBinaryTree(height: Int, index: Int): Any {
        if (height == 0) {
            return index
        }
        val lhs = createPerfectBinaryTree(height - 1, index)
        val rhs = createPerfectBinaryTree(height - 1, index + (1 shl (height - 1)))
        return Pair(lhs, rhs)
    }

    private fun testNext(root: Any, height: Int) {
        val list = asList(root, height)
        baseTestNext(list, { index ->
            PerfectBinaryTreeIterator(root, height, index)
        })
    }

    private fun asList(root: Any, height: Int): List<Any> {
        if (height == 0) {
            return listOf(root)
        }
        val (lhs, rhs) = root as Pair<Any, Any>
        return asList(lhs, height - 1) + asList(rhs, height - 1)
    }

    @Test
    fun previousTests() {
        testPrevious(createPerfectBinaryTree(0, 0), 0)
        testPrevious(createPerfectBinaryTree(1, 0), 1)
        testPrevious(createPerfectBinaryTree(4, 0), 4)
        testPrevious(createPerfectBinaryTree(6, 0), 6)
        testPrevious(createPerfectBinaryTree(7, 0), 7)
        testPrevious(createPerfectBinaryTree(9, 0), 9)
    }

    private fun testPrevious(root: Any, height: Int) {
        val list = asList(root, height)
        baseTestPrevious(list, { index ->
            PerfectBinaryTreeIterator(root, height, index)
        })
    }
}