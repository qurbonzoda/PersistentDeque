package persistentDeque

import buffer.*
import persistentStack.PersistentStack
import persistentStack.emptyStack
import persistentStack.stackOf

private const val RED_RED = 0
private const val RED_YELLOW = 1
private const val RED_GREEN = 2
private const val YELLOW_RED = 3
private const val YELLOW_YELLOW = 4
private const val YELLOW_GREEN = 5
private const val GREEN_RED = 6
private const val GREEN_YELLOW = 7
private const val GREEN_GREEN = 8

private fun colorChange(oldColor: Int, newColor: Int): Int {
    return oldColor * 3 + newColor
}

private data class DequeLevel(val lhs: Buffer, val rhs: Buffer) {
    val color = minOf(lhs.color, rhs.color)
}

private typealias DequeSubStack = PersistentStack<DequeLevel>

class PersistentDeque<T> private constructor(
        private val stack: PersistentStack<DequeSubStack>
) {

//    fun contains(element: T): Boolean
//    fun containsAll(elements: Collection<T>): Boolean

//    fun get(index: Int): T
//    fun set(index: Int, element: E)
//    fun indexOf(element: T): Int
//    fun lastIndexOf(element: T): Int

//    fun iterator(): Iterator<T>
//    fun listIterator(): ListIterator<T>
//    fun listIterator(index: Int): ListIterator<T>

    fun isEmpty(): Boolean {
        return stack.isEmpty()
    }

    val size: Int
        get() {
            if (stack.isEmpty()) return 0

            var size = 0
            var depth = 0
            var level = nextLevel(stack.peek()!!, stack.pop())

            while (level != null) {
                size += (level.first.lhs.size shl depth) + (level.first.rhs.size shl depth)
                depth += 1
                level = nextLevel(level.second, level.third)
            }
            return size
        }

    val first: T?
        get() {
            if (stack.isEmpty()) {
                return null
            }
            val topLevel = stack.peek()!!.peek()!!
            return (if (topLevel.lhs is EmptyBuffer) topLevel.rhs.first else topLevel.lhs.first) as T
        }

    val last: T?
        get() {
            if (stack.isEmpty()) {
                return null
            }
            val topLevel = stack.peek()!!.peek()!!
            return (if (topLevel.rhs is EmptyBuffer) topLevel.lhs.last else topLevel.rhs.last) as T
        }

    fun addFirst(value: T): PersistentDeque<T> {
        if (stack.isEmpty()) {
            val level = DequeLevel(BufferOfOne(value as Any), EmptyBuffer())
            return PersistentDeque(stack.push(stackOf(level)))
        }

        val topLevel = stack.peek()!!.peek()!!
        val newTopLevel = DequeLevel(topLevel.lhs.addFirst(value as Any), topLevel.rhs)

        return makeDequeRegular(topLevel, newTopLevel)
    }

    fun removeFirst(): PersistentDeque<T> {
        if (stack.isEmpty()) {
            throw NoSuchElementException()
        }

        val topLevel = stack.peek()!!.peek()!!

        val newTopLevel: DequeLevel
        if (topLevel.lhs is EmptyBuffer) {
            newTopLevel = DequeLevel(topLevel.lhs, topLevel.rhs.removeFirst())
        } else {
            newTopLevel = DequeLevel(topLevel.lhs.removeFirst(), topLevel.rhs)
        }

        return makeDequeRegular(topLevel, newTopLevel)
    }

    fun addLast(value: T): PersistentDeque<T> {
        if (stack.isEmpty()) {
            val level = DequeLevel(EmptyBuffer(), BufferOfOne(value as Any))
            return PersistentDeque(stack.push(stackOf(level)))
        }

        val topLevel = stack.peek()!!.peek()!!
        val newTopLevel = DequeLevel(topLevel.lhs, topLevel.rhs.addLast(value as Any))

        return makeDequeRegular(topLevel, newTopLevel)
    }

    fun removeLast(): PersistentDeque<T> {
        if (stack.isEmpty()) {
            throw NoSuchElementException()
        }

        val topLevel = stack.peek()!!.peek()!!

        val newTopLevel: DequeLevel
        if (topLevel.rhs is EmptyBuffer) {
            newTopLevel = DequeLevel(topLevel.lhs.removeLast(), topLevel.rhs)
        } else {
            newTopLevel = DequeLevel(topLevel.lhs, topLevel.rhs.removeLast())
        }

        return makeDequeRegular(topLevel, newTopLevel)
    }

    fun toList(): List<T> {
        val list = mutableListOf<T>()
        fillListFromStack(stack, 0, list)
        return list
    }


    private fun fillListFromStack(stack: PersistentStack<DequeSubStack>, depth: Int, list: MutableList<T>) {
        if (stack.isEmpty()) return

        val level = nextLevel(stack.peek()!!, stack.pop()) ?: return
        var (lhs, rhs) = level.first

        while (lhs !is EmptyBuffer) {
            fillListFromNode(lhs.first, depth, list)
            lhs = lhs.removeFirst()
        }

        fillListFromStack(level.third.push(level.second), depth + 1, list)

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
        val topSubStackPop = stack.peek()!!.pop()

        val stackPop = stack.pop()

        val isTopLevelOnlyLevel = stackPop.isEmpty() && topSubStackPop.isEmpty()

        val newStack = when(colorChange(topLevel, newTopLevel, isTopLevelOnlyLevel)) {
            GREEN_GREEN,
            YELLOW_GREEN,
            YELLOW_YELLOW -> stackPop.push( topSubStackPop.push(newTopLevel) )

            GREEN_YELLOW -> when {
                isTopLevelOnlyLevel -> stackPop.push( topSubStackPop.push(newTopLevel) )
                else -> {
                    val nextSubStackTop = stackPop.peek()?.peek()
                    when (nextSubStackTop?.let { levelColor(it, stackPop.pop().isEmpty() && stackPop.peek()!!.pop().isEmpty()) }) {
                        null,
                        GREEN -> stackPop.push(topSubStackPop.push(newTopLevel))

                        RED -> {
                            val nextSubStackPop = stackPop.peek()!!.pop()
                            val stackPopPop = stackPop.pop()
                            makeGreenTopLevel(nextSubStackTop, nextSubStackPop, stackPopPop, topSubStackPop.push(newTopLevel))
                        }

                        else -> throw IllegalStateException()
                    }
                }
            }

            YELLOW_RED -> makeGreenTopLevel(newTopLevel, topSubStackPop, stackPop, null)

            else -> throw IllegalStateException()
        }

        return PersistentDeque(newStack)
    }

    private fun makeGreenTopLevel(topLevel: DequeLevel,
                                  topSubStackPop: DequeSubStack,
                                  stackPop: PersistentStack<DequeSubStack>,
                                  subStackToPushLast: DequeSubStack?): PersistentStack<DequeSubStack> {
        assert(levelColor(topLevel, topSubStackPop.isEmpty() && stackPop.isEmpty()) == RED)
        assert(subStackToPushLast == null || subStackToPushLast.peek()!!.color == YELLOW)

        val newStack: PersistentStack<DequeSubStack>

        if (topSubStackPop.isEmpty() && stackPop.isEmpty()) { // bottom level
            if (topLevel.lhs is EmptyBuffer && topLevel.rhs is EmptyBuffer) {
                newStack = emptyStack()
            } else {
                val (newTopLevel, nextLevel) = makeRedLevelGreen(topLevel, DequeLevel(EmptyBuffer(), EmptyBuffer()))
                assert(newTopLevel.color == GREEN)

                if (nextLevel != null) {
                    newStack = if (bottomLevelColor(nextLevel) == YELLOW) {
                        stackOf( stackOf(nextLevel).push(newTopLevel) )
                    } else {
                        assert(bottomLevelColor(nextLevel) == GREEN)
                        stackOf( stackOf(nextLevel) ).push( stackOf(newTopLevel) )
                    }
                } else {
                    newStack = stackOf( stackOf(newTopLevel) )
                }
            }
        } else if (topSubStackPop.isEmpty()) {
            assert(!stackPop.isEmpty())

            val nextSubStack = stackPop.peek()!!
            val nextTopLevel = nextSubStack.peek()!!

            val stackPopPop = stackPop.pop()
            val nextSubStackPop = nextSubStack.pop()

            val isNextTopLevelBottomLevel = nextSubStackPop.isEmpty() && stackPopPop.isEmpty()

            assert(levelColor(nextTopLevel, isNextTopLevelBottomLevel) == GREEN)

            val (newTopLevel, newNextTopLevel) = makeRedLevelGreen(topLevel, nextTopLevel)

            assert(newTopLevel.color == GREEN)

            newStack = when {
                newNextTopLevel == null -> {
                    assert(isNextTopLevelBottomLevel)
                    stackOf( stackOf(newTopLevel) )
                }
                levelColor(newNextTopLevel, isNextTopLevelBottomLevel) == GREEN -> {
                    val newNextSubStack = nextSubStackPop.push(newNextTopLevel)
                    val newTopSubStack = stackOf(newTopLevel)
                    stackPopPop.push(newNextSubStack).push(newTopSubStack)
                }
                else -> {
                    assert(levelColor(newNextTopLevel, isNextTopLevelBottomLevel) == YELLOW)

                    val newTopSubStack = nextSubStackPop.push(newNextTopLevel).push(newTopLevel)
                    stackPopPop.push(newTopSubStack)
                }
            }
        } else {
            assert(!topSubStackPop.isEmpty())

            val nextLevel = topSubStackPop.peek()!!
            val topSubStackPopPop = topSubStackPop.pop()

            val isNextLevelBottomLevel = topSubStackPopPop.isEmpty() && stackPop.isEmpty()

            assert(levelColor(nextLevel, isNextLevelBottomLevel) == YELLOW)

            val (newTopLevel, newNextLevel) = makeRedLevelGreen(topLevel, nextLevel)

            assert(newTopLevel.color == GREEN || isNextLevelBottomLevel)

            newStack = when {
                newNextLevel == null -> {
                    if (isNextLevelBottomLevel) {
                        stackPop.push( stackOf(newTopLevel) )
                    } else {
                        val emptyLevel = DequeLevel(EmptyBuffer(), EmptyBuffer())
                        val nNLevel = nextLevel(topSubStackPopPop, stackPop)!!
                        val nNNLevel = nextLevel(nNLevel.second, nNLevel.third)

                        if (nNNLevel == null) {
                            val (greenNextLevel, newNNLevel)= makeRedLevelGreen(emptyLevel, nNLevel.first)
                            if (newNNLevel == null) {
                                if (bottomLevelColor(greenNextLevel) == YELLOW) {
                                    stackOf( stackOf(greenNextLevel).push(newTopLevel) )
                                } else {
                                    stackOf( stackOf(greenNextLevel) ).push( stackOf(newTopLevel) )
                                }
                            } else {
                                stackPop.push(topSubStackPopPop.push(emptyLevel))
                                        .push(stackOf(newTopLevel))
                            }
                        } else {
                            stackPop.push(topSubStackPopPop.push(emptyLevel))
                                    .push(stackOf(newTopLevel))
                        }
                    }
                }
                levelColor(newNextLevel, isNextLevelBottomLevel) == YELLOW -> {
                    val newTopSubStack = topSubStackPopPop.push(newNextLevel).push(newTopLevel)
                    stackPop.push(newTopSubStack)
                }
                else -> {
                    assert(levelColor(newNextLevel, isNextLevelBottomLevel) == RED
                            || levelColor(newNextLevel, isNextLevelBottomLevel) == GREEN)

                    if (isNextLevelBottomLevel) {
                        stackPop.push( stackOf(newNextLevel) )
                                .push( stackOf(newTopLevel) )
                    } else if (levelColor(newNextLevel, false) == RED) {

                        val nNLevel = nextLevel(topSubStackPopPop, stackPop)!!
                        val nNNLevel = nextLevel(nNLevel.second, nNLevel.third)

                        if (nNNLevel == null) {
                            val (greenNextLevel, newNNLevel)= makeRedLevelGreen(newNextLevel, nNLevel.first)
                            if (newNNLevel == null) {
                                if (bottomLevelColor(greenNextLevel) == YELLOW) {
                                    stackOf( stackOf(greenNextLevel).push(newTopLevel) )
                                } else {
                                    stackOf( stackOf(greenNextLevel) ).push( stackOf(newTopLevel) )
                                }
                            } else {
                                stackPop.push(topSubStackPopPop.push(newNextLevel))
                                        .push(stackOf(newTopLevel))
                            }
                        } else {
                            stackPop.push(topSubStackPopPop.push(newNextLevel))
                                    .push(stackOf(newTopLevel))
                        }
                    } else {
                        stackPop.push(topSubStackPopPop.push(newNextLevel))
                                .push(stackOf(newTopLevel))
                    }
                }
            }
        }

        checkInvariants(subStackToPushLast?.let { newStack.push(it) } ?: newStack)

        return subStackToPushLast?.let { newStack.push(it) } ?: newStack
    }

    private fun makeRedLevelGreen(level: DequeLevel, nextLevel: DequeLevel): Pair<DequeLevel, DequeLevel?> {
        assert(level.color == RED)

        var (lhs, rhs) = level
        var (nextLhs, nextRhs) = nextLevel

        fun moveOneFromNextLhsToNextRhs() {
            assert(nextRhs is EmptyBuffer)

            nextRhs = BufferOfOne(nextLhs.last)
            nextLhs = nextLhs.removeLast()
        }
        fun moveOneFromNextRhsToNextLhs() {
            assert(nextLhs is EmptyBuffer)

            nextLhs = BufferOfOne(nextRhs.first)
            nextRhs = nextRhs.removeFirst()
        }
        fun moveTwoFromLhsToNextLhs() {
            val xlhs = lhs
            if (xlhs is BufferOfFour) {
                nextLhs = nextLhs.addFirst(Pair(xlhs.e3, xlhs.e4))
                lhs = lhs.removeLast().removeLast()
            } else if (xlhs is BufferOfFive) {
                nextLhs = nextLhs.addFirst(Pair(xlhs.e4, xlhs.e5))
                lhs = lhs.removeLast().removeLast()
            } else {
                throw AssertionError("wrong call")
            }
        }
        fun moveTwoFromRhsToNextRhs() {
            val xrhs = rhs
            if (xrhs is BufferOfFour) {
                nextRhs = nextRhs.addLast(Pair(xrhs.e1, xrhs.e2))
                rhs = rhs.removeFirst().removeFirst()
            } else if (xrhs is BufferOfFive) {
                nextRhs = nextRhs.addLast(Pair(xrhs.e1, xrhs.e2))
                rhs = rhs.removeFirst().removeFirst()
            } else {
                throw AssertionError("wrong call")
            }
        }
        fun moveOneFromNextLhsToLhs() {
            val xlhs = lhs
            if (xlhs is EmptyBuffer) {
                val (e1, e2) = (nextLhs.first as Pair<Any, Any>)
                lhs = BufferOfTwo(e1, e2)
                nextLhs = nextLhs.removeFirst()
            } else if (xlhs is BufferOfOne) {
                val (e2, e3) = (nextLhs.first as Pair<Any, Any>)
                lhs = BufferOfThree(xlhs.e, e2, e3)
                nextLhs = nextLhs.removeFirst()
            } else {
                throw AssertionError("wrong call")
            }
        }
        fun moveOneFromNextRhsToRhs() {
            val xrhs = rhs
            if (xrhs is EmptyBuffer) {
                val (e1, e2) = (nextRhs.last as Pair<Any, Any>)
                rhs = BufferOfTwo(e1, e2)
                nextRhs = nextRhs.removeLast()
            } else if (xrhs is BufferOfOne) {
                val (e1, e2) = (nextRhs.last as Pair<Any, Any>)
                rhs = BufferOfThree(e1, e2, xrhs.e)
                nextRhs = nextRhs.removeLast()
            } else {
                throw AssertionError("wrong call")
            }
        }
        fun moveTwoFromRhsToNextLhs() {
            assert(nextRhs is EmptyBuffer)

            val xrhs = rhs
            if (xrhs is BufferOfFour) {
                nextLhs = nextLhs.addLast(Pair(xrhs.e1, xrhs.e2))
                rhs = rhs.removeFirst().removeFirst()
            } else if (xrhs is BufferOfFive) {
                nextLhs = nextLhs.addLast(Pair(xrhs.e1, xrhs.e2))
                rhs = rhs.removeFirst().removeFirst()
            } else {
                throw AssertionError("wrong call")
            }
        }
        fun moveOneFromNextLhsToRhs() {
            assert(nextRhs is EmptyBuffer)

            val xrhs = rhs
            if (xrhs is EmptyBuffer) {
                val (e1, e2) = (nextLhs.last as Pair<Any, Any>)
                rhs = BufferOfTwo(e1, e2)
                nextLhs = nextLhs.removeLast()
            } else if (xrhs is BufferOfOne) {
                val (e1, e2) = (nextLhs.last as Pair<Any, Any>)
                rhs = BufferOfThree(e1, e2, xrhs.e)
                nextLhs = nextLhs.removeLast()
            } else {
                throw AssertionError("wrong call")
            }
        }

        if (nextLhs.size + nextRhs.size >= 2) {
            if (nextLhs is EmptyBuffer) {
                moveOneFromNextRhsToNextLhs()
            }
            if (nextRhs is EmptyBuffer) {
                moveOneFromNextLhsToNextRhs()
            }

            if (lhs.size >= 4) {
                moveTwoFromLhsToNextLhs()
            }
            if (rhs.size >= 4) {
                moveTwoFromRhsToNextRhs()
            }

            if (lhs.size <= 1) {
                moveOneFromNextLhsToLhs()
            }
            if (rhs.size <= 1) {
                moveOneFromNextRhsToRhs()
            }
        } else if (nextLhs.size + nextRhs.size <= 1 && (lhs.size >= 2 || rhs.size >= 2)) { // nextLevel is bottom level
            if (nextLhs is EmptyBuffer) {
                nextLhs = nextRhs
                nextRhs = EmptyBuffer()
            }
            if (lhs.size >= 4) {
                moveTwoFromLhsToNextLhs()
            }
            if (rhs.size >= 4) {
                moveTwoFromRhsToNextLhs()
            }

            if (lhs.size <= 1) {
                moveOneFromNextLhsToLhs()
            }
            if (rhs.size <= 1) {
                moveOneFromNextLhsToRhs()
            }
        } else if (nextLhs.size + nextRhs.size <= 1 && lhs.size <= 1 && rhs.size <= 1) {
            if (nextLhs is EmptyBuffer) {
                nextLhs = nextRhs
                nextRhs = EmptyBuffer()
            }
            moveOneFromNextLhsToLhs()
        }

        val newLevel = DequeLevel(lhs, rhs)
        val newNextLevel = DequeLevel(nextLhs, nextRhs)

        assert(newLevel.color == GREEN || (nextLhs is EmptyBuffer && nextRhs is EmptyBuffer))

        if (newNextLevel.lhs is EmptyBuffer && newNextLevel.rhs is EmptyBuffer) {
            return Pair(newLevel, null)
        }

        return Pair(newLevel, newNextLevel)
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

    private fun nextLevel(
            topSubStack: DequeSubStack,
            stackPop: PersistentStack<DequeSubStack>
    ): Triple<DequeLevel, DequeSubStack, PersistentStack<DequeSubStack>>? {

        if (topSubStack.isEmpty()) {
            return if (stackPop.isEmpty()) {
                null
            } else {
                Triple(
                        stackPop.peek()!!.peek()!!,
                        stackPop.peek()!!.pop(),
                        stackPop.pop()
                )
            }
        } else {
            return Triple(
                    topSubStack.peek()!!,
                    topSubStack.pop(),
                    stackPop
            )
        }
    }

    private fun checkInvariants(stack: PersistentStack<DequeSubStack>) {
        var result = stack
        var isCurrentLevelTopLevel = true

        while (!result.isEmpty() && !result.pop().isEmpty()) {
            val currentLevel = result.peek()!!.peek()!!

            val isNextTopLevelBottomLevel = result.pop().pop().isEmpty() && result.pop().peek()!!.pop().isEmpty()
            val nextTopLevel = result.pop().peek()!!.peek()!!

            if (isCurrentLevelTopLevel && currentLevel.color == RED) {
                throw IllegalStateException()
            }
            if (isCurrentLevelTopLevel
                    && currentLevel.color == YELLOW
                    && levelColor(nextTopLevel, isNextTopLevelBottomLevel) != GREEN) {
                throw IllegalStateException()
            }
            if (levelColor(nextTopLevel, isNextTopLevelBottomLevel) == YELLOW) {
                throw IllegalStateException()
            }
            if (!isCurrentLevelTopLevel
                    && currentLevel.color != GREEN
                    && levelColor(nextTopLevel, isNextTopLevelBottomLevel) != GREEN) {
                throw IllegalStateException()
            }

            if (levelColor(nextTopLevel, isNextTopLevelBottomLevel) != GREEN
                    && levelColor(nextTopLevel, isNextTopLevelBottomLevel) != RED) {
                throw IllegalStateException()
            }

            isCurrentLevelTopLevel = false
            result = result.pop()
        }
    }


    companion object InstanceHolder {
        val emptyDeque = PersistentDeque<Any>(emptyStack())
    }
}

fun <T> emptyDeque() = PersistentDeque.emptyDeque as PersistentDeque<T>
fun <T> dequeOf(element: T) = emptyDeque<T>().addFirst(element)