/**
 * @author Isaev Konstantin
 */
public class Solution implements AtomicCounter {
    // объявите здесь нужные вам поля
    final Node root = new Node(0);
    final ThreadLocal<Node> last = ThreadLocal.withInitial(() -> root);

    public int getAndAdd(int x) {
        // напишите здесь код
        int value;
        Node currentNode, previousNode;

        do {
            value = last.get().value;

            currentNode = new Node(value + x);
            previousNode = last.get().next.decide(currentNode);
            last.set(previousNode);

        } while (currentNode != previousNode);

        return value;
    }

    // вам наверняка потребуется дополнительный класс
    private static class Node {
        final int value;
        final Consensus<Node> next;

        private Node(int value) {
            this.value = value;
            this.next = new Consensus<>();
        }
    }
}
