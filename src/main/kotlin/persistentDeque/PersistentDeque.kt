package persistentDeque

import buffer.*

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
                return lhs.getLeafValueAt(lIndex, depth) as T
            }
            if (rIndex < rhs.size shl depth) {
                while (rIndex >= 1 shl depth) {
                    rIndex -= 1 shl depth
                    rhs = rhs.pop()
                }
                return rhs.getLeafValueAt((1 shl depth) - rIndex - 1, depth) as T
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
                lhs = lhs.setAt(lIndex, value, depth, true)

                lhsCollector.add(lhs)
                rhsCollector.add(rhs)
                break
            }
            if (rIndex < rhs.size shl depth) {
                rhs = rhs.setAt(rIndex, value, depth, false)

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

    private fun fillListFromStack(levelIterator: LevelIterator,
                                  depth: Int,
                                  list: MutableList<T>) {

        if (!levelIterator.hasNext()) {
            return
        }

        val lhs = levelIterator.topLhs()
        val rhs = levelIterator.topRhs()
        levelIterator.next()

        lhs.fillList(list as MutableList<Any?>, depth, true)
        fillListFromStack(levelIterator, depth + 1, list)
        rhs.fillList(list as MutableList<Any?>, depth, false)
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

        val hasNextLevel = levelIterator.hasNext()

        val nextLevelAllowedPushDeficit = if (hasNextLevel && nonBottomLevelColor(nextLhs, nextRhs) == GREEN) 1 else 0

        if (lhs.size >= YELLOW_HIGH) {
            val nextLhsPushCount = minOf(RED_HIGH - nextLhs.size - nextLevelAllowedPushDeficit, (lhs.size - GREEN_LOW) shr 1)
            if (nextLhsPushCount > 0) {
                nextLhs = lhs.pushPairsOfBottomElementsTo(nextLhs, nextLhsPushCount, isLhs = true)
                lhs = lhs.removeBottom(nextLhsPushCount * 2)
            }
        }

        if (rhs.size >= YELLOW_HIGH) {
            val nextRhsPushCount = minOf(RED_HIGH - nextRhs.size - nextLevelAllowedPushDeficit, (rhs.size - GREEN_LOW) shr 1)
            if (nextRhsPushCount > 0) {
                nextRhs = rhs.pushPairsOfBottomElementsTo(nextRhs, nextRhsPushCount, isLhs = false)
                rhs = rhs.removeBottom(nextRhsPushCount * 2)
            }
        }

        if (lhs.size < GREEN_LOW) {
            val toLeaveNextLhsForRhs = if (rhs.size < GREEN_LOW && !hasNextLevel && nextRhs.size == 0) 1 else 0
            val nextLhsPopCount = minOf(nextLhs.size - nextLevelAllowedPushDeficit - toLeaveNextLhsForRhs, (GREEN_HIGH - lhs.size) shr 1)

            val toLeaveNextRhsForRhs = if (rhs.size < GREEN_LOW && !hasNextLevel) 1 else 0
            val nextRhsRemoveBottomCount = minOf(nextRhs.size - nextLevelAllowedPushDeficit - toLeaveNextRhsForRhs, (GREEN_HIGH - lhs.size) shr 1)

            if (nextLhsPopCount > 0) {
                val topElements = nextLhs.topElementsToPrevLevel(nextLhsPopCount, isLhs = true)
                lhs = lhs.prependSavingOrder(topElements)
                nextLhs = nextLhs.pop(nextLhsPopCount)
            } else if(nextRhsRemoveBottomCount > 0) {
                val bottomElements = nextRhs.bottomElementsToPrevLevel(nextRhsRemoveBottomCount, isLhs = true)
                lhs = lhs.prependSavingOrder(bottomElements)
                nextRhs = nextRhs.removeBottom(nextRhsRemoveBottomCount)
            }
        }

        if (rhs.size < GREEN_LOW) {
            val nextRhsPopCount = minOf(nextRhs.size - nextLevelAllowedPushDeficit, (GREEN_HIGH - rhs.size) shr 1)
            val nextLhsRemoveBottomCount = minOf(nextLhs.size - nextLevelAllowedPushDeficit, (GREEN_HIGH - rhs.size) shr 1)

            if (nextRhsPopCount > 0) {
                val topElements = nextRhs.topElementsToPrevLevel(nextRhsPopCount, isLhs = false)
                rhs = rhs.prependSavingOrder(topElements)
                nextRhs = nextRhs.pop(nextRhsPopCount)
            } else if (nextLhsRemoveBottomCount > 0) {
                val bottomElements = nextLhs.bottomElementsToPrevLevel(nextLhsRemoveBottomCount, isLhs = false)
                rhs = rhs.prependSavingOrder(bottomElements)
                nextLhs = nextLhs.removeBottom(nextLhsRemoveBottomCount)
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

        var takeCount = if (nextLhs.size <= YELLOW_LOW) 1 else if (nextLhs.size >= YELLOW_HIGH) -1 else 0
        takeCount += if (nextRhs.size <= YELLOW_LOW) 1 else if (nextRhs.size >= YELLOW_HIGH) -1 else 0

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