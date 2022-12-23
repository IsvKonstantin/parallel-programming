package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {
    private interface Node {
        ExistingNode getNode();
    }

    private static class ExistingNode implements Node {
        AtomicRef<Node> next;
        int x;

        ExistingNode(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }

        public int getKey() {
            return x;
        }

        @Override
        public ExistingNode getNode() {
            Node node = next.getValue();
            if (node instanceof ExistingNode) {
                return (ExistingNode) node;
            } else {
                return node.getNode();
            }
        }
    }

    private static class RemovedNode implements Node {
        ExistingNode removed;

        RemovedNode(ExistingNode node) {
            this.removed = node;
        }

        @Override
        public ExistingNode getNode() {
            return removed;
        }
    }

    private static class Window {
        ExistingNode cur, next;
    }

    private final ExistingNode head = new ExistingNode(Integer.MIN_VALUE, new ExistingNode(Integer.MAX_VALUE, null));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        while (true) {
            ExistingNode cur = head;
            ExistingNode next = cur.getNode();
            boolean removed = false;

            while (next.getKey() < x) {
                Node node = next.next.getValue();
                if (node instanceof RemovedNode) {
                    if (!cur.next.compareAndSet(next, node.getNode())) {
                        removed = true;
                        break;
                    }
                    next = node.getNode();
                } else {
                    cur = next;
                    next = (ExistingNode) node;
                }
            }

            if (removed) {
                continue;
            }

            Node node = next.next.getValue();
            if (node instanceof RemovedNode) {
                cur.next.compareAndSet(next, node.getNode());
            } else {
                Window w = new Window();
                w.cur = cur;
                w.next = next;
                return w;
            }
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.getKey() == x) {
                return false;
            }

            Node node = new ExistingNode(x, w.next);
            if (w.cur.next.compareAndSet(w.next, node)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.getKey() != x) {
                return false;
            }

            Node node = w.next.next.getValue();
            if (node instanceof ExistingNode) {
                if (w.next.next.compareAndSet(node, new RemovedNode((ExistingNode) node))) {
                    w.cur.next.compareAndSet(w.next, node);
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        return w.next.getKey() == x;
    }
}