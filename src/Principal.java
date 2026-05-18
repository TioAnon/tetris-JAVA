import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Principal {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame("Tetris");
            Tablero tablero = new Tablero();
            ventana.add(tablero);
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.setResizable(false);
            ventana.pack();
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
            tablero.requestFocusInWindow();
        });
    }
}