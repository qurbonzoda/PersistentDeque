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
            return (if (topSubStack.lhs is EmptyBuffer) topSubStack.rhs.first else topSubStack.lhs.first) as T
        }

    val last: T?
        get() {
            val topSubStack = this.topSubStack ?: return null
            return (if (topSubStack.rhs is EmptyBuffer) topSubStack.lhs.last else topSubStack.rhs.last) as T
        }

    fun addFirst(value: T): PersistentDeque<T> {
        if (this.topSubStack == null) {
            val topSubStack = LevelStack(BufferOfOne(value), EmptyBuffer, null)
            return PersistentDeque(topSubStack, null)
        }
        return makeDequeRegular(this.topSubStack.lhs.addFirst(value), this.topSubStack.rhs)
    }

    fun removeFirst(): PersistentDeque<T> {
        val topSubStack = this.topSubStack ?: throw NoSuchElementException()

        return if (topSubStack.lhs is EmptyBuffer) {
            makeDequeRegular(topSubStack.lhs, topSubStack.rhs.removeFirst())
        } else {
            makeDequeRegular(topSubStack.lhs.removeFirst(), topSubStack.rhs)
        }
    }

    fun addLast(value: T): PersistentDeque<T> {
        if (this.topSubStack == null) {
            val topSubStack = LevelStack(EmptyBuffer, BufferOfOne(value), null)
            return PersistentDeque(topSubStack, null)
        }
        return makeDequeRegular(this.topSubStack.lhs, this.topSubStack.rhs.addLast(value))
    }

    fun removeLast(): PersistentDeque<T> {
        val topLevel = this.topSubStack ?: throw NoSuchElementException()

        return if (topLevel.rhs is EmptyBuffer) {
            makeDequeRegular(this.topSubStack.lhs.removeLast(), this.topSubStack.rhs)
        } else {
            makeDequeRegular(this.topSubStack.lhs, this.topSubStack.rhs.removeLast())
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
                    lhs = lhs.removeFirst()
                }
                return get(lIndex, lhs.first, depth)
            }
            if (rIndex < rhs.size shl depth) {
                while (rIndex >= 1 shl depth) {
                    rIndex -= 1 shl depth
                    rhs = rhs.removeLast()
                }
                return get((1 shl depth) - rIndex - 1, rhs.last, depth)
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
                    precedingValues.add(lhs.first)
                    lhs = lhs.removeFirst()
                    lIndex -= 1 shl depth
                }

                val newFirst = set(lIndex, value, lhs.first, depth)
                lhs = lhs.removeFirst().addFirst(newFirst)

                var precedingIndex = precedingValues.size - 1
                while (precedingIndex >= 0) {
                    lhs = lhs.addFirst(precedingValues[precedingIndex--])
                }

                lhsCollector.add(lhs)
                rhsCollector.add(rhs)
                break
            }
            if (rIndex < rhs.size shl depth) {
                val succeedingValues = mutableListOf<Any?>()

                while (rIndex >= 1 shl depth) {
                    succeedingValues.add(rhs.last)
                    rhs = rhs.removeLast()
                    rIndex -= 1 shl depth
                }

                val newLast = set((1 shl depth) - rIndex - 1, value, rhs.last, depth)
                rhs = rhs.removeLast().addLast(newLast)

                var succeedingIndex = succeedingValues.size - 1
                while (succeedingIndex >= 0) {
                    rhs = rhs.addLast(succeedingValues[succeedingIndex--])
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

        while (lhs !is EmptyBuffer) {
            fillListFromNode(lhs.first, depth, list)
            lhs = lhs.removeFirst()
        }

        fillListFromStack(levelIterator, depth + 1, list)

        while (rhs !is EmptyBuffer) {
            fillListFromNode(rhs.first, depth, list)
            rhs = rhs.removeFirst()
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
            if (lhs is EmptyBuffer && rhs is EmptyBuffer) {
                return
            }
            nextLhs = EmptyBuffer
            nextRhs = EmptyBuffer
        } else {
            nextLhs = levelIterator.topLhs()
            nextRhs = levelIterator.topRhs()
            levelIterator.next()
        }

        var lhs = lhs
        var rhs = rhs

        if (lhs.size >= 4) {
            nextLhs = lhs.addFirstPairOfLastTwoElementsTo(nextLhs)
            lhs = lhs.removeLastTwo()
        }
        if (rhs.size >= 4) {
            nextRhs = rhs.addLastPairOfFirstTwoElementsTo(nextRhs)
            rhs = rhs.removeFirstTwo()
        }

        if (lhs.size < 2) {
            if (nextLhs.size > 0) {
                lhs = moveFirstFromNextLevelBufferToLhs(nextLhs, lhs)
                nextLhs = nextLhs.removeFirst()
            } else if (nextRhs.size > 0) {
                lhs = moveFirstFromNextLevelBufferToLhs(nextRhs, lhs)
                nextRhs = nextRhs.removeFirst()
            }
        }
        if (rhs.size < 2) {
            if (nextRhs.size > 0) {
                rhs = moveLastFromNextLevelBufferToRhs(nextRhs, rhs)
                nextRhs = nextRhs.removeLast()
            } else if (nextLhs.size > 0) {
                rhs = moveLastFromNextLevelBufferToRhs(nextLhs, rhs)
                nextLhs = nextLhs.removeLast()
            }
        }

        val isNextLevelEmpty = nextLhs is EmptyBuffer && nextRhs is EmptyBuffer

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

    private fun moveLastFromNextLevelBufferToRhs(nextBuff: Buffer, rhs: Buffer): Buffer {
        val (e1, e2) = (nextBuff.last as Pair<*, *>)
        return rhs.addFirstTwo(e1, e2) // rhs of size 0 or 1
    }

    private fun moveFirstFromNextLevelBufferToLhs(nextBuff: Buffer, lhs: Buffer): Buffer {
        val (e1, e2) = (nextBuff.first as Pair<*, *>)
        return lhs.addLastTwo(e1, e2) // lhs of size 0 or 1
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
        if (lhs is EmptyBuffer) return rhs.color
        if (rhs is EmptyBuffer) return lhs.color
        return minOf(lhs.color, rhs.color)
    }


    companion object InstanceHolder {
        val emptyDeque = PersistentDeque<Any?>(null, null)
    }
}

fun <T> emptyDeque() = PersistentDeque.emptyDeque as PersistentDeque<T>
fun <T> dequeOf(element: T) = emptyDeque<T>().addFirst(element)