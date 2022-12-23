import org.jetbrains.annotations.NotNull;

/**
 * В теле класса решения разрешено использовать только финальные переменные типа RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author Isaev Konstantin
 */
public class Solution implements MonotonicClock {
    private final RegularInt c1 = new RegularInt(0);
    private final RegularInt c2 = new RegularInt(0);
    private final RegularInt c3 = new RegularInt(0);
    private final RegularInt c1copy = new RegularInt(0);
    private final RegularInt c2copy = new RegularInt(0);
    private final RegularInt c3copy = new RegularInt(0);

    @Override
    public void write(@NotNull Time time) {
        // ->
        c1copy.setValue(time.getD1());
        c2copy.setValue(time.getD2());
        c3copy.setValue(time.getD3());

        // <-
        c3.setValue(time.getD3());
        c2.setValue(time.getD2());
        c1.setValue(time.getD1());
    }

    @NotNull
    @Override
    public Time read() {
        // ->
        int r1 = c1.getValue();
        int r2 = c2.getValue();
        int r3 = c3.getValue();

        // <-
        int r3copy = c3copy.getValue();
        int r2copy = c2copy.getValue();
        int r1copy = c1copy.getValue();

        if (r1 == r1copy && r2 == r2copy) {
            return new Time(r1, r2, r3);
        }

        if (r1 == r1copy) {
            return new Time(r1, r2copy, 0);
        }

        return new Time(r1copy, 0, 0);
    }
}
