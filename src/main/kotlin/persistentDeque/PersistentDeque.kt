package persistentDeque

import buffer.*
import deque.ImmutableDeque
import level.DequeBottomLevel
import level.ImmutableLevel
import java.util.*

const val PUSH_TO_LHS = 0
const val POP_FROM_LHS = 1
const val PUSH_TO_RHS = 2
const val POP_FROM_RHS = 3

internal data class DequeSubStack(val stack: ImmutableLevel, val next: DequeSubStack?)

internal class PersistentDeque<T>(private val topSubStack: ImmutableLevel,
                                  private val next: DequeSubStack): ImmutableDeque<T> {
    override fun isEmpty(): Boolean {
        return false
    }

    override val size: Int
        get() {
            var subStack: DequeSubStack? = this.next
            var size = this.topSubStack.subStackSize(0)
            var depth = this.topSubStack.subStackHeight()

            while (subStack != null) {
                size += subStack.stack.subStackSize(depth)
                depth += subStack.stack.subStackHeight()
                subStack = subStack.next
            }
            return size
        }

    override val first: T?
        get() {
            return this.topSubStack.lhs.top as T
        }

    override val last: T?
        get() {
            return this.topSubStack.rhs.top as T
        }

    override fun addFirst(value: T): ImmutableDeque<T> {
        if (this.topSubStack.lhs.size + 1 < YELLOW_HIGH) {
            val newTopSubStack = this.topSubStack.withNewLhs(this.topSubStack.lhs.push(value))
            return PersistentDeque(newTopSubStack, this.next)
        }
        if (this.topSubStack.lhs.size + 1 == YELLOW_HIGH) {
            val newTopSubStack = this.topSubStack.withNewLhs(this.topSubStack.lhs.push(value))
            return this.dequeWithYellowTopLevel(newTopSubStack)
        }
        return this.makeGreenTopSubStackTopPerforming(PUSH_TO_LHS, value)
    }

    override fun removeFirst(): ImmutableDeque<T> {
        if (this.topSubStack.lhs.size - 1 > YELLOW_LOW) {
            val newTopSubStack = this.topSubStack.withNewLhs(this.topSubStack.lhs.pop())
            return PersistentDeque(newTopSubStack, this.next)
        }
        if (this.topSubStack.lhs.size - 1 == YELLOW_LOW) {
            val newTopSubStack = this.topSubStack.withNewLhs(this.topSubStack.lhs.pop())
            return this.dequeWithYellowTopLevel(newTopSubStack)
        }
        return this.makeGreenTopSubStackTopPerforming(POP_FROM_LHS, null)
    }

    override fun addLast(value: T): ImmutableDeque<T> {
        if (this.topSubStack.rhs.size + 1 < YELLOW_HIGH) {
            val newTopSubStack = this.topSubStack.withNewRhs(this.topSubStack.rhs.push(value))
            return PersistentDeque(newTopSubStack, this.next)
        }
        if (this.topSubStack.rhs.size + 1 == YELLOW_HIGH) {
            val newTopSubStack = this.topSubStack.withNewRhs(this.topSubStack.rhs.push(value))
            return this.dequeWithYellowTopLevel(newTopSubStack)
        }
        return this.makeGreenTopSubStackTopPerforming(PUSH_TO_RHS, value)
    }

    override fun removeLast(): ImmutableDeque<T> {
        if (this.topSubStack.rhs.size - 1 > YELLOW_LOW) {
            val newTopSubStack = this.topSubStack.withNewRhs(this.topSubStack.rhs.pop())
            return PersistentDeque(newTopSubStack, this.next)
        }
        if (this.topSubStack.rhs.size - 1 == YELLOW_LOW) {
            val newTopSubStack = this.topSubStack.withNewRhs(this.topSubStack.rhs.pop())
            return this.dequeWithYellowTopLevel(newTopSubStack)
        }
        return this.makeGreenTopSubStackTopPerforming(POP_FROM_RHS, null)
    }

    private fun dequeWithYellowTopLevel(newTopSubStack: ImmutableLevel): ImmutableDeque<T> {
//        assert(newTopSubStack.color == YELLOW)

        if (this.next.stack.color == GREEN) {
            return PersistentDeque(newTopSubStack, this.next)
        }

//        assert(this.next.stack.color == RED)

        return this.makeGreenNextSubStackTop(newTopSubStack)
    }

    private fun makeGreenNextSubStackTop(newTopSubStack: ImmutableLevel): ImmutableDeque<T> {
        val upperLevel = this.next.stack
        var lowerLevel = this.next.stack.next
        var lowerSubStack = this.next.next
        if (lowerLevel == null) {
            lowerLevel = this.next.next?.stack
            lowerSubStack = this.next.next?.next
            if (lowerLevel == null) {   // upperLevel is DequeBottomLevel
//                assert(upperLevel.lhs.size == RED_HIGH || upperLevel.rhs.size == RED_HIGH)
//                assert(upperLevel is DequeBottomLevel<*>)
//                assert(lowerSubStack == null)
                if (upperLevel.lhs.size == RED_HIGH && upperLevel.rhs.size + 2 < YELLOW_HIGH) {
                    val fromLhs = upperLevel.lhs.pop(upperLevel.lhs.size - 2).moveToOppositeSideBuffer()
                    val newRhs = upperLevel.rhs.prependSavingOrder(fromLhs)
                    val newLhs = upperLevel.lhs.removeBottom(2)
                    val newUpperLevel = DequeBottomLevel<T>(newLhs, newRhs)
                    return PersistentDeque(newTopSubStack, DequeSubStack(newUpperLevel, null))
                } else if (upperLevel.rhs.size == RED_HIGH && upperLevel.lhs.size + 2 < YELLOW_HIGH) {
                    val fromRhs = upperLevel.rhs.pop(upperLevel.rhs.size - 2).moveToOppositeSideBuffer()
                    val newLhs = upperLevel.lhs.prependSavingOrder(fromRhs)
                    val newRhs = upperLevel.rhs.removeBottom(2)
                    val newUpperLevel = DequeBottomLevel<T>(newLhs, newRhs)
                    return PersistentDeque(newTopSubStack, DequeSubStack(newUpperLevel, null))
                }

                lowerLevel = DequeBottomLevel<T>(LhsEmptyBuffer, RhsEmptyBuffer)
            }
        }

        if (lowerLevel.color == RED) {
            println()
        }

        val result: ImmutableDeque<T> = lowerLevel.makeGreenUpperLevel(upperLevel, newTopSubStack, lowerSubStack)
//
//        if (result is PersistentDeque) {
//            result.checkInvariants()
//        }

        return result
    }

    private fun makeGreenTopSubStackTopPerforming(operation: Int, value: T?): ImmutableDeque<T> {
        val upperLevel = this.topSubStack
        var lowerLevel = this.topSubStack.next
        var lowerSubStack: DequeSubStack? = this.next
        if (lowerLevel == null) {
            lowerLevel = this.next.stack
            lowerSubStack = this.next.next
        }

        val result: ImmutableDeque<T> = when(operation) {
            PUSH_TO_LHS ->  lowerLevel.makeGreenUpperLevelPushingLhs(upperLevel, value, lowerSubStack)
            POP_FROM_LHS -> lowerLevel.makeGreenUpperLevelPoppingLhs(upperLevel, lowerSubStack)
            PUSH_TO_RHS ->  lowerLevel.makeGreenUpperLevelPushingRhs(upperLevel, value, lowerSubStack)
            POP_FROM_RHS -> lowerLevel.makeGreenUpperLevelPoppingRhs(upperLevel, lowerSubStack)
            else ->         throw AssertionError("Unreachable")
        }
//        if (result is PersistentDeque) {
//            result.checkInvariants()
//        }
        return result
    }

    override fun toList(): List<T> {
        val lhsList = mutableListOf<Any?>()
        val rhsList = mutableListOf<Any?>()

        var depth = 0

        val iterator = LevelIterator(this.topSubStack, this.next)
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

        val levelIterator = LevelIterator(this.topSubStack, this.next)

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
        var subStack: DequeSubStack? = DequeSubStack(this.topSubStack, this.next)

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
                    return PersistentDeque(newSubStack.stack, newSubStack.next!!)
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
        val levelIterator = LevelIterator(topSubStack, next)
        return PersistentDequeIterator(levelIterator, index, size)
    }

    private fun checkInvariants() {
        var topSubStack = this.topSubStack
        var next: DequeSubStack? = this.next

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
