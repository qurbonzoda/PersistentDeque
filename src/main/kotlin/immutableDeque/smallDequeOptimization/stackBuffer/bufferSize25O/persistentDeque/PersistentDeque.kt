package immutableDeque.smallDequeOptimization.stackBuffer.bufferSize25O.persistentDeque

import immutableDeque.ImmutableDeque
import immutableDeque.smallDequeOptimization.stackBuffer.bufferSize25O.buffer.ImmutableBuffer
import immutableDeque.smallDequeOptimization.stackBuffer.bufferSize25O.buffer.LhsEmptyBuffer
import immutableDeque.smallDequeOptimization.stackBuffer.bufferSize25O.buffer.RhsEmptyBuffer
import immutableDeque.smallDequeOptimization.stackBuffer.bufferSize25O.constants.*
import immutableDeque.smallDequeOptimization.stackBuffer.bufferSize25O.level.DequeBottomLevel
import immutableDeque.smallDequeOptimization.stackBuffer.bufferSize25O.level.ImmutableLevel
import immutableDeque.smallDequeOptimization.stackBuffer.bufferSize25O.level.NonBottomLevel
import immutableDeque.smallDequeOptimization.stackBuffer.bufferSize25O.level.SubStackBottomLevel
import java.util.*

internal class DequeSubStack(val stack: ImmutableLevel, val next: DequeSubStack?)

internal class PersistentDeque<T>(val lhs: ImmutableBuffer,
                                  val rhs: ImmutableBuffer,
                                  val next: DequeSubStack,
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
        if (this.lhs.size + 1 == RED_HIGH) {
//            println(this.size - lastRegularizationSize)
//            lastRegularizationSize = this.size

            return this.next.stack.makeGreenUpperLevelPushingLhs(value, this, this.next.next)
        }

//        assert(this.lhs.size + 1 == YELLOW_HIGH)

        if (this.next.stack.color == RED) {
            val nextSubStack = this.makeGreenNextSubStackTop(this.next)
            return nextSubStack.stack.makeGreenUpperLevelPushingLhs(value, this, nextSubStack.next)
        }
        if (this.next.next != null && this.next.next.stack.color == RED) {
            val nextSubStack = this.makeGreenNextSubStackTop(this.next.next)
            return this.next.stack.makeGreenUpperLevelPushingLhs(value, this, nextSubStack)
        }

        val newLhs = this.lhs.push(value)
        return PersistentDeque(newLhs, this.rhs, this.next, this.size + 1)
    }

    override fun removeFirst(): ImmutableDeque<T> {
        if (this.lhs.size - 1 > YELLOW_LOW) {
            val newLhs = this.lhs.pop()
            return PersistentDeque(newLhs, this.rhs, this.next, this.size - 1)
        }
        if (this.lhs.size - 1 == RED_LOW) {
//            println(this.size - lastRegularizationSize)
//            lastRegularizationSize = this.size

            return this.next.stack.makeGreenUpperLevelPoppingLhs(this, this.next.next)
        }

//        assert(this.lhs.size - 1 == YELLOW_LOW)

        if (this.next.stack.color == RED) {
            val nextSubStack = this.makeGreenNextSubStackTop(this.next)
            return nextSubStack.stack.makeGreenUpperLevelPoppingLhs(this, nextSubStack.next)
        }
        if (this.next.next != null && this.next.next.stack.color == RED) {
            val nextSubStack = this.makeGreenNextSubStackTop(this.next.next)
            return this.next.stack.makeGreenUpperLevelPoppingLhs(this, nextSubStack)
        }

        val newLhs = this.lhs.pop()
        return PersistentDeque(newLhs, this.rhs, this.next, this.size - 1)
    }

    override fun addLast(value: T): ImmutableDeque<T> {
        if (this.rhs.size + 1 < YELLOW_HIGH) {
            val newRhs = this.rhs.push(value)
            return PersistentDeque(this.lhs, newRhs, this.next, this.size + 1)
        }
        if (this.rhs.size + 1 == RED_HIGH) {
//            println(this.size - lastRegularizationSize)
//            lastRegularizationSize = this.size

            return this.next.stack.makeGreenUpperLevelPushingRhs(value, this, this.next.next)
        }

//        assert(this.rhs.size + 1 == YELLOW_HIGH)

        if (this.next.stack.color == RED) {
            val nextSubStack = this.makeGreenNextSubStackTop(this.next)
            return nextSubStack.stack.makeGreenUpperLevelPushingRhs(value, this, nextSubStack.next)
        }
        if (this.next.next != null && this.next.next.stack.color == RED) {
            val nextSubStack = this.makeGreenNextSubStackTop(this.next.next)
            return this.next.stack.makeGreenUpperLevelPushingRhs(value, this, nextSubStack)
        }

        val newRhs = this.rhs.push(value)
        return PersistentDeque(this.lhs, newRhs, this.next, this.size + 1)
    }

    override fun removeLast(): ImmutableDeque<T> {
        if (this.rhs.size - 1 > YELLOW_LOW) {
            val newRhs = this.rhs.pop()
            return PersistentDeque(this.lhs, newRhs, this.next, this.size - 1)
        }
        if (this.rhs.size - 1 == RED_LOW) {
//            println(this.size - lastRegularizationSize)
//            lastRegularizationSize = this.size

            return this.next.stack.makeGreenUpperLevelPoppingRhs(this, this.next.next)
        }

//        assert(this.rhs.size - 1 == YELLOW_LOW)

        if (this.next.stack.color == RED) {
            val nextSubStack = this.makeGreenNextSubStackTop(this.next)
            return nextSubStack.stack.makeGreenUpperLevelPoppingRhs(this, nextSubStack.next)
        }
        if (this.next.next != null && this.next.next.stack.color == RED) {
            val nextSubStack = this.makeGreenNextSubStackTop(this.next.next)
            return this.next.stack.makeGreenUpperLevelPoppingRhs(this, nextSubStack)
        }

        val newRhs = this.rhs.pop()
        return PersistentDeque(this.lhs, newRhs, this.next, this.size - 1)
    }

    private fun makeGreenNextSubStackTop(nextSubStack: DequeSubStack): DequeSubStack {
//        println(this.size - lastRegularizationSize)
//        lastRegularizationSize = this.size

        val upperLevel = nextSubStack.stack
        var lowerLevel = nextSubStack.stack.next
        var lowerSubStack = nextSubStack.next
        if (lowerLevel == null) {
            lowerLevel = nextSubStack.next?.stack
            lowerSubStack = nextSubStack.next?.next
        }

        val newNext: DequeSubStack

        if (lowerLevel == null) {
//            assert(upperLevel.lhs.size == RED_HIGH || upperLevel.rhs.size == RED_HIGH)
//            assert(upperLevel is DequeBottomLevel<*>)
//            assert(lowerSubStack == null)
            if (upperLevel.lhs.size == RED_HIGH && upperLevel.rhs.size + 2 <= GREEN_HIGH) {
                val toMoveToRhsSide = GREEN_HIGH - upperLevel.rhs.size
                val fromLhs = upperLevel.lhs.pop(upperLevel.lhs.size - toMoveToRhsSide).moveToOppositeSideBuffer()
                val upperRhs = upperLevel.rhs.prependSavingOrder(fromLhs)
                val upperLhs = upperLevel.lhs.removeBottom(toMoveToRhsSide)
                val newUpperLevel = DequeBottomLevel<T>(upperLhs, upperRhs)

                newNext = DequeSubStack(newUpperLevel, null)
            } else if (upperLevel.rhs.size == RED_HIGH && upperLevel.lhs.size + 2 <= GREEN_HIGH) {
                val toMoveToLhsSide = GREEN_HIGH - upperLevel.lhs.size
                val fromRhs = upperLevel.rhs.pop(upperLevel.rhs.size - toMoveToLhsSide).moveToOppositeSideBuffer()
                val upperLhs = upperLevel.lhs.prependSavingOrder(fromRhs)
                val upperRhs = upperLevel.rhs.removeBottom(toMoveToLhsSide)
                val newUpperLevel = DequeBottomLevel<T>(upperLhs, upperRhs)

                newNext = DequeSubStack(newUpperLevel, null)
            } else {
                lowerLevel = DequeBottomLevel<T>(LhsEmptyBuffer, RhsEmptyBuffer)
                newNext = lowerLevel.makeGreenUpperLevel(upperLevel, lowerSubStack)
            }
        } else {
            newNext = lowerLevel.makeGreenUpperLevel(upperLevel, lowerSubStack)
        }

        return newNext
    }

    override fun toList(): List<T> {
        val lhsList = mutableListOf<Any?>()
        val rhsList = mutableListOf<Any?>()

        var depth = 0

        val iterator = this.levelIterator()
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

        val levelIterator = this.levelIterator()

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
        var subStack: DequeSubStack? = DequeSubStack(SubStackBottomLevel(this.lhs, this.rhs), this.next)

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
                    val newNext = DequeSubStack(newSubStack.next!!.stack, newSubStack.next!!.next)
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
        return PersistentDequeIterator(this.levelIterator(), index, this.size)
    }

    private fun levelIterator(): LevelIterator {
        return LevelIterator(SubStackBottomLevel(this.lhs, this.rhs), this.next)
    }

    fun checkInvariants() {
//        val rhss = mutableListOf<Int>()
//        val lhss = mutableListOf<Int>()
//        val levelIterator = this.levelIterator()
//        while (levelIterator.hasNext()) {
//            val level = levelIterator.next()
//            rhss.add(level.lhs.size)
//            lhss.add(level.rhs.size)
//        }
//        println(lhss)
//        println(rhss)

        var topSubStack: ImmutableLevel
        var next: DequeSubStack?

        if (this.next.stack.color == YELLOW) {
            topSubStack = NonBottomLevel(this.lhs, this.rhs, this.next.stack)
            next = this.next.next
        } else {
            topSubStack = SubStackBottomLevel(this.lhs, this.rhs)
            next = this.next
        }

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
