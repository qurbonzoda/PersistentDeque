package immutableDeque.childCount4

internal const val CHILD_COUNT = 4

internal fun childCountToThePow(n: Int): Int {
    return 1 shl (n shl 1)
}
