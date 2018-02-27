package persistentDeque

import level.ImmutableLevel

internal class LevelIterator(private var topSubStack: ImmutableLevel?, private var next: DequeSubStack?) {
    fun hasNext(): Boolean {
        return this.topSubStack != null || this.next != null
    }

    fun next(): ImmutableLevel {
        if (this.topSubStack == null) {
            this.topSubStack = this.next!!.stack
            this.next = next!!.next
        }
        val result = this.topSubStack!!
        this.topSubStack = this.topSubStack!!.next
        return result

    }
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