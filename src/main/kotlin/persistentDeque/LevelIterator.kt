package persistentDeque

import buffer.EmptyBuffer
import buffer.GREEN
import buffer.RED
import buffer.YELLOW

internal data class LevelStack(val value: DequeLevel, val next: LevelStack?)

internal class LevelIterator(private var subStack: DequeSubStack?) {
    var stack: LevelStack? = null

    fun hasNext(): Boolean {
        return stack != null || subStack != null
    }

    fun next(): DequeLevel {
        if (stack == null) {
            stack = subStack!!.stack
            subStack = subStack!!.next
        }

        val result = stack!!.value
        stack = stack!!.next
        return result
    }

    fun skipStack() {
        stack = null
    }

    fun add(level: DequeLevel) {
        if (stack == null || levelColor(stack!!.value, hasOnlyOneLevel()) == YELLOW) {
            stack = LevelStack(level, stack)
        } else {
            subStack = DequeSubStack(stack!!, subStack)
            stack = LevelStack(level, null)
        }
    }

    fun addStack(stack: LevelStack) {
        if (this.stack != null) {
            assert(levelColor(this.stack!!.value, isBottomLevel = false) != YELLOW)
            subStack = DequeSubStack(this.stack!!, subStack)
        }
        this.stack = stack
    }

    fun createDequeSubStack(): DequeSubStack? {
        val dequeSubStack = if (stack == null) {
            subStack
        } else {
            DequeSubStack(stack!!, subStack)
        }
        checkInvariants(dequeSubStack)
        return dequeSubStack
    }

    fun hasOnlyOneLevel(): Boolean {
        if (stack == null) {
            return subStack != null && subStack!!.next == null && subStack!!.stack.next == null
        }
        return stack!!.next == null && subStack == null
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
            val currentLevel = result.stack.value

            val isNextTopLevelBottomLevel = result.next!!.next == null && result.next!!.stack.next == null
            val nextTopLevel = result.next!!.stack.value

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