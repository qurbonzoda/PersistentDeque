package immutableDeque.childCount3.bufferSize16.constants

internal const val MAX_BUFFER_SIZE = 16
internal const val RED_LOW = 0
internal const val RED_HIGH = MAX_BUFFER_SIZE
internal const val YELLOW_LOW = 1
internal const val YELLOW_HIGH = MAX_BUFFER_SIZE - 1
internal const val GREEN_LOW = 2
internal const val GREEN_HIGH = MAX_BUFFER_SIZE - 2

internal const val FULL_BUFFER_DEQUE_SHOULD_MOVE_TO_OPPOSITE_SIDE = 12   // a bit bigger than (MAX_BUFFER_SIZE / 2)
internal const val MIN_COUNT_FULL_BOTTOM_LEVEL_DEQUE_SHOULD_MOVE_TO_OPPOSITE_SIDE = 4
internal const val FULL_BOTTOM_LEVEL_DEQUE_SHOULD_MOVE_TO_NEXT_LEVEL = 9
internal const val FULL_UPPER_LEVEL_SHOULD_MOVE_TO_THIS_LEVEL = 12       // k * CHILD_COUNT, k > 1

internal const val EMPTY_UPPER_LEVEL_SHOULD_MOVE_FROM_THIS_LEVEL = 4     // count at this level

// YELLOW_HIGH - MIN_COUNT_FULL_BOTTOM_LEVEL_DEQUE_SHOULD_MOVE_TO_OPPOSITE_SIDE
//      > FULL_BOTTOM_LEVEL_DEQUE_SHOULD_MOVE_TO_NEXT_LEVEL
//
// EMPTY_UPPER_LEVEL_SHOULD_MOVE_FROM_THIS_LEVEL ~ FULL_UPPER_LEVEL_SHOULD_MOVE_TO_THIS_LEVEL / 2

//var lastRegularizationSize = 0
