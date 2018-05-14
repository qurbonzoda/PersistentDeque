package immutableDeque.childCount16

internal const val CHILD_COUNT = 16
internal const val LOG_CHILD_COUNT = 4

internal fun childCountToThePow(n: Int): Int {
    return 1 shl (n shl 2)
}
