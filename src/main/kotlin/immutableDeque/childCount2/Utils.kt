package immutableDeque.childCount2

internal const val CHILD_COUNT = 2
internal const val LOG_CHILD_COUNT = 1

internal fun childCountToThePow(n: Int): Int {
    return 1 shl n
}
