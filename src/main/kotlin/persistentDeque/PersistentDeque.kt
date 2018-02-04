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

internal data class DequeLevel(val lhs: Buffer, val rhs: Buffer) {
    val color = minOf(lhs.color, rhs.color)
}

private val emptyLevel = DequeLevel(EmptyBuffer, EmptyBuffer)

internal data class LevelStack(val value: DequeLevel, val next: LevelStack?)

internal data class DequeSubStack(val stack: LevelStack, val next: DequeSubStack?)

class PersistentDeque<T> internal constructor(
        private val topSubStack: LevelStack?, private val next: DequeSubStack?
) {
    init {
        assert(topSubStack != null || next == null)
    }

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
                val level = levelIterator.next()
                size += (level.lhs.size + level.rhs.size) shl depth
                depth += 1
            }
            return size
        }

    val first: T?
        get() {
            val topLevel = this.topSubStack?.value ?: return null
            return (if (topLevel.lhs is EmptyBuffer) topLevel.rhs.first else topLevel.lhs.first) as T
        }

    val last: T?
        get() {
            val topLevel = this.topSubStack?.value ?: return null
            return (if (topLevel.rhs is EmptyBuffer) topLevel.lhs.last else topLevel.rhs.last) as T
        }

    fun addFirst(value: T): PersistentDeque<T> {
        if (this.topSubStack == null) {
            val level = DequeLevel(BufferOfOne(value as Any), EmptyBuffer)
            return PersistentDeque(LevelStack(level, null), null)
        }

        val topLevel = this.topSubStack.value
        val newTopLevel = DequeLevel(topLevel.lhs.addFirst(value as Any), topLevel.rhs)

        return makeDequeRegular(topLevel, newTopLevel)
    }

    fun removeFirst(): PersistentDeque<T> {
        val topLevel = this.topSubStack?.value ?: throw NoSuchElementException()

        val newTopLevel = if (topLevel.lhs is EmptyBuffer) {
            DequeLevel(topLevel.lhs, topLevel.rhs.removeFirst())
        } else {
            DequeLevel(topLevel.lhs.removeFirst(), topLevel.rhs)
        }

        return makeDequeRegular(topLevel, newTopLevel)
    }

    fun addLast(value: T): PersistentDeque<T> {
        if (this.topSubStack == null) {
            val level = DequeLevel(EmptyBuffer, BufferOfOne(value as Any))
            return PersistentDeque(LevelStack(level, null), null)
        }

        val topLevel = this.topSubStack.value
        val newTopLevel = DequeLevel(topLevel.lhs, topLevel.rhs.addLast(value as Any))

        return makeDequeRegular(topLevel, newTopLevel)
    }

    fun removeLast(): PersistentDeque<T> {
        val topLevel = this.topSubStack?.value ?: throw NoSuchElementException()

        val newTopLevel = if (topLevel.rhs is EmptyBuffer) {
            DequeLevel(topLevel.lhs.removeLast(), topLevel.rhs)
        } else {
            DequeLevel(topLevel.lhs, topLevel.rhs.removeLast())
        }

        return makeDequeRegular(topLevel, newTopLevel)
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
            var (lhs, rhs) = levelIterator.next()

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
        val levelCollector = mutableListOf<DequeLevel>()

        var depth = 0

        while (levelIterator.hasNext()) {
            val topLevel = levelIterator.next()
            if (lIndex < topLevel.lhs.size shl depth) {
                var lhs = topLevel.lhs
                val precedingValues = mutableListOf<Any>()

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

                levelCollector.add(DequeLevel(lhs, topLevel.rhs))
                break
            }
            if (rIndex < topLevel.rhs.size shl depth) {
                var rhs = topLevel.rhs
                val succeedingValues = mutableListOf<Any>()

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

                levelCollector.add(DequeLevel(topLevel.lhs, rhs))
                break
            }

            lIndex -= topLevel.lhs.size shl depth
            rIndex -= topLevel.rhs.size shl depth

            depth += 1
            levelCollector.add(topLevel)
        }

        var levelIndex = levelCollector.size - 1
        while (levelIndex >= 0) {
            levelIterator.add(levelCollector[levelIndex--])
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

    private fun get(index: Int, node: Any, depth: Int): T {
        if (depth == 0) {
            return node as T
        }
        val pair = node as Pair<Any, Any>
        val lSize = 1 shl (depth - 1)

        if (index < lSize) {
            return get(index, pair.first, depth - 1)
        }
        return get(index - lSize, pair.second, depth - 1)
    }

    private fun set(index: Int, value: T, node: Any, depth: Int): Any {
        if (depth == 0) {
            return value as Any
        }
        val pair = node as Pair<Any, Any>
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

        var (lhs, rhs) = levelIterator.next()

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

    private fun fillListFromNode(node: Any, depth: Int, list: MutableList<T>) {
        if (depth == 0) {
            list.add(node as T)
        } else {
            val pair = node as Pair<Any, Any>
            fillListFromNode(pair.first, depth - 1, list)
            fillListFromNode(pair.second, depth - 1, list)
        }
    }

    private fun makeDequeRegular(topLevel: DequeLevel, newTopLevel: DequeLevel): PersistentDeque<T> {
        val topSubStackNext = this.topSubStack!!.next
        val next = this.next

        val isTopLevelOnlyLevel = isEmpty(topSubStackNext, next)

        return when(colorChange(topLevel, newTopLevel, isTopLevelOnlyLevel)) {
            GREEN_GREEN,
            YELLOW_GREEN,
            YELLOW_YELLOW -> PersistentDeque(LevelStack(newTopLevel, topSubStackNext), next)

            GREEN_YELLOW -> {
                if (isTopLevelOnlyLevel || next == null
                        || levelColor(next.stack.value, isEmpty(next.stack.next, next.next)) == GREEN) {
                    PersistentDeque(LevelStack(newTopLevel, topSubStackNext), next)
                } else {
                    val levelIterator = LevelIterator(next.stack, next.next)
                    makeGreenTopLevel(levelIterator.next(), levelIterator)
                    levelIterator.addStack(LevelStack(newTopLevel, topSubStackNext))
                    levelIterator.createPersistentDeque()
                }
            }

            YELLOW_RED -> {
                val levelIterator = LevelIterator(topSubStackNext, next)
                makeGreenTopLevel(newTopLevel, levelIterator)
                levelIterator.createPersistentDeque()
            }

            else -> throw IllegalStateException()
        }
    }

    private fun makeGreenTopLevel(topLevel: DequeLevel, levelIterator: LevelIterator) {
        assert(levelColor(topLevel, !levelIterator.hasNext()) == RED)

        if (!levelIterator.hasNext()) {
            if (topLevel.lhs is EmptyBuffer && topLevel.rhs is EmptyBuffer) {
                return
            }
            makeGreenTopLevel(topLevel, emptyLevel, levelIterator)
        } else {
            val nextLevel = levelIterator.next()
            makeGreenTopLevel(topLevel, nextLevel, levelIterator)
        }
    }

    private fun makeGreenTopLevel(topLevel: DequeLevel, nextLevel: DequeLevel, levelIterator: LevelIterator) {
        assert(topLevel.color == RED)

        val isNextLevelBottomLevel = !levelIterator.hasNext()

        val (newTopLevel, newNextLevel) = makeRedLevelGreen(topLevel, nextLevel)

        assert(newTopLevel.color == GREEN || newNextLevel == null)

        if (isNextLevelBottomLevel) {
            if (newNextLevel != null)
                levelIterator.add(newNextLevel)
        }
        else if (newNextLevel == null) {
            makeBottomLevelsRegular(emptyLevel, levelIterator)
        }
        else if (levelColor(newNextLevel, isBottomLevel = false) == RED) {
            makeBottomLevelsRegular(newNextLevel, levelIterator)
        }
        else {
            levelIterator.add(newNextLevel)
        }

        levelIterator.add(newTopLevel)
    }

    private fun makeBottomLevelsRegular(newNextLevel: DequeLevel, levelIterator: LevelIterator) {
        assert(newNextLevel.color == RED && levelIterator.hasNext())

        if (!levelIterator.hasOnlyOneLevel()) {
            levelIterator.add(newNextLevel)
            return
        }

        val nNLevel = levelIterator.next()

        var takeCount = if (newNextLevel.lhs.size < 2) 1 else if (newNextLevel.lhs.size > 3) -1 else 0
        takeCount += if (newNextLevel.rhs.size < 2) 1 else if (newNextLevel.rhs.size > 3) -1 else 0

        if (nNLevel.lhs.size + nNLevel.rhs.size <= takeCount) {
            assert(nNLevel.lhs !is EmptyBuffer || nNLevel.rhs !is EmptyBuffer)

            val (greenNextLevel, newNNLevel) = makeRedLevelGreen(newNextLevel, nNLevel)

            assert(newNNLevel == null)

            levelIterator.add(greenNextLevel)
        } else {
            levelIterator.add(nNLevel)
            levelIterator.add(newNextLevel)
        }
    }

    private fun makeRedLevelGreen(level: DequeLevel, nextLevel: DequeLevel): Pair<DequeLevel, DequeLevel?> {
        assert(level.color == RED)

        var (lhs, rhs) = level
        var (nextLhs, nextRhs) = nextLevel

        if (lhs.size >= 4) {
            nextLhs = moveLastTwoToNextLevelBuffer(nextLhs, lhs)
            lhs = lhs.removeLastTwo()
        }
        if (rhs.size >= 4) {
            nextRhs = moveFirstTwoToNextLevelBuffer(nextRhs, rhs)
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

        val newLevel = DequeLevel(lhs, rhs)

        assert(newLevel.color == GREEN || (nextLhs is EmptyBuffer && nextRhs is EmptyBuffer))

        if (nextLhs is EmptyBuffer && nextRhs is EmptyBuffer) {
            return Pair(newLevel, null)
        }

        val newNextLevel = DequeLevel(nextLhs, nextRhs)

        return Pair(newLevel, newNextLevel)
    }

    private fun moveLastTwoToNextLevelBuffer(nextBuff: Buffer, lhs: Buffer): Buffer {
        return when (lhs) {
            is BufferOfFour -> nextBuff.addFirst(Pair(lhs.e3, lhs.e4))
            is BufferOfFive -> nextBuff.addFirst(Pair(lhs.e4, lhs.e5))
            else -> throw AssertionError("wrong call")
        }
    }

    private fun moveFirstTwoToNextLevelBuffer(nextBuff: Buffer, rhs: Buffer): Buffer {
        return when (rhs) {
            is BufferOfFour -> nextBuff.addLast(Pair(rhs.e1, rhs.e2))
            is BufferOfFive -> nextBuff.addLast(Pair(rhs.e1, rhs.e2))
            else -> throw AssertionError("wrong call")
        }
    }

    private fun moveLastFromNextLevelBufferToRhs(nextBuff: Buffer, rhs: Buffer): Buffer {
        val (e1, e2) = (nextBuff.last as Pair<Any, Any>)
        return rhs.addFirstTwo(e1, e2) // rhs of size 0 or 1
    }

    private fun moveFirstFromNextLevelBufferToLhs(nextBuff: Buffer, lhs: Buffer): Buffer {
        val (e1, e2) = (nextBuff.first as Pair<Any, Any>)
        return lhs.addLastTwo(e1, e2) // lhs of size 0 or 1
    }

    private fun levelColor(level: DequeLevel, isBottomLevel: Boolean): Int {
        return if (isBottomLevel) bottomLevelColor(level) else level.color
    }

    private fun colorChange(oldLevel: DequeLevel, newLevel: DequeLevel, isBottomLevel: Boolean): Int {
        if (isBottomLevel) {
            return colorChange(bottomLevelColor(oldLevel), bottomLevelColor(newLevel))
        }
        return colorChange(oldLevel.color, newLevel.color)
    }

    private fun bottomLevelColor(level: DequeLevel): Int {
        if (level.lhs is EmptyBuffer) return level.rhs.color
        if (level.rhs is EmptyBuffer) return level.lhs.color
        return level.color
    }


    companion object InstanceHolder {
        val emptyDeque = PersistentDeque<Any>(null, null)
    }
}

fun <T> emptyDeque() = PersistentDeque.emptyDeque as PersistentDeque<T>
fun <T> dequeOf(element: T) = emptyDeque<T>().addFirst(element)