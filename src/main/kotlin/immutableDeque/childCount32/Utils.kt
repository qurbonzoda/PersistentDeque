package immutableDeque.childCount32

internal const val CHILD_COUNT = 32

internal fun childCountToThePow(n: Int): Int {
    return 1 shl (n * 5)
}
