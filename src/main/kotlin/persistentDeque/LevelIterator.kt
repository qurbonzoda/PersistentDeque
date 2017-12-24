package persistentDeque

import buffer.EmptyBuffer
import buffer.GREEN
import buffer.RED
import buffer.YELLOW
import persistentStack.PersistentStack
import persistentStack.emptyStack
import persistentStack.stackOf

internal class LevelIterator(private var subStack: DequeSubStack?) {
    var stack: PersistentStack<DequeLevel> = emptyStack()

    fun hasNext(): Boolean {
        return !stack.isEmpty() || subStack != null
    }

    fun next(): DequeLevel {
        if (stack.isEmpty()) {
            stack = subStack!!.stack
            subStack = subStack!!.next
        }

        val result = stack.peek()!!
        stack = stack.pop()
        return result
    }

    fun skipStack() {
        stack = emptyStack()
    }

    fun add(level: DequeLevel) {
        if (stack.isEmpty() || levelColor(stack.peek()!!, hasOnlyOneLevel()) == YELLOW) {
            stack = stack.push(level)
        } else {
            subStack = DequeSubStack(stack, subStack)
            stack = stackOf(level)
        }
    }

    fun addStack(stack: PersistentStack<DequeLevel>) {
        if (!this.stack.isEmpty()) {
            subStack = DequeSubStack(this.stack, subStack)
        }
        this.stack = stack
    }

    fun createDequeSubStack(): DequeSubStack? {
        val dequeSubStack = if (stack.isEmpty()) {
            subStack
        } else {
            DequeSubStack(stack, subStack)
        }
//        checkInvariants(dequeSubStack)
        return dequeSubStack
    }

    fun hasOnlyOneLevel(): Boolean {
        if (stack.isEmpty()) {
            return subStack != null && subStack!!.next == null && subStack!!.stack.pop().isEmpty()
        }
        return stack.pop().isEmpty() && subStack == null
    }

    private fun levelColor(level: DequeLevel, isBottomLevel: Boolean): Int {
        return if (isBottomLevel) bottomLevelColor(level) else level.color
    }

    private fun bottomLevelColor(level: DequeLevel): Int {
        if (level.lhs is EmptyBuffer) return level.rhs.color
        if (level.rhs is EmptyBuffer) return level.lhs.color
        return level.color
    }

    private fun checkInvariants(stack: DequeSubStack?) {
        var result = stack
        var isCurrentLevelTopLevel = true

        while (result != null && result.next != null) {
            val currentLevel = result.stack.peek()!!

            val isNextTopLevelBottomLevel = result.next!!.next == null && result.next!!.stack.pop().isEmpty()
            val nextTopLevel = result.next!!.stack.peek()!!

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
            result = result.next
        }
    }
}