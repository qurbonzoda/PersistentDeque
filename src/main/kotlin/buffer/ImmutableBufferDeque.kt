package buffer

import deque.ImmutableDeque

internal interface ImmutableBufferDeque<T>: ImmutableBuffer, ImmutableDeque<T>