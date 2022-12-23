package stack;

import kotlinx.atomicfu.AtomicRef;

import java.util.ArrayList;
import java.util.List;

public class StackImpl implements Stack {
    private static final int ARRAY_SIZE = 20;
    private static final int LOOKUP_DISTANCE = 10;
    private static final int SPIN_TIME = 30;

    private final List<AtomicRef<Integer>> eliminationArray;
    private AtomicRef<Node> head;

    public StackImpl() {
        head = new AtomicRef<>(null);
        eliminationArray = new ArrayList<>(ARRAY_SIZE);

        for (int i = 0; i < ARRAY_SIZE; i++) {
            eliminationArray.add(new AtomicRef<Integer>(null));
        }
    }

    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    private boolean spinWait(int index, Integer value) {
        for (int t = 0; t < SPIN_TIME; t++) {
            Integer current = eliminationArray.get(index).getValue();

            if (current == null || !current.equals(value)) {
                return true;
            }
        }

        return !eliminationArray.get(index).compareAndSet(value, null);
    }

    @Override
    public void push(int x) {
        int index = (int) (Math.random() * (ARRAY_SIZE));
        int lb = Math.min(index, (index + LOOKUP_DISTANCE) % ARRAY_SIZE);
        int ub = Math.max(index, (index + LOOKUP_DISTANCE) % ARRAY_SIZE);

        for (int i = lb; i <= ub; i++) {
            Integer value = x;

            if (eliminationArray.get(i).compareAndSet(null, value)) {
                if (spinWait(i, value)) {
                    return;
                } else {
                    break;
                }
            }
        }

        while (true) {
            Node currentHead = head.getValue();
            Node newHead = new Node(x, currentHead);

            if (head.compareAndSet(currentHead, newHead)) {
                return;
            }
        }
    }

    @Override
    public int pop() {
        int index = (int) (Math.random() * (ARRAY_SIZE));
        int lb = Math.min(index, (index + LOOKUP_DISTANCE) % ARRAY_SIZE);
        int ub = Math.max(index, (index + LOOKUP_DISTANCE) % ARRAY_SIZE);

        for (int i = lb; i <= ub; i++) {
            Integer value = eliminationArray.get(i).getValue();

            if (value != null && eliminationArray.get(i).compareAndSet(value, null)) {
                return value;
            }
        }

        while (true) {
            Node currentHead = head.getValue();

            if (currentHead == null) {
                return Integer.MIN_VALUE;
            }
            if (head.compareAndSet(currentHead, currentHead.next.getValue())) {
                return currentHead.x;
            }
        }
    }
}