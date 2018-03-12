package persistentDeque

import buffer.*
import deque.ImmutableDeque
import level.DequeBottomLevel
import level.ImmutableLevel
import level.NonBottomLevel
import level.SubStackBottomLevel
import java.util.*

const val PUSH_TO_LHS = 0
const val POP_FROM_LHS = 1
const val PUSH_TO_RHS = 2
const val POP_FROM_RHS = 3

internal class DequeSubStack(val stack: ImmutableLevel, val next: DequeSubStack?)
internal class NextHolder(val nextLevel: ImmutableLevel?, val nextSubStack: DequeSubStack?)

internal class PersistentDeque<T>(private val lhs: ImmutableBuffer,
                                  private val rhs: ImmutableBuffer,
                                  private val next: NextHolder,
                                  override val size: Int): ImmutableDeque<T> {
    override fun isEmpty(): Boolean {
        return false
    }

    override val first: T?
        get() {
            return this.lhs.top as T
        }

    override val last: T?
        get() {
            return this.rhs.top as T
        }

    override fun addFirst(value: T): ImmutableDeque<T> {
        if (this.lhs.size + 1 < YELLOW_HIGH) {
            val newLhs = this.lhs.push(value)
            return PersistentDeque(newLhs, this.rhs, this.next, this.size + 1)
        }
        if (this.lhs.size + 1 == YELLOW_HIGH) {
            val newLhs = this.lhs.push(value)
            return this.dequeWithYellowTopLevel(newLhs, this.rhs, this.size + 1)
        }
        return this.makeGreenTopSubStackTopPerforming(PUSH_TO_LHS, value, this.size + 1)
    }

    override fun removeFirst(): ImmutableDeque<T> {
        if (this.lhs.size - 1 > YELLOW_LOW) {
            val newLhs = this.lhs.pop()
            return PersistentDeque(newLhs, this.rhs, this.next, this.size - 1)
        }
        if (this.lhs.size - 1 == YELLOW_LOW) {
            val newLhs = this.lhs.pop()
            return this.dequeWithYellowTopLevel(newLhs, this.rhs, this.size - 1)
        }
        return this.makeGreenTopSubStackTopPerforming(POP_FROM_LHS, null, this.size - 1)
    }

    override fun addLast(value: T): ImmutableDeque<T> {
        if (this.rhs.size + 1 < YELLOW_HIGH) {
            val newRhs = this.rhs.push(value)
            return PersistentDeque(this.lhs, newRhs, this.next, this.size + 1)
        }
        if (this.rhs.size + 1 == YELLOW_HIGH) {
            val newRhs = this.rhs.push(value)
            return this.dequeWithYellowTopLevel(this.lhs, newRhs, this.size + 1)
        }
        return this.makeGreenTopSubStackTopPerforming(PUSH_TO_RHS, value, this.size + 1)
    }

    override fun removeLast(): ImmutableDeque<T> {
        if (this.rhs.size - 1 > YELLOW_LOW) {
            val newRhs = this.rhs.pop()
            return PersistentDeque(this.lhs, newRhs, this.next, this.size - 1)
        }
        if (this.rhs.size - 1 == YELLOW_LOW) {
            val newRhs = this.rhs.pop()
            return this.dequeWithYellowTopLevel(this.lhs, newRhs, this.size - 1)
        }
        return this.makeGreenTopSubStackTopPerforming(POP_FROM_RHS, null, this.size - 1)
    }

    private fun dequeWithYellowTopLevel(newLhs: ImmutableBuffer, newRhs: ImmutableBuffer, newSize: Int): ImmutableDeque<T> {
//        assert(newTopSubStack.color == YELLOW)

        if (this.next.nextSubStack == null || this.next.nextSubStack.stack.color == GREEN) {
            return PersistentDeque(newLhs, newRhs, this.next, newSize)
        }

//        assert(this.next.stack.color == RED)

        return this.makeGreenNextSubStackTop(newLhs, newRhs, newSize)
    }

    private fun makeGreenNextSubStackTop(newLhs: ImmutableBuffer, newRhs: ImmutableBuffer, newSize: Int): ImmutableDeque<T> {
//        println(newSize - lastRegularizationSize)
//        lastRegularizationSize = newSize

        val upperLevel = this.next.nextSubStack!!.stack
        var lowerLevel = this.next.nextSubStack.stack.next
        var lowerSubStack = this.next.nextSubStack.next
        if (lowerLevel == null) {
            lowerLevel = this.next.nextSubStack.next?.stack
            lowerSubStack = this.next.nextSubStack.next?.next
            if (lowerLevel == null) {
//                assert(upperLevel.lhs.size == RED_HIGH || upperLevel.rhs.size == RED_HIGH)
//                assert(upperLevel is DequeBottomLevel<*>)
//                assert(lowerSubStack == null)
                if (upperLevel.lhs.size == RED_HIGH && upperLevel.rhs.size + 2 <= GREEN_HIGH) {
                    val toMoveToRhsSide = GREEN_HIGH - upperLevel.rhs.size
                    val fromLhs = upperLevel.lhs.pop(upperLevel.lhs.size - toMoveToRhsSide).moveToOppositeSideBuffer()
                    val upperRhs = upperLevel.rhs.prependSavingOrder(fromLhs)
                    val upperLhs = upperLevel.lhs.removeBottom(toMoveToRhsSide)
                    val newUpperLevel = DequeBottomLevel<T>(upperLhs, upperRhs)
                    val newNext = NextHolder(this.next.nextLevel, DequeSubStack(newUpperLevel, null))
                    return PersistentDeque(newLhs, newRhs, newNext, newSize)
                } else if (upperLevel.rhs.size == RED_HIGH && upperLevel.lhs.size + 2 <= GREEN_HIGH) {
                    val toMoveToLhsSide = GREEN_HIGH - upperLevel.lhs.size
                    val fromRhs = upperLevel.rhs.pop(upperLevel.rhs.size - toMoveToLhsSide).moveToOppositeSideBuffer()
                    val upperLhs = upperLevel.lhs.prependSavingOrder(fromRhs)
                    val upperRhs = upperLevel.rhs.removeBottom(toMoveToLhsSide)
                    val newUpperLevel = DequeBottomLevel<T>(upperLhs, upperRhs)
                    val newNext = NextHolder(this.next.nextLevel, DequeSubStack(newUpperLevel, null))
                    return PersistentDeque(newLhs, newRhs, newNext, newSize)
                }

                lowerLevel = DequeBottomLevel<T>(LhsEmptyBuffer, RhsEmptyBuffer)
            }
        }

        val result: ImmutableDeque<T>
                = lowerLevel.makeGreenUpperLevel(upperLevel, this.topSubStack(newLhs, newRhs), lowerSubStack, newSize)

        if (result is PersistentDeque) {
            result.checkInvariants()
        }

        return result
    }

    private fun makeGreenTopSubStackTopPerforming(operation: Int, value: T?, newSize: Int): ImmutableDeque<T> {
//        println(newSize - lastRegularizationSize)
//        lastRegularizationSize = newSize

        var lowerLevel = this.next.nextLevel
        var lowerSubStack: DequeSubStack? = this.next.nextSubStack
        if (lowerLevel == null) {
            lowerLevel = this.next.nextSubStack!!.stack
            lowerSubStack = this.next.nextSubStack.next
        }

        val result: ImmutableDeque<T> = when(operation) {
            PUSH_TO_LHS ->  lowerLevel.makeGreenUpperLevelPushingLhs(this.lhs, this.rhs, value, lowerSubStack, newSize)
            POP_FROM_LHS -> lowerLevel.makeGreenUpperLevelPoppingLhs(this.rhs, lowerSubStack, newSize)
            PUSH_TO_RHS ->  lowerLevel.makeGreenUpperLevelPushingRhs(this.lhs, this.rhs, value, lowerSubStack, newSize)
            POP_FROM_RHS -> lowerLevel.makeGreenUpperLevelPoppingRhs(this.lhs, lowerSubStack, newSize)
            else ->         throw AssertionError("Unreachable")
        }

        if (result is PersistentDeque) {
            result.checkInvariants()
        }

        return result
    }

    override fun toList(): List<T> {
        val lhsList = mutableListOf<Any?>()
        val rhsList = mutableListOf<Any?>()

        var depth = 0

        val iterator = LevelIterator(this.topSubStack(), this.next.nextSubStack)
        while (iterator.hasNext()) {
            val level = iterator.next()
            level.lhs.addLeafValuesTo(lhsList, depth)

            val list = mutableListOf<Any?>()
            level.rhs.addLeafValuesTo(list, depth)

            rhsList.addAll(list.reversed())

            depth += 1
        }

        return (lhsList + rhsList.reversed()) as List<T>
    }

    override fun get(index: Int): T {
        if (index < 0 || index >= this.size) throw IndexOutOfBoundsException()

        var depth = 0
        var lhsIndex = index
        var rhsIndex = this.size - index - 1

        val levelIterator = LevelIterator(this.topSubStack(), this.next.nextSubStack)

        while (levelIterator.hasNext()) {
            val level = levelIterator.next()
            val lhsSize = level.lhs.size shl depth
            val rhsSize = level.rhs.size shl depth

            if (lhsIndex < lhsSize) {
                return level.lhs.getLeafValueAt(lhsIndex, depth) as T
            }
            if (rhsIndex < rhsSize) {
                return level.rhs.getLeafValueAt(rhsSize - rhsIndex - 1, depth) as T
            }

            depth += 1
            lhsIndex -= lhsSize
            rhsIndex -= rhsSize
        }

        throw AssertionError("Unreachable")
    }

    override fun set(index: Int, value: T): ImmutableDeque<T> {
        if (index < 0 || index >= this.size) throw IndexOutOfBoundsException()

        var depth = 0
        var dequeSize = this.size
        var lhsIndex = index
        var rhsIndex = dequeSize - index - 1

        val upperSubStacks = Stack<ImmutableLevel>()
        var subStack: DequeSubStack? = DequeSubStack(this.topSubStack(), this.next.nextSubStack)

        while (subStack != null) {

            var level: ImmutableLevel? = subStack.stack
            var subStackSize = 0
            val subStackIndex = lhsIndex
            val subStackDepth = depth

            while (level != null) {
                val lhsSize = level.lhs.size shl depth
                val rhsSize = level.rhs.size shl depth

                if (lhsIndex < lhsSize || rhsIndex < rhsSize) {
                    val newStack = subStack.stack.setBufferLeafValueAt(subStackIndex, value, dequeSize, subStackDepth)
                    var newSubStack = DequeSubStack(newStack, subStack.next)

                    while (!upperSubStacks.isEmpty()) {
                        newSubStack = DequeSubStack(upperSubStacks.pop(), newSubStack)
                    }
                    val newNext = NextHolder(newSubStack.stack.next, newSubStack.next)
                    return PersistentDeque(newSubStack.stack.lhs, newSubStack.stack.rhs, newNext, this.size)
                }

                depth += 1
                lhsIndex -= lhsSize
                rhsIndex -= rhsSize
                subStackSize += lhsSize + rhsSize

                level = level.next
            }

            upperSubStacks.push(subStack.stack)
            dequeSize -= subStackSize
            subStack = subStack.next
        }

        throw AssertionError("Unreachable")
    }

    override fun listIterator(index: Int): ListIterator<T> {
        val levelIterator = LevelIterator(this.topSubStack(), this.next.nextSubStack)
        return PersistentDequeIterator(levelIterator, index, this.size)
    }

    private fun topSubStack(): ImmutableLevel {
        if (this.next.nextLevel == null) {
            return SubStackBottomLevel(this.lhs, this.rhs)
        }
        return NonBottomLevel(this.lhs, this.rhs, this.next.nextLevel)
    }

    private fun topSubStack(newLhs: ImmutableBuffer, newRhs: ImmutableBuffer): ImmutableLevel {
        if (this.next.nextLevel == null) {
            return SubStackBottomLevel(newLhs, newRhs)
        }
        return NonBottomLevel(newLhs, newRhs, this.next.nextLevel)
    }

    private fun checkInvariants() {
//        val rhss = mutableListOf<Int>()
//        val levelIterator = LevelIterator(this.topSubStack, this.next)
//        while (levelIterator.hasNext()) {
//            rhss.add(levelIterator.next().lhs.size)
//        }
//        println(rhss)

        var topSubStack = this.topSubStack()
        var next: DequeSubStack? = this.next.nextSubStack

        var isCurrentLevelTopLevel = true

        while (next != null) {
            val colorOfCurrentLevel = topSubStack.color
            val colorOfNextTopLevel = next.stack.color

            if (isCurrentLevelTopLevel && colorOfCurrentLevel == RED) {
                throw IllegalStateException()
            }
            if (isCurrentLevelTopLevel
                    && colorOfCurrentLevel == YELLOW
                    && colorOfNextTopLevel != GREEN) {
                throw IllegalStateException()
            }
            if (colorOfNextTopLevel == YELLOW) {
                throw IllegalStateException()
            }
            if (!isCurrentLevelTopLevel
                    && colorOfCurrentLevel != GREEN
                    && colorOfNextTopLevel != GREEN) {
                throw IllegalStateException()
            }

            if (colorOfNextTopLevel != GREEN && colorOfNextTopLevel != RED) {
                throw IllegalStateException()
            }

            isCurrentLevelTopLevel = false
            topSubStack = next.stack
            next = next.next
        }
    }
}
