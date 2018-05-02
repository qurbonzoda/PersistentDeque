package immutableDeque.childCount16

internal const val CHILD_COUNT = 16

internal fun childCountToThePow(n: Int): Int {
    return 1 shl (n shl 2)
}
