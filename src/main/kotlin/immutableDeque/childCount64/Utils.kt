package immutableDeque.childCount64

internal const val CHILD_COUNT = 64

internal fun childCountToThePow(n: Int): Int {
    return 1 shl (n * 6)
}
