import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val RETRY: Boolean = false

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    private val head: AtomicReference<Node>
    private val tail: AtomicReference<Node>

    init {
        val dummy = Node()
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    private fun shouldRetry(a: Node, b: Node): Boolean {
        return !a.next.compareAndSet(null, b)
    }

    private suspend fun enqueueAndSuspend(tail: Node, node: Node): Boolean {
        if (tail == this.tail.get()) {
            val next = tail.next.get()
            if (next == null) {
                val res = suspendCoroutine<Boolean> sc@{ cont ->
                    node.continuation.set(cont)
                    if (shouldRetry(tail, node)) {
                        cont.resume(RETRY)
                        return@sc
                    } else {
                        this.tail.compareAndSet(tail, node)
                    }
                }
                return res != RETRY
            } else {
                this.tail.compareAndSet(tail, next)
            }
        }

        return false
    }

    private fun dequeueAndResume(head: Node, next: Node, element: E?): Boolean {
        if (this.head.compareAndSet(head, next)) {
            if (element != null) {
                next.element.set(element)
            }

            next.continuation.get()!!.resume(!RETRY)
            return true
        }

        return false
    }

    override suspend fun send(element: E) {
        val node = Node(element, Token.SEND)

        while (true) {
            val tail = this.tail.get()
            val head = this.head.get()
            if (head == tail || tail.token == Token.SEND) {
                if (enqueueAndSuspend(tail, node)) {
                    return
                }
            } else {
                val next = head.next.get()
                if (tail == this.tail.get() && head == this.head.get()) {
                    if (next != null) {
                        if (dequeueAndResume(head, next, element)) {
                            return
                        }
                    }
                }
            }
        }
    }

    override suspend fun receive(): E {
        val node = Node(null, Token.RECEIVE)

        while (true) {
            val tail = this.tail.get()
            val head = this.head.get()
            if (head == tail || tail.token == Token.RECEIVE) {
                if (enqueueAndSuspend(tail, node)) {
                    return node.element.get()!!
                }
            } else {
                val next = head.next.get()
                if (tail == this.tail.get() && head == this.head.get()) {
                    if (next != null) {
                        if (dequeueAndResume(head, next, null)) {
                            return next.element.get()!!
                        }
                    }
                }
            }
        }
    }

    private enum class Token {
        SEND,
        RECEIVE
    }

    private inner class Node {
        var token: Token? = null
        val next: AtomicReference<Node?> = AtomicReference(null)
        val element: AtomicReference<E?> = AtomicReference(null)
        var continuation: AtomicReference<Continuation<Boolean>?> = AtomicReference(null)

        constructor() {
            this.token = null
            this.element.set(null)
        }

        constructor(element: E?, token: Token) {
            this.token = token
            this.element.set(element)
        }
    }
}
