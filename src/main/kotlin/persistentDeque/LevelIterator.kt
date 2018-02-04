package persistentDeque

import buffer.EmptyBuffer
import buffer.GREEN
import buffer.RED
import buffer.YELLOW

internal class LevelIterator(private var topSubStack: LevelStack?, private var next: DequeSubStack?) {

    fun hasNext(): Boolean {
        return topSubStack != null || next != null
    }

    fun next(): DequeLevel {
        if (topSubStack == null) {
            topSubStack = next!!.stack
            next = next!!.next
        }

        val result = topSubStack!!.value
        topSubStack = topSubStack!!.next
        return result
    }

    fun add(level: DequeLevel) {
        if (topSubStack == null || levelColor(topSubStack!!.value, hasOnlyOneLevel()) == YELLOW) {
            topSubStack = LevelStack(level, topSubStack)
        } else {
            next = DequeSubStack(topSubStack!!, next)
            topSubStack = LevelStack(level, null)
        }
    }

    fun addStack(stack: LevelStack) {
        if (this.topSubStack != null) {
//            assert(levelColor(this.topSubStack!!.value, hasOnlyOneLevel()) != YELLOW)
            next = DequeSubStack(this.topSubStack!!, next)
        }
        this.topSubStack = stack
    }

    fun <T> createPersistentDeque(): PersistentDeque<T> {
//        checkInvariants()
        return if (topSubStack == null) {
            PersistentDeque(next?.stack, next?.next)
        } else {
            PersistentDeque(topSubStack, next)
        }
    }

    fun hasOnlyOneLevel(): Boolean {
        if (topSubStack == null) {
            return next != null && next!!.next == null && next!!.stack.next == null
        }
        return topSubStack!!.next == null && next == null
    }

    private fun levelColor(level: DequeLevel, isBottomLevel: Boolean): Int {
        return if (isBottomLevel) bottomLevelColor(level) else level.color
    }

    private fun bottomLevelColor(level: DequeLevel): Int {
        if (level.lhs is EmptyBuffer) return level.rhs.color
        if (level.rhs is EmptyBuffer) return level.lhs.color
        return level.color
    }

//    private fun checkInvariants() {
//        var topSubStack = this.topSubStack
//        var next = this.next
//        if (topSubStack == null) {
//            topSubStack = next?.stack
//            next = next?.next
//        }
//
//        var isCurrentLevelTopLevel = true
//
//        while (topSubStack != null && next != null) {
//            val currentLevel = topSubStack.value
//
//            val isNextTopLevelBottomLevel = next.next == null && next.stack.next == null
//            val nextTopLevel = next.stack.value
//
//            if (isCurrentLevelTopLevel && currentLevel.color == RED) {
//                throw IllegalStateException()
//            }
//            if (isCurrentLevelTopLevel
//                    && currentLevel.color == YELLOW
//                    && levelColor(nextTopLevel, isNextTopLevelBottomLevel) != GREEN) {
//                throw IllegalStateException()
//            }
//            if (levelColor(nextTopLevel, isNextTopLevelBottomLevel) == YELLOW) {
//                throw IllegalStateException()
//            }
//            if (!isCurrentLevelTopLevel
//                    && currentLevel.color != GREEN
//                    && levelColor(nextTopLevel, isNextTopLevelBottomLevel) != GREEN) {
//                throw IllegalStateException()
//            }
//
//            if (levelColor(nextTopLevel, isNextTopLevelBottomLevel) != GREEN
//                    && levelColor(nextTopLevel, isNextTopLevelBottomLevel) != RED) {
//                throw IllegalStateException()
//            }
//
//            isCurrentLevelTopLevel = false
//            topSubStack = next.stack
//            next = next.next
//        }
//    }
}