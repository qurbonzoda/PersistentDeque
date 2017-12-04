package experimental

import buffer.*
import persistentStack.*

private data class DequeLevel(val lhs: Buffer, val rhs: Buffer) {
    val color = minOf(lhs.color, rhs.color)
}

private typealias DequeSubStack = PersistentStack<DequeLevel>

class PersistentDeque<T> private constructor(
        private val stack: PersistentStack<DequeSubStack>
) {
    constructor(): this(emptyStack())

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

    private fun makeDequeRegular(topLevel: DequeLevel, newTopLevel: DequeLevel): PersistentDeque<T> {
        val topSubStackPop = stack.peek()!!.pop()

        val stackPop = stack.pop()

        val isTopLevelOnlyLevel = stackPop.isEmpty() && topSubStackPop.isEmpty()
//
//        if (newTopLevel.lhs is buffer.EmptyBuffer && newTopLevel.rhs is buffer.EmptyBuffer) {
//            assert(isTopLevelOnlyLevel)
//            return persistentDeque.persistentDeque(stackPop)
//        }

        val newStack = when(Pair(levelColor(topLevel, isTopLevelOnlyLevel), levelColor(newTopLevel, isTopLevelOnlyLevel))) {
            Pair(GREEN, GREEN),
            Pair(YELLOW, GREEN),
            Pair(YELLOW, YELLOW) -> stackPop.push( topSubStackPop.push(newTopLevel) )

            Pair(GREEN, YELLOW) -> when {
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

            Pair(YELLOW, RED) -> makeGreenTopLevel(newTopLevel, topSubStackPop, stackPop, null)

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
            newStack = if (topLevel.lhs is EmptyBuffer && topLevel.rhs is EmptyBuffer) {
                stackPop
            } else {
                val (newTopLevel, nextLevel) = makeGreenRedBottomLevel(topLevel)
                assert(newTopLevel.color == GREEN)

                val newTopSubStack = (if (nextLevel != null) topSubStackPop.push(nextLevel) else topSubStackPop).push(newTopLevel)
                stackPop.push(newTopSubStack)
            }
        } else if (topSubStackPop.isEmpty()) {
            assert(!stackPop.isEmpty())

            val nextSubStack = stackPop.peek()!!
            val nextTopLevel = nextSubStack.peek()!!

            val stackPopPop = stackPop.pop()
            val nextSubStackPop = nextSubStack.pop()

            val isNextTopLevelBottomLevel = nextSubStackPop.isEmpty() && stackPopPop.isEmpty()

            if (levelColor(nextTopLevel, isNextTopLevelBottomLevel) != GREEN) {
                println()
            }

            assert(levelColor(nextTopLevel, isNextTopLevelBottomLevel) == GREEN)

            val (newTopLevel, newNextTopLevel) = makeRedLevelGreen(topLevel, nextTopLevel)

            assert(newTopLevel.color == GREEN)

            newStack = when {
                newNextTopLevel == null -> {
                    assert(isNextTopLevelBottomLevel)
                    val newTopSubStack = topSubStackPop.push(newTopLevel)
                    stackPopPop.push(newTopSubStack)
                }
                levelColor(newNextTopLevel, isNextTopLevelBottomLevel) == GREEN -> {
                    val newNextSubStack = nextSubStackPop.push(newNextTopLevel)
                    val newTopSubStack = topSubStackPop.push(newTopLevel)
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

            assert(newTopLevel.color == GREEN
                    || stackPop.peek() == null
                    || stackPop.peek()?.peek()?.color == GREEN)

            newStack = when {
                newNextLevel == null -> {
                    assert(topSubStackPopPop.isEmpty())
                    stackPop.push(topSubStackPopPop.push(newTopLevel))
                }
                levelColor(newNextLevel, isNextLevelBottomLevel) == YELLOW -> {
                    val newTopSubStack = topSubStackPopPop.push(newNextLevel).push(newTopLevel)
                    stackPop.push(newTopSubStack)
                }
                else -> {
                    assert(levelColor(newNextLevel, isNextLevelBottomLevel) == RED
                            || levelColor(newNextLevel, isNextLevelBottomLevel) == GREEN)
                    val newNextSubStack = topSubStackPopPop.push(newNextLevel)
                    stackPop.push(newNextSubStack).push(stackOf(newTopLevel))
                }
            }
        }

        var result = subStackToPushLast?.let { newStack.push(it) } ?: newStack

        if (!result.isEmpty() && !result.pop().isEmpty()) {
            val topSubStack = result.peek()!!
            val nextSubStack = result.pop().peek()!!

            if (result.pop().pop().isEmpty() && topSubStack.pop().isEmpty() && !nextSubStack.pop().isEmpty() && nextSubStack.pop().pop().isEmpty()) {
                val topLevel = result.peek()!!.peek()!!
                val nextTopLevel = result.pop().peek()!!.peek()!!
                val nextTopLevelNextLevel = result.pop().peek()!!.pop().peek()!!

                if (topLevel.lhs is BufferOfTwo && topLevel.rhs is BufferOfThree) {
                    if (nextTopLevel.lhs is EmptyBuffer && nextTopLevel.rhs is BufferOfOne) {
                        if (nextTopLevelNextLevel.lhs is BufferOfOne && nextTopLevelNextLevel.rhs is EmptyBuffer) {
                            println()
                        }
                    }
                }
            }
        }



        var isCurrentLevelTopLevel = true

        while (!result.isEmpty() && !result.pop().isEmpty()) {
            val currentLevel = result.peek()!!.peek()!!

            val isNextTopLevelBottomLevel = result.pop().pop().isEmpty() && result.pop().peek()!!.pop().isEmpty()
            val nextTopLevel = result.pop().peek()!!.peek()!!


            if (isCurrentLevelTopLevel && currentLevel.color == RED) {
                println()
            }
            if (isCurrentLevelTopLevel
                    && currentLevel.color == YELLOW
                    && levelColor(nextTopLevel, isNextTopLevelBottomLevel) != GREEN) {
                println()
            }
            if (levelColor(nextTopLevel, isNextTopLevelBottomLevel) == YELLOW) {
                println()
            }
            if (!isCurrentLevelTopLevel
                    && currentLevel.color != GREEN
                    && levelColor(nextTopLevel, isNextTopLevelBottomLevel) != GREEN) {
                println()
            }

            if (levelColor(nextTopLevel, isNextTopLevelBottomLevel) != GREEN
                    && levelColor(nextTopLevel, isNextTopLevelBottomLevel) != RED) {
                println()
            }

            isCurrentLevelTopLevel = false
            result = result.pop()
        }

        return subStackToPushLast?.let { newStack.push(it) } ?: newStack
    }

    private fun makeGreenRedBottomLevel(level: DequeLevel): Pair<DequeLevel, DequeLevel?> {
        assert(level.color == RED)
        assert(level.lhs !is EmptyBuffer || level.rhs !is EmptyBuffer)
        assert(!((level.lhs is EmptyBuffer && level.rhs !is BufferOfFive)
                || (level.rhs is EmptyBuffer && level.lhs !is BufferOfFive)))

        val newLevel: DequeLevel
        val nextLevel: DequeLevel?

        when(level.lhs) {
            is EmptyBuffer -> when(level.rhs) {
                is BufferOfFive -> {
                    newLevel = DequeLevel(
                            BufferOfTwo(level.rhs.e1, level.rhs.e2),
                            BufferOfThree(level.rhs.e3, level.rhs.e4, level.rhs.e5)
                    )
                    nextLevel = null
                }
                else -> throw AssertionError("Not red actually")
            }
            is BufferOfOne -> when(level.rhs) {
                is BufferOfFive -> {
                    newLevel = DequeLevel(
                            BufferOfThree(level.lhs.e, level.rhs.e1, level.rhs.e2),
                            BufferOfThree(level.rhs.e3, level.rhs.e4, level.rhs.e5)
                    )
                    nextLevel = null
                }
                else -> throw AssertionError("Not red actually")
            }
            is BufferOfTwo,
            is BufferOfThree -> when(level.rhs) {
                is BufferOfFive -> {
                    newLevel = DequeLevel(
                            lhs = level.lhs,
                            rhs = BufferOfThree(level.rhs.e3, level.rhs.e4, level.rhs.e5)
                    )
                    nextLevel = DequeLevel(
                            lhs = EmptyBuffer(),
                            rhs = BufferOfOne(Pair(level.rhs.e1, level.rhs.e2))
                    )
                }
                else -> throw AssertionError("Not red actually")
            }
            is BufferOfFour -> when(level.rhs) {
                is BufferOfFive -> {
                    newLevel = DequeLevel(
                            lhs = BufferOfTwo(level.lhs.e1, level.lhs.e2),
                            rhs = BufferOfThree(level.rhs.e3, level.rhs.e4, level.rhs.e5)
                    )
                    nextLevel = DequeLevel(
                            lhs = BufferOfOne(Pair(level.lhs.e3, level.lhs.e4)),
                            rhs = BufferOfOne(Pair(level.rhs.e1, level.rhs.e2))
                    )
                }
                else -> throw AssertionError("Not red actually")
            }
            is BufferOfFive -> when(level.rhs) {
                is EmptyBuffer -> {
                    newLevel = DequeLevel(
                            BufferOfThree(level.lhs.e1, level.lhs.e2, level.lhs.e3),
                            BufferOfTwo(level.lhs.e4, level.lhs.e5)
                    )
                    nextLevel = null
                }
                is BufferOfOne -> {
                    newLevel = DequeLevel(
                            BufferOfThree(level.lhs.e1, level.lhs.e2, level.lhs.e3),
                            BufferOfThree(level.lhs.e4, level.lhs.e5, level.rhs.e)
                    )
                    nextLevel = null
                }
                is BufferOfTwo,
                is BufferOfThree -> {
                    newLevel = DequeLevel(
                            lhs = BufferOfThree(level.lhs.e1, level.lhs.e2, level.lhs.e3),
                            rhs = level.rhs
                    )
                    nextLevel = DequeLevel(
                            lhs = BufferOfOne(Pair(level.lhs.e4, level.lhs.e5)),
                            rhs = EmptyBuffer()
                    )
                }
                is BufferOfFour -> {
                    newLevel = DequeLevel(
                            lhs = BufferOfThree(level.lhs.e1, level.lhs.e2, level.lhs.e3),
                            rhs = BufferOfTwo(level.rhs.e3, level.rhs.e4)
                    )
                    nextLevel = DequeLevel(
                            lhs = BufferOfOne(Pair(level.lhs.e4, level.lhs.e5)),
                            rhs = BufferOfOne(Pair(level.rhs.e1, level.rhs.e2))
                    )
                }
                is BufferOfFive -> throw IllegalStateException("Left and right buffers are red at the same time")
            }
        }

        return Pair(newLevel, nextLevel)
    }

    private fun makeRedLevelGreen(level: DequeLevel, nextLevel: DequeLevel): Pair<DequeLevel, DequeLevel?> {
        assert(level.color == RED)

        val newLevel: DequeLevel
        val newNextLevel: DequeLevel

        when(level.lhs) {
            is EmptyBuffer -> when(level.rhs) {
                is BufferOfOne -> {
                    var lhs = nextLevel.lhs
                    var rhs = nextLevel.rhs
                    val lhsFirst: Pair<Any, Any>
                    val rhsLast: Pair<Any, Any>?

                    if (lhs !is EmptyBuffer) {
                        lhsFirst = lhs.first as Pair<Any, Any>
                        lhs = lhs.removeFirst()
                    } else {
                        lhsFirst = rhs.first as Pair<Any, Any>
                        rhs = rhs.removeFirst()
                    }
                    if (rhs !is EmptyBuffer) {
                        rhsLast = rhs.last as Pair<Any, Any>
                        rhs = rhs.removeLast()
                    } else if (lhs !is EmptyBuffer) {
                        rhsLast = lhs.last as Pair<Any, Any>
                        lhs = lhs.removeLast()
                    } else {
                        rhsLast = null
                    }

                    newNextLevel = DequeLevel(lhs, rhs)
                    newLevel = DequeLevel(
                            lhs = BufferOfTwo(lhsFirst.first, lhsFirst.second),
                            rhs = rhsLast?.let { BufferOfThree(rhsLast.first, rhsLast.second, level.rhs.e) } ?: level.rhs
                    )
                }
                is BufferOfTwo,
                is BufferOfThree -> {
                    var lhs = nextLevel.lhs
                    var rhs = nextLevel.rhs
                    val lhsFirst: Pair<Any, Any>

                    if (lhs !is EmptyBuffer) {
                        lhsFirst = lhs.first as Pair<Any, Any>
                        lhs = lhs.removeFirst()
                    } else {
                        lhsFirst = rhs.first as Pair<Any, Any>
                        rhs = rhs.removeFirst()
                    }

                    newNextLevel = DequeLevel(lhs, rhs)
                    newLevel = DequeLevel(
                            lhs = BufferOfTwo(lhsFirst.first, lhsFirst.second),
                            rhs = level.rhs
                    )
                }
                is BufferOfFour -> {
                    var lhs = nextLevel.lhs
                    var rhs = nextLevel.rhs
                    val lhsFirst: Pair<Any, Any>

                    if (lhs !is EmptyBuffer) {
                        lhsFirst = lhs.first as Pair<Any, Any>
                        lhs = lhs.removeFirst()
                    } else {
                        lhsFirst = rhs.first as Pair<Any, Any>
                        rhs = rhs.removeFirst()
                    }

                    newNextLevel = DequeLevel(
                            lhs = lhs,
                            rhs = rhs.addLast(Pair(level.rhs.e1, level.rhs.e2))
                    )
                    newLevel = DequeLevel(
                            lhs = BufferOfTwo(lhsFirst.first, lhsFirst.second),
                            rhs = BufferOfTwo(level.rhs.e3, level.rhs.e4)
                    )
                }
                else -> throw IllegalStateException("Left and right buffers are red at the same time")
            }

            is BufferOfOne -> when(level.rhs) {
                is EmptyBuffer -> {
                    var lhs = nextLevel.lhs
                    var rhs = nextLevel.rhs
                    val lhsFirst: Pair<Any, Any>?
                    val rhsLast: Pair<Any, Any>

                    if (rhs !is EmptyBuffer) {
                        rhsLast = rhs.last as Pair<Any, Any>
                        rhs = rhs.removeLast()
                    } else {
                        rhsLast = lhs.last as Pair<Any, Any>
                        lhs = lhs.removeLast()
                    }
                    if (lhs !is EmptyBuffer) {
                        lhsFirst = lhs.first as Pair<Any, Any>
                        lhs = lhs.removeFirst()
                    } else if (rhs !is EmptyBuffer) {
                        lhsFirst = rhs.first as Pair<Any, Any>
                        rhs = rhs.removeFirst()
                    } else {
                        lhsFirst = null
                    }

                    newNextLevel = DequeLevel(lhs, rhs)
                    newLevel = DequeLevel(
                            lhs = lhsFirst?.let { BufferOfThree(level.lhs.e, lhsFirst.first, lhsFirst.second) } ?: level.lhs,
                            rhs = BufferOfTwo(rhsLast.first, rhsLast.second)
                    )
                }
                is BufferOfFive -> {
                    var lhs = nextLevel.lhs
                    var rhs = nextLevel.rhs
                    val lhsFirst: Pair<Any, Any>

                    if (lhs !is EmptyBuffer) {
                        lhsFirst = lhs.first as Pair<Any, Any>
                        lhs = lhs.removeFirst()
                    } else {
                        lhsFirst = rhs.first as Pair<Any, Any>
                        rhs = rhs.removeFirst()
                    }

                    newNextLevel = DequeLevel(
                            lhs = lhs,
                            rhs = rhs.addLast(Pair(level.rhs.e1, level.rhs.e2))
                    )
                    newLevel = DequeLevel(
                            lhs = BufferOfThree(level.lhs.e, lhsFirst.first, lhsFirst.second),
                            rhs = BufferOfThree(level.rhs.e3, level.rhs.e4, level.rhs.e5)
                    )
                }
                else -> throw IllegalStateException("None of the buffers are red")
            }

            is BufferOfTwo,
            is BufferOfThree -> when(level.rhs) {
                is EmptyBuffer -> {
                    var lhs = nextLevel.lhs
                    var rhs = nextLevel.rhs
                    val rhsLast: Pair<Any, Any>

                    if (rhs !is EmptyBuffer) {
                        rhsLast = rhs.last as Pair<Any, Any>
                        rhs = rhs.removeLast()
                    } else {
                        rhsLast = lhs.last as Pair<Any, Any>
                        lhs = lhs.removeLast()
                    }

                    newNextLevel = DequeLevel(lhs, rhs)
                    newLevel = DequeLevel(
                            lhs = level.lhs,
                            rhs = BufferOfTwo(rhsLast.first, rhsLast.second)
                    )
                }
                is BufferOfFive -> {
                    newNextLevel = DequeLevel(
                            lhs = nextLevel.lhs,
                            rhs = nextLevel.rhs.addLast(Pair(level.rhs.e1, level.rhs.e2))
                    )
                    newLevel = DequeLevel(
                            lhs = level.lhs,
                            rhs = BufferOfThree(level.rhs.e3, level.rhs.e4, level.rhs.e5)
                    )
                }
                else -> throw IllegalStateException("None of the buffers are red")
            }

            is BufferOfFour -> when(level.rhs) {
                is EmptyBuffer -> {
                    var lhs = nextLevel.lhs
                    var rhs = nextLevel.rhs
                    val rhsLast: Pair<Any, Any>

                    if (rhs !is EmptyBuffer) {
                        rhsLast = rhs.last as Pair<Any, Any>
                        rhs = rhs.removeLast()
                    } else {
                        rhsLast = lhs.last as Pair<Any, Any>
                        lhs = lhs.removeLast()
                    }

                    newNextLevel = DequeLevel(
                            lhs = lhs.addFirst(Pair(level.lhs.e3, level.lhs.e4)),
                            rhs = rhs
                    )
                    newLevel = DequeLevel(
                            lhs = BufferOfTwo(level.lhs.e1, level.lhs.e2),
                            rhs = BufferOfTwo(rhsLast.first, rhsLast.second)
                    )
                }
                is BufferOfFive -> {
                    newNextLevel = DequeLevel(
                            lhs = nextLevel.lhs.addFirst(Pair(level.lhs.e3, level.lhs.e4)),
                            rhs = nextLevel.rhs.addLast(Pair(level.rhs.e1, level.rhs.e2))
                    )
                    newLevel = DequeLevel(
                            lhs = BufferOfTwo(level.lhs.e1, level.lhs.e2),
                            rhs = BufferOfThree(level.rhs.e3, level.rhs.e4, level.rhs.e5)
                    )
                }
                else -> throw IllegalStateException("None of the buffers are red")
            }

            is BufferOfFive -> when(level.rhs) {
                is BufferOfOne -> {
                    var lhs = nextLevel.lhs
                    var rhs = nextLevel.rhs
                    val rhsLast: Pair<Any, Any>

                    if (rhs !is EmptyBuffer) {
                        rhsLast = rhs.last as Pair<Any, Any>
                        rhs = rhs.removeLast()
                    } else {
                        rhsLast = lhs.last as Pair<Any, Any>
                        lhs = lhs.removeLast()
                    }

                    newNextLevel = DequeLevel(
                            lhs = lhs.addFirst(Pair(level.lhs.e4, level.lhs.e5)),
                            rhs = rhs
                    )
                    newLevel = DequeLevel(
                            lhs = BufferOfThree(level.lhs.e1, level.lhs.e2, level.lhs.e3),
                            rhs = BufferOfThree(rhsLast.first, rhsLast.second, level.rhs.e)
                    )
                }
                is BufferOfTwo,
                is BufferOfThree -> {
                    newNextLevel = DequeLevel(
                            lhs = nextLevel.lhs.addFirst(Pair(level.lhs.e4, level.lhs.e5)),
                            rhs = nextLevel.rhs
                    )
                    newLevel = DequeLevel(
                            lhs = BufferOfThree(level.lhs.e1, level.lhs.e2, level.lhs.e3),
                            rhs = level.rhs
                    )
                }
                is BufferOfFour -> {
                    newNextLevel = DequeLevel(
                            lhs = nextLevel.lhs.addFirst(Pair(level.lhs.e4, level.lhs.e5)),
                            rhs = nextLevel.rhs.addLast(Pair(level.rhs.e1, level.rhs.e2))
                    )
                    newLevel = DequeLevel(
                            lhs = BufferOfThree(level.lhs.e1, level.lhs.e2, level.lhs.e3),
                            rhs = BufferOfTwo(level.rhs.e3, level.rhs.e4)
                    )
                }
                else -> throw IllegalStateException("Left and right buffers are red at the same time")
            }
        }

        assert(newLevel.color == GREEN || (newNextLevel.lhs is EmptyBuffer && newNextLevel.rhs is EmptyBuffer))

        if (newNextLevel.lhs is EmptyBuffer && newNextLevel.rhs is EmptyBuffer) {
            return Pair(newLevel, null)
        }

        return Pair(newLevel, newNextLevel)
    }

    private fun levelColor(level: DequeLevel, isBottomLevel: Boolean): Int {
        return if (isBottomLevel) bottomLevelColor(level) else level.color
    }
    private fun bottomLevelColor(level: DequeLevel): Int {
        if (level.lhs is EmptyBuffer) return level.rhs.color
        if (level.rhs is EmptyBuffer) return level.lhs.color
        return level.color
    }
}