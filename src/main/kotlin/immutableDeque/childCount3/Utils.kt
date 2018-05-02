package immutableDeque.childCount3

internal const val CHILD_COUNT = 3

internal fun childCountToThePow(n: Int): Int {
    var result = 1
    repeat(n) {
        result *= CHILD_COUNT
    }
    return result
}
