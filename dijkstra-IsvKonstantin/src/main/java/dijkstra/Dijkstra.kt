package dijkstra

import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.concurrent.thread
import kotlin.random.Random

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> o1!!.distance.compareTo(o2!!.distance) }

fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()

    start.distance = 0
    val q = MultiQueuePQ(workers, NODE_DISTANCE_COMPARATOR)
    q.add(start)

    val onFinish = Phaser(workers + 1)
    val activeNodes = AtomicInteger(1)

    repeat(workers) {
        thread {
            while (true) {
                val cur: Node = q.poll() ?: if (activeNodes.get() == 0) break else continue
                for (e in cur.outgoingEdges) {
                    while (true) {
                        val newDistance = e.weight + cur.distance
                        val oldDistance = e.to.distance

                        if (oldDistance > newDistance) {
                            if (e.to.casDistance(oldDistance, newDistance)) {
                                q.add(e.to)
                                activeNodes.incrementAndGet()
                                break
                            }
                        } else break
                    }
                }
                activeNodes.decrementAndGet()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}

class MultiQueuePQ(workers: Int, comparator: Comparator<Node>) {
    private val n = 2 * workers
    private val queues = Collections.nCopies(n, PriorityQueue(comparator))

    fun poll(): Node? {
        var firstQueueIndex = getRandomIndex()
        var secondQueueIndex = getRandomIndex()

        while (firstQueueIndex == secondQueueIndex) {
            firstQueueIndex = getRandomIndex()
            secondQueueIndex = getRandomIndex()
        }

        val firstQueue = queues[firstQueueIndex]
        val secondQueue = queues[secondQueueIndex]

        synchronized(firstQueue) {
            synchronized(secondQueue) {
                val firstNode = firstQueue.peek()
                val secondNode = secondQueue.peek()

                if (firstNode != null && secondNode != null) {
                    return if (firstNode.distance < secondNode.distance) {
                        firstQueue.poll()
                    } else {
                        secondQueue.poll()
                    }
                }

                return when {
                    firstNode == null -> secondQueue.poll()
                    secondNode == null -> firstQueue.poll()
                    else -> null
                }
            }
        }
    }

    fun add(node: Node) {
        val queue = queues[getRandomIndex()]
        synchronized(queue) {
            queue.add(node)
        }
    }

    private fun getRandomIndex(): Int {
        return Random.nextInt(n)
    }
}