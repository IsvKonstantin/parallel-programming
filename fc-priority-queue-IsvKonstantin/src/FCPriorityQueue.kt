import kotlinx.atomicfu.AtomicArray
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import java.util.*

class FCPriorityQueue<E : Comparable<E>> {
    private val core: Core<E> = Core()

    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns null if the queue is empty.
     */
    fun poll(): E? {
        while (true) {
            val i = core.getRandomIndex()
            val currentTask: Task<E> = core.tasks[i].value!!
            val pollTask: Task<E> = Task(Token.POLL, null)
            val emptyTask: Task<E> = Task(Token.EMPTY, null)

            if (currentTask.token == Token.EMPTY) {
                if (core.tasks[i].compareAndSet(currentTask, pollTask)) {
                    while (true) {
                        val task = core.tasks[i].value!!
                        val element: E?

                        if (core.tryLock()) {
                            val previousTask = core.tasks[i].value!!
                            core.tasks[i].compareAndSet(task, emptyTask)
                            element = if (previousTask.token != Token.HELPED) core.queue.poll() else previousTask.value
                            helpOthers()

                            return element
                        } else if (task.token == Token.HELPED) {
                            element = task.value
                            core.tasks[i].compareAndSet(task, emptyTask)

                            return element
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the element with the highest priority
     * or null if the queue is empty.
     */
    fun peek(): E? {
        return core.queue.peek()
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        while (true) {
            val i = core.getRandomIndex()
            val currentTask: Task<E> = core.tasks[i].value!!
            val addTask: Task<E> = Task(Token.ADD, element)
            val emptyTask: Task<E> = Task(Token.EMPTY, null)

            if (currentTask.token == Token.EMPTY) {
                if (core.tasks[i].compareAndSet(currentTask, addTask)) {
                    while (true) {
                        val task = core.tasks[i].value!!

                        if (core.tryLock()) {
                            val previousTask = core.tasks[i].value!!
                            core.tasks[i].compareAndSet(task, emptyTask)
                            if (previousTask.token != Token.HELPED) core.queue.add(element)
                            helpOthers()

                            return
                        } else if (task.token == Token.HELPED) {
                            core.tasks[i].compareAndSet(task, emptyTask)

                            return
                        }
                    }
                }
            }
        }
    }

    private fun helpOthers() {
        (0 until core.size).forEach { i ->
            val task = core.tasks[i].value!!
            when (task.token) {
                Token.POLL -> {
                    val newTask: Task<E> = Task(Token.HELPED, core.queue.poll())
                    core.tasks[i].compareAndSet(task, newTask)
                }
                Token.ADD -> {
                    val newTask: Task<E> = Task(Token.HELPED, null)
                    core.queue.add(task.value)
                    core.tasks[i].compareAndSet(task, newTask)
                }
                else -> return@forEach
            }
        }

        core.unlock()
    }

    enum class Token {
        EMPTY,
        HELPED,
        POLL,
        ADD,
    }

    class Task<E>(val token: Token, val value: E?)

    private class Core<E> {
        val size: Int = 10
        val locked: AtomicBoolean = atomic(false)
        val queue: PriorityQueue<E> = PriorityQueue<E>()
        val tasks: AtomicArray<Task<E>?> = atomicArrayOfNulls(this.size)

        init {
            (0 until this.size).forEach { i ->
                this.tasks[i].compareAndSet(null, Task(Token.EMPTY, null))
            }
        }

        fun tryLock(): Boolean {
            return this.locked.compareAndSet(expect = false, update = true)
        }

        fun unlock() {
            this.locked.compareAndSet(expect = true, update = false)
        }

        fun getRandomIndex(): Int {
            return Random().nextInt(this.size)
        }
    }
}