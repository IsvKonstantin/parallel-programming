package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private final AtomicRef<Node> head;
    private final AtomicRef<Node> tail;

    public MSQueue() {
        Node dummy = new Node(0);

        this.head = new AtomicRef<>(dummy);
        this.tail = new AtomicRef<>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x);

        while (true) {
            Node currentTail = tail.getValue();

            if (currentTail.next.compareAndSet(null, newTail)) {
                tail.compareAndSet(currentTail, newTail);

                return;
            } else {
                tail.compareAndSet(currentTail, currentTail.next.getValue());
            }
        }
    }

    @Override
    public int dequeue() {
        int value;

        while (true) {
            Node currentHead = head.getValue();
            Node currentTail = tail.getValue();
            Node currentNext = currentHead.next.getValue();

            //if (currentHead == head.getValue()) {
                if (currentHead == currentTail) {
                    if (currentNext == null) {
                        return Integer.MIN_VALUE;
                    }

                    tail.compareAndSet(currentTail, currentNext);
                } else {
                    value = currentNext.x;

                    if (head.compareAndSet(currentHead, currentNext)) {
                        break;
                    }
                }
            //}
        }

        return value;
    }

    @Override
    public int peek() {
        Node next = head.getValue().next.getValue();

        if (next == null) {
            return Integer.MIN_VALUE;
        }

        return next.x;
    }

    private static class Node {
        final int x;
        final AtomicRef<Node> next;

        Node(int x) {
            this.x = x;
            this.next = new AtomicRef<>(null);
        }
    }
}