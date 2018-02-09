package persistentDeque

import buffer.*

internal class LevelIterator(private var topSubStack: LevelStack?, private var next: DequeSubStack?) {

    fun hasNext(): Boolean {
        return topSubStack != null || next != null
    }

    fun next() {
        if (topSubStack == null) {
            topSubStack = next!!.stack
            next = next!!.next
        }
        topSubStack = topSubStack!!.next
    }

    fun topLhs(): Buffer {
        return this.topSubStack?.lhs ?: this.next!!.stack.lhs
    }

    fun topRhs(): Buffer {
        return this.topSubStack?.rhs ?: this.next!!.stack.rhs
    }

    fun add(lhs: Buffer, rhs: Buffer) {
        if (topSubStack == null || levelColor(topSubStack!!.lhs, topSubStack!!.rhs, hasOnlyOneLevel()) == YELLOW) {
            topSubStack = LevelStack(lhs, rhs, topSubStack)
        } else {
            next = DequeSubStack(topSubStack!!, next)
            topSubStack = LevelStack(lhs, rhs, null)
        }
    }

    fun addStack(stack: LevelStack) {
        if (this.topSubStack != null) {
//            assert(levelColor(this.topSubStack!!.lhs, this.topSubStack!!.rhs, hasOnlyOneLevel()) != YELLOW)
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

    private fun levelColor(lhs: Buffer, rhs: Buffer, isBottomLevel: Boolean): Int {
        return if (isBottomLevel)
            bottomLevelColor(lhs, rhs)
        else
            nonBottomLevelColor(lhs, rhs)
    }

    private fun nonBottomLevelColor(lhs: Buffer, rhs: Buffer): Int {
        return minOf(lhs.color, rhs.color)
    }

    private fun bottomLevelColor(lhs: Buffer, rhs: Buffer): Int {
        if (lhs.isEmpty) return rhs.color
        if (rhs.isEmpty) return lhs.color
        return minOf(lhs.color, rhs.color)
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
//            val colorOfCurrentLevel = nonBottomLevelColor(topSubStack.lhs, topSubStack.rhs)
//
//            val isNextTopLevelBottomLevel = next.next == null && next.stack.next == null
//            val colorOfNextTopLevel = levelColor(next.stack.lhs, next.stack.rhs, isNextTopLevelBottomLevel)
//
//            if (isCurrentLevelTopLevel && colorOfCurrentLevel == RED) {
//                throw IllegalStateException()
//            }
//            if (isCurrentLevelTopLevel
//                    && colorOfCurrentLevel == YELLOW
//                    && colorOfNextTopLevel != GREEN) {
//                throw IllegalStateException()
//            }
//            if (colorOfNextTopLevel == YELLOW) {
//                throw IllegalStateException()
//            }
//            if (!isCurrentLevelTopLevel
//                    && colorOfCurrentLevel != GREEN
//                    && colorOfNextTopLevel != GREEN) {
//                throw IllegalStateException()
//            }
//
//            if (colorOfNextTopLevel != GREEN && colorOfNextTopLevel != RED) {
//                throw IllegalStateException()
//            }
//
//            isCurrentLevelTopLevel = false
//            topSubStack = next.stack
//            next = next.next
//        }
//    }
}