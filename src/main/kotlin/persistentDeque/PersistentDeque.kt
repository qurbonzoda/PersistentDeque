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

private val emptyLevel = DequeLevel(EmptyBuffer, EmptyBuffer)

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
            var topSubStack = stack.peek() ?: return 0
            var stackPop = stack.pop()
            var topLevel = topLevel(topSubStack, stackPop)

            var size = 0
            var depth = 0

            while (topLevel != null) {
                size += (topLevel.lhs.size + topLevel.rhs.size) shl depth
                depth += 1

                val oldTopSubStack = topSubStack
                topSubStack = topSubStackByRemovingTopLevel(oldTopSubStack, stackPop)
                stackPop = stackPopByRemovingTopLevel(oldTopSubStack, stackPop)
                topLevel = topLevel(topSubStack, stackPop)
            }
            return size
        }

    val first: T?
        get() {
            val topLevel = stack.peek()?.peek() ?: return null
            return (if (topLevel.lhs is EmptyBuffer) topLevel.rhs.first else topLevel.lhs.first) as T
        }

    val last: T?
        get() {
            val topLevel = stack.peek()?.peek() ?: return null
            return (if (topLevel.rhs is EmptyBuffer) topLevel.lhs.last else topLevel.rhs.last) as T
        }

    fun addFirst(value: T): PersistentDeque<T> {
        if (stack.isEmpty()) {
            val level = DequeLevel(BufferOfOne(value as Any), EmptyBuffer)
            return PersistentDeque(stack.push(stackOf(level)))
        }

        val topLevel = stack.peek()!!.peek()!!
        val newTopLevel = DequeLevel(topLevel.lhs.addFirst(value as Any), topLevel.rhs)

        return makeDequeRegular(topLevel, newTopLevel)
    }

    fun removeFirst(): PersistentDeque<T> {
        val topLevel = stack.peek()?.peek() ?: throw NoSuchElementException()

        val newTopLevel = if (topLevel.lhs is EmptyBuffer) {
            DequeLevel(topLevel.lhs, topLevel.rhs.removeFirst())
        } else {
            DequeLevel(topLevel.lhs.removeFirst(), topLevel.rhs)
        }

        return makeDequeRegular(topLevel, newTopLevel)
    }

    fun addLast(value: T): PersistentDeque<T> {
        if (stack.isEmpty()) {
            val level = DequeLevel(EmptyBuffer, BufferOfOne(value as Any))
            return PersistentDeque(stack.push(stackOf(level)))
        }

        val topLevel = stack.peek()!!.peek()!!
        val newTopLevel = DequeLevel(topLevel.lhs, topLevel.rhs.addLast(value as Any))

        return makeDequeRegular(topLevel, newTopLevel)
    }

    fun removeLast(): PersistentDeque<T> {
        val topLevel = stack.peek()?.peek() ?: throw NoSuchElementException()

        val newTopLevel = if (topLevel.rhs is EmptyBuffer) {
            DequeLevel(topLevel.lhs.removeLast(), topLevel.rhs)
        } else {
            DequeLevel(topLevel.lhs, topLevel.rhs.removeLast())
        }

        return makeDequeRegular(topLevel, newTopLevel)
    }

    fun toList(): List<T> {
        if (stack.isEmpty()) {
            return emptyList()
        }
        val list = mutableListOf<T>()
        fillListFromStack(stack.peek()!!, stack.pop(), 0, list)
        return list
    }


    private fun fillListFromStack(topSubStack: DequeSubStack,
                                  stackPop: PersistentStack<DequeSubStack>,
                                  depth: Int,
                                  list: MutableList<T>) {

        var (lhs, rhs) = topLevel(topSubStack, stackPop) ?: return

        while (lhs !is EmptyBuffer) {
            fillListFromNode(lhs.first, depth, list)
            lhs = lhs.removeFirst()
        }

        fillListFromStack(topSubStackByRemovingTopLevel(topSubStack, stackPop),
                stackPopByRemovingTopLevel(topSubStack, stackPop), depth + 1, list)

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
                else -> makeGreenNextSubStackTop(stackPop).push(topSubStackPop.push(newTopLevel))
            }

            YELLOW_RED -> makeGreenTopLevel(newTopLevel, topSubStackPop, stackPop)

            else -> throw IllegalStateException()
        }

        return PersistentDeque(newStack)
    }

    private fun makeGreenNextSubStackTop(stackPop: PersistentStack<DequeSubStack>): PersistentStack<DequeSubStack> {
        val nextSubStackTop = stackPop.peek()?.peek()
        return when (nextSubStackTop?.let { levelColor(it, stackPop.pop().isEmpty() && stackPop.peek()!!.pop().isEmpty()) }) {
            null,
            GREEN -> stackPop

            RED -> {
                val nextSubStackPop = stackPop.peek()!!.pop()
                val stackPopPop = stackPop.pop()
                makeGreenTopLevel(nextSubStackTop, nextSubStackPop, stackPopPop)
            }

            else -> throw IllegalStateException()
        }
    }

    private fun makeGreenTopLevel(topLevel: DequeLevel,
                                  topSubStackPop: DequeSubStack,
                                  stackPop: PersistentStack<DequeSubStack>): PersistentStack<DequeSubStack> {
        assert(levelColor(topLevel, topSubStackPop.isEmpty() && stackPop.isEmpty()) == RED)

        val nextLevel = topLevel(topSubStackPop, stackPop)

        val newStack = if (nextLevel == null) {
            if (topLevel.lhs is EmptyBuffer && topLevel.rhs is EmptyBuffer) {
                return emptyStack()
            }

            makeGreenTopLevel(topLevel, emptyLevel, emptyStack(), emptyStack())
        } else {
            val subStack = topSubStackByRemovingTopLevel(topSubStackPop, stackPop)
            val stack = stackPopByRemovingTopLevel(topSubStackPop, stackPop)

            makeGreenTopLevel(topLevel, nextLevel, subStack, stack)
        }

        checkInvariants(newStack)

        return newStack
    }

    private fun makeGreenTopLevel(topLevel: DequeLevel,
                                  nextLevel: DequeLevel,
                                  subStack: DequeSubStack,
                                  stack: PersistentStack<DequeSubStack>): PersistentStack<DequeSubStack> {

        assert(subStack.isEmpty()
                || levelColor(subStack.peek()!!, subStack.pop().isEmpty() && stack.isEmpty()) == YELLOW)

        assert(stack.isEmpty()
                || levelColor(stack.peek()!!.peek()!!, stack.peek()!!.pop().isEmpty() && stack.pop().isEmpty()) != YELLOW)

        val isNextLevelBottomLevel = subStack.isEmpty() && stack.isEmpty()

        val (newTopLevel, newNextLevel) = makeRedLevelGreen(topLevel, nextLevel)

        assert(newTopLevel.color == GREEN || newNextLevel == null)

        if (newNextLevel == null) {
            return if (isNextLevelBottomLevel) {
                stackOf( stackOf(newTopLevel) )
            } else {
                makeBottomLevelsRegular(newTopLevel, emptyLevel, subStack, stack)
            }
        } else {
            // newTopLevel is GREEN
            return if (levelColor(newNextLevel, isNextLevelBottomLevel) == YELLOW) {
                stack.push( subStack.push(newNextLevel).push(newTopLevel) )
            } else if (isNextLevelBottomLevel || levelColor(newNextLevel, isBottomLevel = false) == GREEN) {
                stack.push( subStack.push(newNextLevel) )
                        .push( stackOf(newTopLevel) )
            } else {
                assert(levelColor(newNextLevel, isBottomLevel = false) == RED)
                makeBottomLevelsRegular(newTopLevel, newNextLevel, subStack, stack)
            }
        }
    }

    private fun makeBottomLevelsRegular(newTopLevel: DequeLevel,
                                        newNextLevel: DequeLevel,
                                        topSubStackPopPop: DequeSubStack,
                                        stackPop: PersistentStack<DequeSubStack>): PersistentStack<DequeSubStack> {

        val nNLevel = topLevel(topSubStackPopPop, stackPop)!!
        val nNNLevel = topLevel(topSubStackByRemovingTopLevel(topSubStackPopPop, stackPop),
                stackPopByRemovingTopLevel(topSubStackPopPop, stackPop))

        return if (nNNLevel == null) {
            val (greenNextLevel, newNNLevel)= makeRedLevelGreen(newNextLevel, nNLevel)
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
    }

    private fun makeRedLevelGreen(level: DequeLevel, nextLevel: DequeLevel): Pair<DequeLevel, DequeLevel?> {
        assert(level.color == RED)

        var (lhs, rhs) = level
        var (nextLhs, nextRhs) = nextLevel

        if (lhs.size >= 4) {
            nextLhs = moveLastTwoToNextLevelBuffer(nextLhs, lhs)
            lhs = lhs.removeLast().removeLast()
        }
        if (rhs.size >= 4) {
            nextRhs = moveFirstTwoToNextLevelBuffer(nextRhs, rhs)
            rhs = rhs.removeFirst().removeFirst()
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
        return when (rhs) {
            is EmptyBuffer -> {
                BufferOfTwo(e1, e2)
            }
            is BufferOfOne -> {
                BufferOfThree(e1, e2, rhs.e)
            }
            else -> throw AssertionError("wrong call")
        }
    }

    private fun moveFirstFromNextLevelBufferToLhs(nextBuff: Buffer, lhs: Buffer): Buffer {
        val (e1, e2) = (nextBuff.first as Pair<Any, Any>)
        return when (lhs) {
            is EmptyBuffer -> {
                BufferOfTwo(e1, e2)
            }
            is BufferOfOne -> {
                BufferOfThree(lhs.e, e1, e2)
            }
            else -> throw AssertionError("wrong call")
        }
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

    private fun topLevel(topSubStack: DequeSubStack,
                         stackPop: PersistentStack<DequeSubStack>): DequeLevel? {
        if (topSubStack.isEmpty()) return stackPop.peek()?.peek()
        return topSubStack.peek()
    }

    private fun topSubStackByRemovingTopLevel(topSubStack: DequeSubStack,
                                              stackPop: PersistentStack<DequeSubStack>): DequeSubStack {
        if (topSubStack.isEmpty()) return stackPop.peek()!!.pop()
        return topSubStack.pop()
    }

    private fun stackPopByRemovingTopLevel(topSubStack: DequeSubStack,
                                           stackPop: PersistentStack<DequeSubStack>): PersistentStack<DequeSubStack> {
        if (topSubStack.isEmpty()) return stackPop.pop()
        return stackPop
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