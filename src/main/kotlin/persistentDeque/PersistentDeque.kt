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
        assert(newTopSubStack.color == YELLOW)

        if (this.next.stack.color == GREEN) {
            return PersistentDeque(newTopSubStack, this.next)
        }

        assert(this.next.stack.color == RED)

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
                assert(upperLevel.lhs.size == RED_HIGH || upperLevel.rhs.size == RED_HIGH)
                assert(lowerSubStack == null)
                lowerLevel = DequeBottomLevel<T>(LhsEmptyBuffer, RhsEmptyBuffer)
            }
        }

        return lowerLevel.makeGreenUpperLevel(upperLevel, newTopSubStack, lowerSubStack)
    }

    private fun makeGreenTopSubStackTopPerforming(operation: Int, value: T?): ImmutableDeque<T> {
        val upperLevel = this.topSubStack
        var lowerLevel = this.topSubStack.next
        var lowerSubStack: DequeSubStack? = this.next
        if (lowerLevel == null) {
            lowerLevel = this.next.stack
            lowerSubStack = this.next.next
        }

        return when(operation) {
            PUSH_TO_LHS ->  lowerLevel.makeGreenUpperLevelPushingLhs(upperLevel, value, lowerSubStack)
            POP_FROM_LHS -> lowerLevel.makeGreenUpperLevelPoppingLhs(upperLevel, lowerSubStack)
            PUSH_TO_RHS ->  lowerLevel.makeGreenUpperLevelPushingRhs(upperLevel, value, lowerSubStack)
            POP_FROM_RHS -> lowerLevel.makeGreenUpperLevelPoppingRhs(upperLevel, lowerSubStack)
            else ->         throw AssertionError("Unreachable")
        }
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

        var dequeSize = this.size
        if (isValueAtIndexLocatedInSubStack(index, this.topSubStack, dequeSize, 0)) {
            return this.topSubStack.getBufferLeafValueAt(index, dequeSize, 0) as T
        }
        dequeSize -= this.topSubStack.subStackSize(0)
        var depth = this.topSubStack.subStackHeight()

        var subStack: DequeSubStack? = this.next
        while (subStack != null) {
            if (isValueAtIndexLocatedInSubStack(index, subStack.stack, dequeSize, depth)) {
                return subStack.stack.getBufferLeafValueAt(index, dequeSize, depth) as T
            }
            dequeSize -= subStack.stack.subStackSize(depth)
            depth += subStack.stack.subStackHeight()
            subStack = subStack.next
        }

        throw AssertionError("Unreachable")
    }

    private fun isValueAtIndexLocatedInSubStack(index: Int, subStack: ImmutableLevel, dequeSize: Int, depth: Int): Boolean {
        var level: ImmutableLevel? = subStack
        var levelDepth = depth
        var subStackSize = dequeSize

        while (level != null) {
            val lhsSize = level.lhs.size shl levelDepth
            val rhsSize = level.rhs.size shl levelDepth
            if (index < lhsSize || subStackSize - index <= rhsSize) {
                return true
            }

            level = level.next
            levelDepth += 1
            subStackSize -= lhsSize + rhsSize
        }
        return false
    }

    override fun set(index: Int, value: T): ImmutableDeque<T> {
        if (index < 0 || index >= this.size) throw IndexOutOfBoundsException()


        var dequeSize = this.size
        if (isValueAtIndexLocatedInSubStack(index, this.topSubStack, dequeSize, 0)) {
            val newTopSubStack = this.topSubStack.setBufferLeafValueAt(index, value, dequeSize, 0)
            return PersistentDeque(newTopSubStack, this.next)
        }
        dequeSize -= this.topSubStack.subStackSize(0)
        var depth = this.topSubStack.subStackHeight()

        val upperSubStacks = Stack<ImmutableLevel>()

        var subStack: DequeSubStack? = this.next
        while (subStack != null) {
            if (isValueAtIndexLocatedInSubStack(index, subStack.stack, dequeSize, depth)) {
                val newStack = subStack.stack.setBufferLeafValueAt(index, value, dequeSize, 0)
                var newSubStack = DequeSubStack(newStack, subStack.next)

                while (!upperSubStacks.isEmpty()) {
                    newSubStack = DequeSubStack(upperSubStacks.pop(), newSubStack)
                }
                return PersistentDeque(this.topSubStack, newSubStack)
            }
            dequeSize -= subStack.stack.subStackSize(depth)
            depth += subStack.stack.subStackHeight()
            upperSubStacks.push(subStack.stack)
            subStack = subStack.next
        }

        throw AssertionError("Unreachable")
    }

    fun listIterator(index: Int): ListIterator<T> {
        val levelIterator = LevelIterator(topSubStack, next)
        return PersistentDequeIterator(levelIterator, index, size)
    }

    fun listIterator(): ListIterator<T> {
        return this.listIterator(0)
    }
}
