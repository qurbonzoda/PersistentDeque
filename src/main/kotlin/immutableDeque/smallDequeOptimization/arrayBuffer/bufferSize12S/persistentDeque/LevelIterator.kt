package immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize12S.persistentDeque

import immutableDeque.smallDequeOptimization.arrayBuffer.bufferSize12S.level.ImmutableLevel

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
