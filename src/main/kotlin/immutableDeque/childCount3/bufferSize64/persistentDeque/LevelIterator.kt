package immutableDeque.childCount3.bufferSize64.persistentDeque

import immutableDeque.childCount3.bufferSize64.level.ImmutableLevel

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
