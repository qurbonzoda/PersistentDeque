package immutableDeque.childCount8

internal const val CHILD_COUNT = 8
internal const val LOG_CHILD_COUNT = 3

internal fun childCountToThePow(n: Int): Int {
    return 1 shl (n * 3)
}
