import java.util.concurrent.atomic.*;

public class Solution implements Lock<Solution.Node> {
    private final Environment env;
    private final AtomicReference<Node> tail;

    public Solution(Environment env) {
        this.env = env;
        this.tail = new AtomicReference<>(null);
    }

    @Override
    public Node lock() {
        Node my = new Node(); // сделали узел
        my.locked.set(true);
        Node pred = tail.getAndSet(my);
        if (pred != null) {
            pred.next.set(my);

            while (my.locked.get()) {
                env.park();
            }
        }

        return my; // вернули узел
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void unlock(Node node) {
        if (node.next.get() == null) {
            if (tail.compareAndSet(node, null)) {
                return;
            } else {
                while (node.next.get() == null) {
                }
            }
        }

        node.next.get().locked.set(false);
        env.unpark(node.next.get().thread);
    }

    static class Node {
        final Thread thread = Thread.currentThread(); // запоминаем поток, которые создал узел
        final AtomicReference<Node> next = new AtomicReference<>();
        final AtomicReference<Boolean> locked = new AtomicReference<>(false);
    }
}
