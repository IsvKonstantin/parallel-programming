import kotlinx.atomicfu.*

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val resizeConst: Int = 2
    private val atomicCore: AtomicRef<Core<E>> = atomic(Core(INITIAL_CAPACITY))
    private val atomicSize: AtomicInt = atomic(0)

    override fun get(index: Int): E {
        if (index < size) {
            while (true) {
                val core = atomicCore.value
                val node = core.elements[index].value!!
                val element = node.getValue()

                when (node.getType()) {
                    "ordinary" -> {
                        return element
                    }
                    "fixed" -> {
                        val newNode = OrdinaryNode(element)
                        if (core.next.value!!.elements[index].compareAndSet(null, newNode)) {
                            return element
                        }
                    }
                }
            }
        } else {
            throw IllegalArgumentException("Index exceeds the size of the array")
        }
    }

    override fun put(index: Int, element: E) {
        if (index < size) {
            while (true) {
                val core = atomicCore.value
                val node = core.elements[index].value!!
                val newNode = OrdinaryNode(element)

                when (node.getType()) {
                    "ordinary" -> {
                        if (core.elements[index].compareAndSet(node, newNode)) {
                            return
                        }
                    }
                    "fixed" -> {
                        if (core.next.value!!.elements[index].compareAndSet(null, newNode)) {
                            return
                        }
                    }
                }
            }
        } else {
            throw IllegalArgumentException("Index exceeds the size of the array")
        }
    }

    private fun ensureCapacity(core: Core<E>) {
        if (core.next.compareAndSet(null, Core(core.capacity * resizeConst))) {
            var index = 0
            var flag = true

            while (index < core.capacity) {
                val nodeRef = core.elements[index]

                while (flag) {
                    val node = nodeRef.value
                    val element = node!!.getValue()

                    if (nodeRef.compareAndSet(node, FixedNode(element))) {
                        flag = false
                        core.next.value!!.elements[index].compareAndSet(null, OrdinaryNode(element))
                    }
                }

                flag = true
                index += 1
            }

            atomicCore.compareAndSet(atomicCore.value, atomicCore.value.next.value!!)
        }
    }

    override fun pushBack(element: E) {
        while (true) {
            val core = atomicCore.value
            val capacity = size

            if (capacity < core.capacity) {
                if (core.elements[capacity].compareAndSet(null, OrdinaryNode(element))) {
                    atomicSize.compareAndSet(capacity, capacity + 1)

                    return
                }
            } else {
                ensureCapacity(core)
            }
        }
    }

    override val size: Int
        get() {
            return atomicSize.value
        }
}

interface Node<E> {
    fun getType(): String

    fun getValue(): E
}

class OrdinaryNode<E>(private val element: E) : Node<E> {
    override fun getType(): String {
        return "ordinary"
    }

    override fun getValue(): E {
        return element
    }
}

class FixedNode<E>(private val element: E) : Node<E> {
    override fun getType(): String {
        return "fixed"
    }

    override fun getValue(): E {
        return element
    }
}

private class Core<E>(val capacity: Int) {
    val elements: AtomicArray<Node<E>?> = atomicArrayOfNulls(capacity)
    val next: AtomicRef<Core<E>?> = atomic(null)
}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME