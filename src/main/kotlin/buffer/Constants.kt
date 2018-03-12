package buffer

const val RED = 0
const val YELLOW = 1
const val GREEN = 2

const val MAX_BUFFER_SIZE = 25
const val RED_LOW = 0
const val RED_HIGH = MAX_BUFFER_SIZE
const val YELLOW_LOW = 1
const val YELLOW_HIGH = MAX_BUFFER_SIZE - 1
const val GREEN_LOW = 2
const val GREEN_HIGH = MAX_BUFFER_SIZE - 2

const val FULL_BUFFER_DEQUE_SHOULD_MOVE_TO_OPPOSITE_SIDE = 18   // a bit bigger than (MAX_BUFFER_SIZE / 2)
const val MIN_COUNT_FULL_BOTTOM_LEVEL_DEQUE_SHOULD_MOVE_TO_OPPOSITE_SIDE = 4
const val FULL_BOTTOM_LEVEL_DEQUE_SHOULD_MOVE_TO_NEXT_LEVEL = 16
const val FULL_UPPER_LEVEL_SHOULD_MOVE_TO_THIS_LEVEL = 16

const val EMPTY_UPPER_LEVEL_SHOULD_MOVE_FROM_THIS_LEVEL = 8     // count at this level

// YELLOW_HIGH - MIN_COUNT_FULL_BOTTOM_LEVEL_DEQUE_SHOULD_MOVE_TO_OPPOSITE_SIDE
//      > FULL_BOTTOM_LEVEL_DEQUE_SHOULD_MOVE_TO_NEXT_LEVEL
//
// 4 + MIN_COUNT_FULL_UPPER_LEVEL_SHOULD_MOVE_TO_THIS_LEVEL <= GREEN_HIGH
//
// EMPTY_UPPER_LEVEL_SHOULD_MOVE_FROM_THIS_LEVEL ~ FULL_UPPER_LEVEL_SHOULD_MOVE_TO_THIS_LEVEL / 2

//var lastRegularizationSize = 0