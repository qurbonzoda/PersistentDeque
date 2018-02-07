package persistentDeque

import buffer.*

private const val YELLOW_RED = 3
private const val YELLOW_YELLOW = 4
private const val YELLOW_GREEN = 5
private const val GREEN_YELLOW = 7
private const val GREEN_GREEN = 8

private fun colorChange(oldColor: Int, newColor: Int): Int {
    return oldColor * 3 + newColor
}

internal data class LevelStack(val lhs: Buffer, val rhs: Buffer, val next: LevelStack?)

internal data class DequeSubStack(val stack: LevelStack, val next: DequeSubStack?)

class PersistentDeque<T> internal constructor(
        private val topSubStack: LevelStack?, private val next: DequeSubStack?
) {
//    init {
//        assert(topSubStack != null || next == null)
//    }

//    fun contains(element: T): Boolean
//    fun containsAll(elements: Collection<T>): Boolean

//    fun indexOf(element: T): Int
//    fun lastIndexOf(element: T): Int

    fun isEmpty(): Boolean {
        return isEmpty(this.topSubStack, this.next)
    }

    private fun isEmpty(topSubStack: LevelStack?, next: DequeSubStack?): Boolean {
        return topSubStack == null && next == null
    }

    val size: Int
        get() {
            val levelIterator = LevelIterator(this.topSubStack, this.next)
            var size = 0
            var depth = 0

            while (levelIterator.hasNext()) {
                val lhs = levelIterator.topLhs()
                val rhs = levelIterator.topRhs()
                levelIterator.next()
                size += (lhs.size + rhs.size) shl depth
                depth += 1
            }
            return size
        }

    val first: T?
        get() {
            val topSubStack = this.topSubStack ?: return null
            return (if (topSubStack.lhs === Buffer.empty) topSubStack.rhs.bottom else topSubStack.lhs.top) as T
        }

    val last: T?
        get() {
            val topSubStack = this.topSubStack ?: return null
            return (if (topSubStack.rhs === Buffer.empty) topSubStack.lhs.bottom else topSubStack.rhs.top) as T
        }

    fun addFirst(value: T): PersistentDeque<T> {
        if (this.topSubStack == null) {
            val topSubStack = LevelStack(Buffer.empty.push(value), Buffer.empty, null)
            return PersistentDeque(topSubStack, null)
        }
        return makeDequeRegular(this.topSubStack.lhs.push(value), this.topSubStack.rhs)
    }

    fun removeFirst(): PersistentDeque<T> {
        val topSubStack = this.topSubStack ?: throw NoSuchElementException()

        return if (topSubStack.lhs === Buffer.empty) {
            makeDequeRegular(topSubStack.lhs, topSubStack.rhs.removeBottom())
        } else {
            makeDequeRegular(topSubStack.lhs.pop(), topSubStack.rhs)
        }
    }

    fun addLast(value: T): PersistentDeque<T> {
        if (this.topSubStack == null) {
            val topSubStack = LevelStack(Buffer.empty, Buffer.empty.push(value), null)
            return PersistentDeque(topSubStack, null)
        }
        return makeDequeRegular(this.topSubStack.lhs, this.topSubStack.rhs.push(value))
    }

    fun removeLast(): PersistentDeque<T> {
        val topLevel = this.topSubStack ?: throw NoSuchElementException()

        return if (topLevel.rhs === Buffer.empty) {
            makeDequeRegular(this.topSubStack.lhs.removeBottom(), this.topSubStack.rhs)
        } else {
            makeDequeRegular(this.topSubStack.lhs, this.topSubStack.rhs.pop())
        }
    }

    fun toList(): List<T> {
        val list = mutableListOf<T>()
        fillListFromStack(LevelIterator(this.topSubStack, this.next), 0, list)
        return list
    }

    fun get(index: Int): T {
        if (index < 0 || index >= this.size) throw IndexOutOfBoundsException()

        var lIndex = index
        var rIndex = this.size - index - 1

        val levelIterator = LevelIterator(this.topSubStack, this.next)

        var depth = 0

        while (levelIterator.hasNext()) {
            var lhs = levelIterator.topLhs()
            var rhs = levelIterator.topRhs()
            levelIterator.next()

            if (lIndex < lhs.size shl depth) {
                while (lIndex >= 1 shl depth) {
                    lIndex -= 1 shl depth
                    lhs = lhs.pop()
                }
                return get(lIndex, lhs.top, depth)
            }
            if (rIndex < rhs.size shl depth) {
                while (rIndex >= 1 shl depth) {
                    rIndex -= 1 shl depth
                    rhs = rhs.pop()
                }
                return get((1 shl depth) - rIndex - 1, rhs.top, depth)
            }

            lIndex -= lhs.size shl depth
            rIndex -= rhs.size shl depth

            depth += 1
        }

        throw AssertionError("Unreachable")
    }

    fun set(index: Int, value: T): PersistentDeque<T> {
        if (index < 0 || index >= this.size) throw IndexOutOfBoundsException()

        var lIndex = index
        var rIndex = this.size - index - 1

        val levelIterator = LevelIterator(this.topSubStack, this.next)
        val lhsCollector = mutableListOf<Buffer>()
        val rhsCollector = mutableListOf<Buffer>()

        var depth = 0

        while (levelIterator.hasNext()) {
            var lhs = levelIterator.topLhs()
            var rhs = levelIterator.topRhs()
            levelIterator.next()

            if (lIndex < lhs.size shl depth) {
                val precedingValues = mutableListOf<Any?>()

                while (lIndex >= 1 shl depth) {
                    precedingValues.add(lhs.top)
                    lhs = lhs.pop()
                    lIndex -= 1 shl depth
                }

                val newFirst = set(lIndex, value, lhs.top, depth)
                lhs = lhs.pop().push(newFirst)

                var precedingIndex = precedingValues.size - 1
                while (precedingIndex >= 0) {
                    lhs = lhs.push(precedingValues[precedingIndex--])
                }

                lhsCollector.add(lhs)
                rhsCollector.add(rhs)
                break
            }
            if (rIndex < rhs.size shl depth) {
                val succeedingValues = mutableListOf<Any?>()

                while (rIndex >= 1 shl depth) {
                    succeedingValues.add(rhs.top)
                    rhs = rhs.pop()
                    rIndex -= 1 shl depth
                }

                val newLast = set((1 shl depth) - rIndex - 1, value, rhs.top, depth)
                rhs = rhs.pop().push(newLast)

                var succeedingIndex = succeedingValues.size - 1
                while (succeedingIndex >= 0) {
                    rhs = rhs.push(succeedingValues[succeedingIndex--])
                }

                lhsCollector.add(lhs)
                rhsCollector.add(rhs)
                break
            }

            lIndex -= lhs.size shl depth
            rIndex -= rhs.size shl depth

            depth += 1
            lhsCollector.add(lhs)
            rhsCollector.add(rhs)
        }

//        assert(lhsCollector.size == rhsCollector.size)

        var levelIndex = lhsCollector.size - 1
        while (levelIndex >= 0) {
            levelIterator.add(lhsCollector[levelIndex], rhsCollector[levelIndex])
            levelIndex -= 1
        }

        return levelIterator.createPersistentDeque()
    }

    fun listIterator(index: Int): ListIterator<T> {
        val levelIterator = LevelIterator(topSubStack, next)
        return PersistentDequeIterator(levelIterator, index, size)
    }

    fun listIterator(): ListIterator<T> {
        return listIterator(0)
    }

    fun iterator(): Iterator<T> {
        return listIterator()
    }

    private fun get(index: Int, node: Any?, depth: Int): T {
        if (depth == 0) {
            return node as T
        }
        val pair = node as Pair<*, *>
        val lSize = 1 shl (depth - 1)

        if (index < lSize) {
            return get(index, pair.first, depth - 1)
        }
        return get(index - lSize, pair.second, depth - 1)
    }

    private fun set(index: Int, value: T, node: Any?, depth: Int): Any? {
        if (depth == 0) {
            return value
        }
        val pair = node as Pair<*, *>
        val lSize = 1 shl (depth - 1)

        if (index < lSize) {
            val newFirst = set(index, value, pair.first, depth - 1)
            return Pair(newFirst, pair.second)
        }
        val newSecond = set(index - lSize, value, pair.second, depth - 1)
        return Pair(pair.first, newSecond)
    }

    private fun fillListFromStack(levelIterator: LevelIterator,
                                  depth: Int,
                                  list: MutableList<T>) {

        if (!levelIterator.hasNext()) {
            return
        }

        var lhs = levelIterator.topLhs()
        var rhs = levelIterator.topRhs()
        levelIterator.next()

        while (lhs !== Buffer.empty) {
            fillListFromNode(lhs.top, depth, list)
            lhs = lhs.pop()
        }

        fillListFromStack(levelIterator, depth + 1, list)

        while (rhs !== Buffer.empty) {
            fillListFromNode(rhs.bottom, depth, list)
            rhs = rhs.removeBottom()
        }
    }

    private fun fillListFromNode(node: Any?, depth: Int, list: MutableList<T>) {
        if (depth == 0) {
            list.add(node as T)
        } else {
            val pair = node as Pair<*, *>
            fillListFromNode(pair.first, depth - 1, list)
            fillListFromNode(pair.second, depth - 1, list)
        }
    }

    private fun makeDequeRegular(newTopLhs: Buffer, newTopRhs: Buffer): PersistentDeque<T> {
        val topSubStackNext = this.topSubStack!!.next
        val next = this.next

        val isTopLevelOnlyLevel = isEmpty(topSubStackNext, next)

        return when(colorChange(this.topSubStack.lhs, this.topSubStack.rhs, newTopLhs, newTopRhs, isTopLevelOnlyLevel)) {
            GREEN_GREEN,
            YELLOW_GREEN,
            YELLOW_YELLOW -> PersistentDeque(LevelStack(newTopLhs, newTopRhs, topSubStackNext), next)

            GREEN_YELLOW -> {
                if (isTopLevelOnlyLevel || next == null
                        || levelColor(next.stack.lhs, next.stack.rhs, isEmpty(next.stack.next, next.next)) == GREEN) {
                    PersistentDeque(LevelStack(newTopLhs, newTopRhs, topSubStackNext), next)
                } else {
                    val levelIterator = LevelIterator(next.stack.next, next.next)
                    makeRedLevelGreen(next.stack.lhs, next.stack.rhs, levelIterator)
                    levelIterator.addStack(LevelStack(newTopLhs, newTopRhs, topSubStackNext))
                    levelIterator.createPersistentDeque()
                }
            }

            YELLOW_RED -> {
                val levelIterator = LevelIterator(topSubStackNext, next)
                makeRedLevelGreen(newTopLhs, newTopRhs, levelIterator)
                levelIterator.createPersistentDeque()
            }

            else -> throw IllegalStateException()
        }
    }

    private fun makeRedLevelGreen(lhs: Buffer, rhs: Buffer, levelIterator: LevelIterator) {
//        assert(nonBottomLevelColor(lhs, rhs) == RED)

        var nextLhs: Buffer
        var nextRhs: Buffer
        if (!levelIterator.hasNext()) {
            if (lhs === Buffer.empty && rhs === Buffer.empty) {
                return
            }
            nextLhs = Buffer.empty
            nextRhs = Buffer.empty
        } else {
            nextLhs = levelIterator.topLhs()
            nextRhs = levelIterator.topRhs()
            levelIterator.next()
        }

        var lhs = lhs
        var rhs = rhs

        if (lhs.size >= 4) {
            nextLhs = lhs.pushPairOfBottomTwoElementsTo(nextLhs, isLhs = true)
            lhs = lhs.removeBottomTwo()
        }
        if (rhs.size >= 4) {
            nextRhs = rhs.pushPairOfBottomTwoElementsTo(nextRhs, isLhs = false)
            rhs = rhs.removeBottomTwo()
        }

        if (lhs.size < 2) {
            if (nextLhs.size > 0) {
                val (e1, e2) = nextLhs.top as Pair<*, *>
                lhs = lhs.prependTwo(e2, e1)
                nextLhs = nextLhs.pop()
            } else if (nextRhs.size > 0) {
                val (e1, e2) = nextRhs.bottom as Pair<*, *>
                lhs = lhs.prependTwo(e2, e1)
                nextRhs = nextRhs.removeBottom()
            }
        }
        if (rhs.size < 2) {
            if (nextRhs.size > 0) {
                val (e1, e2) = nextRhs.top as Pair<*, *>
                rhs = rhs.prependTwo(e1, e2)
                nextRhs = nextRhs.pop()
            } else if (nextLhs.size > 0) {
                val (e1, e2) = nextLhs.bottom as Pair<*, *>
                rhs = rhs.prependTwo(e1, e2)
                nextLhs = nextLhs.removeBottom()
            }
        }

        val isNextLevelEmpty = nextLhs.size == 0 && nextRhs.size == 0

        if (shouldMakeGreenNextLevel(nextLhs, nextRhs, levelIterator)) {
            makeRedLevelGreen(nextLhs, nextRhs, levelIterator)
        } else if (levelIterator.hasNext() || !isNextLevelEmpty) {
            levelIterator.add(nextLhs, nextRhs)
        }
        levelIterator.add(lhs, rhs)
    }

    private fun shouldMakeGreenNextLevel(nextLhs: Buffer, nextRhs: Buffer, levelIterator: LevelIterator): Boolean {
        if (!levelIterator.hasNext() || !levelIterator.hasOnlyOneLevel()) return false
        if (nonBottomLevelColor(nextLhs, nextRhs) != RED) return false

        var takeCount = if (nextLhs.size < 2) 1 else if (nextLhs.size > 3) -1 else 0
        takeCount += if (nextRhs.size < 2) 1 else if (nextRhs.size > 3) -1 else 0

        return levelIterator.topLhs().size + levelIterator.topRhs().size <= takeCount
    }

    private fun colorChange(oldLhs: Buffer, oldRhs: Buffer, newLhs: Buffer, newRhs: Buffer, isBottomLevel: Boolean): Int {
        return if (isBottomLevel)
            bottomColorChange(oldLhs, oldRhs, newLhs, newRhs)
        else
            nonBottomColorChange(oldLhs, oldRhs, newLhs, newRhs)
    }

    private fun nonBottomColorChange(oldLhs: Buffer, oldRhs: Buffer, newLhs: Buffer, newRhs: Buffer): Int {
        return colorChange(nonBottomLevelColor(oldLhs, oldRhs), nonBottomLevelColor(newLhs, newRhs))
    }

    private fun bottomColorChange(oldLhs: Buffer, oldRhs: Buffer, newLhs: Buffer, newRhs: Buffer): Int {
        return colorChange(bottomLevelColor(oldLhs, oldRhs), bottomLevelColor(newLhs, newRhs))
    }

    private fun levelColor(lhs: Buffer, rhs: Buffer, isBottomLevel: Boolean): Int {
        return if (isBottomLevel)
            bottomLevelColor(lhs, rhs)
        else
            nonBottomLevelColor(lhs, rhs)
    }

    private fun nonBottomLevelColor(lhs: Buffer, rhs: Buffer): Int {
        return minOf(lhs.color, rhs.color)
    }

    private fun bottomLevelColor(lhs: Buffer, rhs: Buffer): Int {
        if (lhs === Buffer.empty) return rhs.color
        if (rhs === Buffer.empty) return lhs.color
        return minOf(lhs.color, rhs.color)
    }


    companion object InstanceHolder {
        val emptyDeque = PersistentDeque<Any?>(null, null)
    }
}

fun <T> emptyDeque() = PersistentDeque.emptyDeque as PersistentDeque<T>
fun <T> dequeOf(element: T) = emptyDeque<T>().addFirst(element)