import java.awt.Color;
import java.util.Random;

public class Pieza {
    private int[][] forma;
    private Color color;
    private Color colorBorde;
    private int indice;
    private int x;
    private int y;

    private static final int[][][] CONFIGURACIONES = {
            {{0,0,0,0},{1,1,1,1},{0,0,0,0},{0,0,0,0}}, // I
            {{1,0,0},{1,1,1},{0,0,0}},                 // J
            {{0,0,1},{1,1,1},{0,0,0}},                 // L
            {{1,1},{1,1}},                             // O
            {{0,1,1},{1,1,0},{0,0,0}},                 // S
            {{0,1,0},{1,1,1},{0,0,0}},                 // T
            {{1,1,0},{0,1,1},{0,0,0}}                  // Z
    };

    private static final Color[] COLORES = {
            new Color(0, 220, 220),
            new Color(30, 100, 210),
            new Color(255, 140, 0),
            new Color(240, 200, 0),
            new Color(0, 190, 0),
            new Color(155, 0, 200),
            new Color(220, 30, 30)
    };

    private static final Color[] COLORES_BORDE = {
            new Color(120, 255, 255),
            new Color(90, 160, 255),
            new Color(255, 200, 80),
            new Color(255, 240, 120),
            new Color(80, 255, 80),
            new Color(210, 100, 255),
            new Color(255, 100, 100)
    };

    public Pieza() {
        Random aleatorio = new Random();
        this.indice = aleatorio.nextInt(CONFIGURACIONES.length);
        this.forma = CONFIGURACIONES[this.indice];
        this.color = COLORES[this.indice];
        this.colorBorde = COLORES_BORDE[this.indice];
        this.x = 10 / 2 - this.forma[0].length / 2;
        this.y = 0;
    }

    public int[][] obtenerMatrizRotada() {
        int n = forma.length;
        int[][] rotada = new int[n][n];
        for (int f = 0; f < n; f++)
            for (int c = 0; c < n; c++)
                rotada[c][n - 1 - f] = forma[f][c];
        return rotada;
    }

    public int[][] getForma() { return forma; }
    public void setForma(int[][] forma) { this.forma = forma; }
    public Color getColor() { return color; }
    public Color getColorBorde() { return colorBorde; }
    public int getIndice() { return indice; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
}