import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;

public class Test {

    public static void main(String[] args) {
        try {
            long z =
                Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory();
            new JLabel();
            long zz =
                Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory();
            long a =
                Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory();
            new JPanel();
            long b =
                Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory();
            System.out.println(b - a);
            long c =
                Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory();

            new JViewport();

            long d =
                Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory();
            System.out.println(d - c);
        } catch (Exception e) {}
    }
}
