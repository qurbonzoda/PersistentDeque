package immutableDeque.childCount64

internal const val CHILD_COUNT = 64
internal const val LOG_CHILD_COUNT = 6

internal fun childCountToThePow(n: Int): Int {
    return 1 shl (n * 6)
}
