import kotlinx.atomicfu.*

class FAAQueue<T> {
    private val head: AtomicRef<Segment> // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private val tail: AtomicRef<Segment> // Tail pointer, similarly to the Michael-Scott queue

    init {
        val firstNode = Segment()
        head = atomic(firstNode)
        tail = atomic(firstNode)
    }

    /**
     * Adds the specified element [x] to the queue.
     */
    fun enqueue(x: T) {
        while (true) {
            val tailSegment: Segment = tail.value
            val nextSegment: Segment? = tailSegment.next.value
            if (nextSegment != null) {
                tail.compareAndSet(tailSegment, nextSegment)
            } else {
                val enqIdx: Int = tailSegment.enqIdx.getAndIncrement()
                if (enqIdx < SEGMENT_SIZE) {
                    if (tailSegment.elements[enqIdx].compareAndSet(null, x)) {
                        return
                    }
                } else {
                    if (tailSegment == tail.value && tail.value.next.compareAndSet(null, Segment(x))) {
                        break
                    }
                }
            }
        }
    }

    /**
     * Retrieves the first element from the queue
     * and returns it; returns `null` if the queue
     * is empty.
     */
    @Suppress("UNCHECKED_CAST")
    fun dequeue(): T? {
        while (true) {
            val headSegment: Segment = head.value
            if (headSegment.isEmpty) {
                val nextSegment: Segment? = headSegment.next.value
                if (nextSegment == null) {
                    return null
                } else {
                    head.compareAndSet(headSegment, nextSegment)
                }
            } else {
                val deqIdx: Int = headSegment.deqIdx.getAndIncrement()
                if (deqIdx < SEGMENT_SIZE) {
                    val element: Any? = headSegment.elements[deqIdx].getAndSet(DONE)
                    if (element == null) {
                        continue
                    } else {
                        return element as T
                    }
                }
            }
        }
    }

    /**
     * Returns `true` if this queue is empty;
     * `false` otherwise.
     */
    val isEmpty: Boolean
        get() {
            while (true) {
                val headSegment: Segment = head.value
                val nextSegment: Segment? = headSegment.next.value
                if (headSegment.isEmpty) {
                    if (nextSegment == null) {
                        return true
                    } else {
                        head.compareAndSet(headSegment, nextSegment)
                    }
                } else {
                    return false
                }
            }
        }
}

private class Segment {
    val next = atomic<Segment?>(null)
    val enqIdx = atomic(0) // index for the next enqueue operation
    val deqIdx = atomic(0) // index for the next dequeue operation
    val elements = atomicArrayOfNulls<Any>(SEGMENT_SIZE)

    constructor() // for the first segment creation

    constructor(x: Any?) { // each next new segment should be constructed with an element
        enqIdx.incrementAndGet()
        elements[0].value = x
    }

    val isEmpty: Boolean get() = deqIdx.value >= SEGMENT_SIZE
}

private val DONE = Any() // Marker for the "DONE" slot state; to avoid memory leaks
const val SEGMENT_SIZE = 2 // DO NOT CHANGE, IMPORTANT FOR TESTS
