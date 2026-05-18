import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Tablero extends JPanel implements ActionListener, KeyListener {

    private final int COLUMNAS = 10;
    private final int FILAS = 20;
    private final int TAMANO_BLOQUE = 30;
    private final int PANEL_IZQ = 150;   // Panel izquierdo (HOLD)
    private final int PANEL_DER = 160;   // Panel derecho (SIGUIENTE, stats)

    private final Color COLOR_FONDO   = new Color(15, 15, 35);
    private final Color COLOR_TABLERO = new Color(20, 20, 50);
    private final Color COLOR_GRID    = new Color(40, 40, 80);
    private final Color COLOR_PANEL   = new Color(30, 30, 65);
    private final Color COLOR_TITULO  = new Color(255, 210, 0);
    private final Color COLOR_TEXTO   = new Color(200, 200, 255);

    private int[][] matriz;
    private int[][] matrizColoresBorde;
    private Timer reloj;
    private boolean juegoTerminado = false;
    private JButton botonReiniciar;

    private int puntuacion = 0;
    private int lineasTotales = 0;
    private int nivel = 1;

    private Timer cronometro;
    private int tiempoSegundos = 0;

    private Pieza piezaActual;
    private Pieza siguientePieza;
    private Pieza piezaHold;         // Pieza guardada en HOLD
    private boolean holdUsado = false; // Solo 1 hold por turno

    private Musica musica;

    private static final int[] VELOCIDADES = {600, 480, 380, 300, 230, 170, 120, 80, 50, 30};

    public Tablero() {
        int anchoTotal = PANEL_IZQ + (COLUMNAS * TAMANO_BLOQUE) + PANEL_DER;
        this.setPreferredSize(new Dimension(anchoTotal, FILAS * TAMANO_BLOQUE));
        this.setBackground(COLOR_FONDO);
        this.setLayout(null);
        this.setFocusable(true);
        this.addKeyListener(this);

        matriz = new int[FILAS][COLUMNAS];
        matrizColoresBorde = new int[FILAS][COLUMNAS];

        // Botón reiniciar centrado en el tablero de juego
        int btnX = PANEL_IZQ + (COLUMNAS * TAMANO_BLOQUE) / 2 - 75;
        botonReiniciar = new JButton("▶ REINTENTAR");
        botonReiniciar.setBounds(btnX, 370, 150, 45);
        botonReiniciar.setFont(new Font("Arial", Font.BOLD, 13));
        botonReiniciar.setBackground(new Color(255, 210, 0));
        botonReiniciar.setForeground(new Color(20, 20, 50));
        botonReiniciar.setFocusable(false);
        botonReiniciar.setVisible(false);
        botonReiniciar.setBorderPainted(false);
        botonReiniciar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botonReiniciar.addActionListener(e -> reiniciarJuego());
        this.add(botonReiniciar);

        piezaActual = new Pieza();
        siguientePieza = new Pieza();
        piezaHold = null;

        musica = new Musica();

        reloj = new Timer(getVelocidadNivel(), this);
        reloj.start();

        cronometro = new Timer(1000, e -> {
            if (!juegoTerminado) { tiempoSegundos++; repaint(); }
        });
        cronometro.start();
    }

    private int getVelocidadNivel() {
        return VELOCIDADES[Math.min(nivel - 1, VELOCIDADES.length - 1)];
    }

    private void actualizarNivel() {
        int nuevoNivel = (lineasTotales / 10) + 1;
        if (nuevoNivel > nivel) {
            nivel = Math.min(nuevoNivel, 10);
            reloj.setDelay(getVelocidadNivel());
        }
    }

    private void reiniciarJuego() {
        matriz = new int[FILAS][COLUMNAS];
        matrizColoresBorde = new int[FILAS][COLUMNAS];
        piezaActual = new Pieza();
        siguientePieza = new Pieza();
        piezaHold = null;
        holdUsado = false;
        juegoTerminado = false;
        puntuacion = 0;
        lineasTotales = 0;
        nivel = 1;
        tiempoSegundos = 0;
        reloj.setDelay(getVelocidadNivel());
        botonReiniciar.setVisible(false);
        reloj.start();
        cronometro.start();
        repaint();
        this.requestFocusInWindow();
    }

    // === HOLD ===
    private void usarHold() {
        if (holdUsado) return; // Solo 1 vez por pieza

        if (piezaHold == null) {
            // Primera vez: guarda la actual y saca la siguiente
            piezaHold = piezaActual;
            piezaActual = siguientePieza;
            siguientePieza = new Pieza();
        } else {
            // Intercambio: actual ↔ hold
            Pieza temp = piezaHold;
            piezaHold = piezaActual;
            piezaActual = temp;
        }

        // Resetear posición de la pieza que entra
        piezaActual.setX(COLUMNAS / 2 - piezaActual.getForma()[0].length / 2);
        piezaActual.setY(0);
        holdUsado = true;

        musica.playEfecto(3); // sonido hold
        repaint();
    }

    private boolean puedeMoverse(int[][] formaPieza, int nuevaX, int nuevaY) {
        for (int f = 0; f < formaPieza.length; f++) {
            for (int c = 0; c < formaPieza[f].length; c++) {
                if (formaPieza[f][c] != 0) {
                    int col = nuevaX + c;
                    int fila = nuevaY + f;
                    if (col < 0 || col >= COLUMNAS || fila >= FILAS) return false;
                    if (fila >= 0 && matriz[fila][col] != 0) return false;
                }
            }
        }
        return true;
    }

    private void fijarPiezaYGenerarSiguiente() {
        int[][] forma = piezaActual.getForma();
        for (int f = 0; f < forma.length; f++) {
            for (int c = 0; c < forma[f].length; c++) {
                if (forma[f][c] != 0) {
                    int fila = piezaActual.getY() + f;
                    int col  = piezaActual.getX() + c;
                    if (fila >= 0) {
                        matriz[fila][col] = piezaActual.getColor().getRGB();
                        matrizColoresBorde[fila][col] = piezaActual.getColorBorde().getRGB();
                    }
                }
            }
        }

        int lineasAntes = lineasTotales;
        borrarLineasCompletas();

        // Efectos de sonido según líneas borradas
        int borradas = lineasTotales - lineasAntes;
        if (borradas == 4) musica.playEfecto(1);
        else if (borradas > 0) musica.playEfecto(0);

        // Siguiente pieza entra
        piezaActual = siguientePieza;
        siguientePieza = new Pieza();
        piezaActual.setX(COLUMNAS / 2 - piezaActual.getForma()[0].length / 2);
        piezaActual.setY(0);
        holdUsado = false; // Se puede usar hold de nuevo

        if (!puedeMoverse(piezaActual.getForma(), piezaActual.getX(), piezaActual.getY())) {
            juegoTerminado = true;
            reloj.stop();
            cronometro.stop();
            musica.playEfecto(2);
            botonReiniciar.setVisible(true);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (juegoTerminado) return;
        if (puedeMoverse(piezaActual.getForma(), piezaActual.getX(), piezaActual.getY() + 1)) {
            piezaActual.setY(piezaActual.getY() + 1);
        } else {
            fijarPiezaYGenerarSiguiente();
        }
        repaint();
    }

    private void borrarLineasCompletas() {
        int borradas = 0;
        for (int f = FILAS - 1; f >= 0; f--) {
            boolean llena = true;
            for (int c = 0; c < COLUMNAS; c++) {
                if (matriz[f][c] == 0) { llena = false; break; }
            }
            if (llena) {
                borradas++;
                for (int fs = f; fs > 0; fs--) {
                    System.arraycopy(matriz[fs - 1], 0, matriz[fs], 0, COLUMNAS);
                    System.arraycopy(matrizColoresBorde[fs - 1], 0, matrizColoresBorde[fs], 0, COLUMNAS);
                }
                java.util.Arrays.fill(matriz[0], 0);
                java.util.Arrays.fill(matrizColoresBorde[0], 0);
                f++;
            }
        }
        int[] pts = {0, 100, 300, 500, 800};
        if (borradas > 0) {
            puntuacion += pts[Math.min(borradas, 4)] * nivel;
            lineasTotales += borradas;
            actualizarNivel();
        }
    }

    private int getGhostY() {
        int gy = piezaActual.getY();
        while (puedeMoverse(piezaActual.getForma(), piezaActual.getX(), gy + 1)) gy++;
        return gy;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (juegoTerminado) return;
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT  && puedeMoverse(piezaActual.getForma(), piezaActual.getX()-1, piezaActual.getY()))
            piezaActual.setX(piezaActual.getX()-1);
        if (k == KeyEvent.VK_RIGHT && puedeMoverse(piezaActual.getForma(), piezaActual.getX()+1, piezaActual.getY()))
            piezaActual.setX(piezaActual.getX()+1);
        if (k == KeyEvent.VK_DOWN  && puedeMoverse(piezaActual.getForma(), piezaActual.getX(), piezaActual.getY()+1))
            piezaActual.setY(piezaActual.getY()+1);
        if (k == KeyEvent.VK_UP) {
            int[][] rot = piezaActual.obtenerMatrizRotada();
            if (puedeMoverse(rot, piezaActual.getX(), piezaActual.getY()))
                piezaActual.setForma(rot);
        }
        if (k == KeyEvent.VK_SPACE) {
            while (puedeMoverse(piezaActual.getForma(), piezaActual.getX(), piezaActual.getY()+1))
                piezaActual.setY(piezaActual.getY()+1);
            fijarPiezaYGenerarSiguiente();
        }
        if (k == KeyEvent.VK_C || k == KeyEvent.VK_SHIFT) {
            usarHold();
        }
        if (k == KeyEvent.VK_M) {
            musica.toggleMute();
            repaint();
        }
        repaint();
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    // ===== DIBUJO =====

    private void dibujarBloque(Graphics g, int x, int y, Color base, Color borde) {
        int s = TAMANO_BLOQUE;
        g.setColor(base);
        g.fillRect(x, y, s, s);
        g.setColor(borde);
        g.fillRect(x, y, s, 3);
        g.fillRect(x, y, 3, s);
        g.setColor(base.darker().darker());
        g.fillRect(x+s-3, y, 3, s);
        g.fillRect(x, y+s-3, s, 3);
        g.setColor(new Color(0,0,0,120));
        g.drawRect(x, y, s-1, s-1);
    }

    private void dibujarBloqueMini(Graphics g, int x, int y, Color base, Color borde) {
        int s = 22;
        g.setColor(base);
        g.fillRect(x, y, s, s);
        g.setColor(borde);
        g.fillRect(x, y, s, 2);
        g.fillRect(x, y, 2, s);
        g.setColor(base.darker().darker());
        g.fillRect(x+s-2, y, 2, s);
        g.fillRect(x, y+s-2, s, 2);
        g.setColor(new Color(0,0,0,100));
        g.drawRect(x, y, s-1, s-1);
    }

    private void dibujarRecuadro(Graphics2D g2, int x, int y, int ancho, int alto, String titulo) {
        g2.setColor(COLOR_PANEL);
        g2.fillRoundRect(x, y, ancho, alto, 10, 10);
        g2.setColor(new Color(70, 70, 140));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, ancho, alto, 10, 10);
        g2.setStroke(new BasicStroke(1));
        g2.setColor(new Color(50, 50, 110));
        g2.fillRoundRect(x, y, ancho, 20, 10, 10);
        g2.fillRect(x, y+10, ancho, 10);
        g2.setColor(COLOR_TITULO);
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(titulo, x + (ancho - fm.stringWidth(titulo))/2, y+14);
    }

    private Color nivelColor(int n) {
        if (n <= 3) return new Color(0, 200, 80);
        if (n <= 5) return new Color(200, 200, 0);
        if (n <= 7) return new Color(255, 140, 0);
        return new Color(220, 30, 30);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int tx = PANEL_IZQ; // X donde empieza el tablero
        int anchoTablero = COLUMNAS * TAMANO_BLOQUE;

        // === FONDOS ===
        g.setColor(COLOR_FONDO);
        g.fillRect(0, 0, PANEL_IZQ, getHeight());
        g.setColor(COLOR_TABLERO);
        g.fillRect(tx, 0, anchoTablero, getHeight());
        g.setColor(COLOR_FONDO);
        g.fillRect(tx + anchoTablero, 0, PANEL_DER, getHeight());

        // === CUADRÍCULA ===
        g.setColor(COLOR_GRID);
        for (int i = 0; i <= COLUMNAS; i++)
            g.drawLine(tx + i*TAMANO_BLOQUE, 0, tx + i*TAMANO_BLOQUE, getHeight());
        for (int i = 0; i <= FILAS; i++)
            g.drawLine(tx, i*TAMANO_BLOQUE, tx+anchoTablero, i*TAMANO_BLOQUE);

        // === GHOST PIECE ===
        if (!juegoTerminado && piezaActual != null) {
            int gy = getGhostY();
            if (gy != piezaActual.getY()) {
                int[][] forma = piezaActual.getForma();
                for (int f = 0; f < forma.length; f++) {
                    for (int c = 0; c < forma[f].length; c++) {
                        if (forma[f][c] != 0) {
                            int xp = tx + (piezaActual.getX()+c)*TAMANO_BLOQUE;
                            int yp = (gy+f)*TAMANO_BLOQUE;
                            Color col = piezaActual.getColor();
                            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 50));
                            g.fillRect(xp, yp, TAMANO_BLOQUE, TAMANO_BLOQUE);
                            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 120));
                            g.drawRect(xp, yp, TAMANO_BLOQUE-1, TAMANO_BLOQUE-1);
                        }
                    }
                }
            }
        }

        // === BLOQUES FIJOS ===
        for (int f = 0; f < FILAS; f++) {
            for (int c = 0; c < COLUMNAS; c++) {
                if (matriz[f][c] != 0) {
                    dibujarBloque(g, tx + c*TAMANO_BLOQUE, f*TAMANO_BLOQUE,
                            new Color(matriz[f][c]), new Color(matrizColoresBorde[f][c]));
                }
            }
        }

        // === PIEZA ACTIVA ===
        if (!juegoTerminado && piezaActual != null) {
            int[][] forma = piezaActual.getForma();
            for (int f = 0; f < forma.length; f++) {
                for (int c = 0; c < forma[f].length; c++) {
                    if (forma[f][c] != 0) {
                        dibujarBloque(g, tx + (piezaActual.getX()+c)*TAMANO_BLOQUE,
                                (piezaActual.getY()+f)*TAMANO_BLOQUE,
                                piezaActual.getColor(), piezaActual.getColorBorde());
                    }
                }
            }
        }

        // === BORDE DEL TABLERO ===
        g2.setColor(new Color(80, 80, 160));
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(tx+1, 1, anchoTablero-2, getHeight()-2);
        g2.setStroke(new BasicStroke(1));

        // ===========================
        // === PANEL IZQUIERDO: HOLD ===
        // ===========================
        int px_izq = 8;
        int pw_izq = PANEL_IZQ - 16;

        dibujarRecuadro(g2, px_izq, 10, pw_izq, 130, "HOLD  [C]");

        if (piezaHold != null) {
            int[][] fh = piezaHold.getForma();
            int cols = fh[0].length, rows = fh.length;
            int ox = px_izq + (pw_izq - cols*22)/2;
            int oy = 38 + (90 - rows*22)/2;
            // Si holdUsado, dibujamos en gris (no disponible)
            Color baseH  = holdUsado ? Color.DARK_GRAY : piezaHold.getColor();
            Color bordeH = holdUsado ? Color.GRAY      : piezaHold.getColorBorde();
            for (int f = 0; f < rows; f++)
                for (int c = 0; c < cols; c++)
                    if (fh[f][c] != 0)
                        dibujarBloqueMini(g, ox+c*22, oy+f*22, baseH, bordeH);

            if (holdUsado) {
                g.setColor(new Color(255,80,80,180));
                g.setFont(new Font("Arial", Font.BOLD, 10));
                g.drawString("NO DISP.", px_izq + 10, 128);
            }
        } else {
            g.setColor(new Color(100, 100, 150));
            g.setFont(new Font("Arial", Font.PLAIN, 11));
            FontMetrics fm = g.getFontMetrics();
            String vacio = "vacío";
            g.drawString(vacio, px_izq + (pw_izq - fm.stringWidth(vacio))/2, 80);
        }

        // Instrucción hold
        g.setColor(new Color(120,120,180));
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString("Presiona C", px_izq + 10, 152);
        g.drawString("o SHIFT", px_izq + 18, 163);

        // Separador visual
        g2.setColor(new Color(60, 60, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(PANEL_IZQ-2, 0, PANEL_IZQ-2, getHeight());
        g2.setStroke(new BasicStroke(1));

        // Controles (panel izq, abajo)
        dibujarRecuadro(g2, px_izq, 185, pw_izq, 140, "TECLAS");
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.setColor(COLOR_TEXTO);
        String[] teclas = {"← → Mover", "↑   Rotar", "↓   Bajar", "SPC Drop", "C   Hold", "M   Música"};
        for (int i = 0; i < teclas.length; i++) {
            FontMetrics fm = g.getFontMetrics();
            g.drawString(teclas[i], px_izq + (pw_izq - fm.stringWidth(teclas[i]))/2, 208 + i*19);
        }

        // ==============================
        // === PANEL DERECHO: stats ===
        // ==============================
        int px_der = tx + anchoTablero + 10;
        int pw_der = PANEL_DER - 15;

        // --- SIGUIENTE ---
        dibujarRecuadro(g2, px_der, 10, pw_der, 120, "SIGUIENTE");
        if (siguientePieza != null) {
            int[][] fs = siguientePieza.getForma();
            int cols = fs[0].length, rows = fs.length;
            int ox = px_der + (pw_der - cols*22)/2;
            int oy = 38 + (80 - rows*22)/2;
            for (int f = 0; f < rows; f++)
                for (int c = 0; c < cols; c++)
                    if (fs[f][c] != 0)
                        dibujarBloqueMini(g, ox+c*22, oy+f*22,
                                siguientePieza.getColor(), siguientePieza.getColorBorde());
        }

        // --- PUNTOS ---
        dibujarRecuadro(g2, px_der, 145, pw_der, 70, "PUNTOS");
        g.setColor(COLOR_TITULO);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        String puntos = String.valueOf(puntuacion);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(puntos, px_der + (pw_der - fm.stringWidth(puntos))/2, 196);

        // --- NIVEL ---
        dibujarRecuadro(g2, px_der, 230, pw_der, 70, "NIVEL");
        int progreso = (int)(((lineasTotales % 10) / 10.0) * (pw_der - 16));
        g.setColor(new Color(40,40,80));
        g.fillRoundRect(px_der+7, 278, pw_der-14, 10, 5, 5);
        g.setColor(nivelColor(nivel));
        g.fillRoundRect(px_der+7, 278, progreso, 10, 5, 5);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.setColor(nivelColor(nivel));
        String nivelStr = String.valueOf(nivel);
        fm = g.getFontMetrics();
        g.drawString(nivelStr, px_der + (pw_der - fm.stringWidth(nivelStr))/2, 274);

        // --- LÍNEAS ---
        dibujarRecuadro(g2, px_der, 315, pw_der, 60, "LÍNEAS");
        g.setColor(COLOR_TEXTO);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        String lineas = String.valueOf(lineasTotales);
        fm = g.getFontMetrics();
        g.drawString(lineas, px_der + (pw_der - fm.stringWidth(lineas))/2, 356);

        // --- TIEMPO ---
        dibujarRecuadro(g2, px_der, 390, pw_der, 60, "TIEMPO");
        String tiempo = String.format("%02d:%02d", tiempoSegundos/60, tiempoSegundos%60);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        fm = g.getFontMetrics();
        g.drawString(tiempo, px_der + (pw_der - fm.stringWidth(tiempo))/2, 431);

        // --- MÚSICA ---
        dibujarRecuadro(g2, px_der, 465, pw_der, 45, "MÚSICA  [M]");
        String estadoMusica = musica.isActiva() ? "🔊 ON" : "🔇 OFF";
        g.setColor(musica.isActiva() ? new Color(0,200,80) : new Color(180,60,60));
        g.setFont(new Font("Arial", Font.BOLD, 16));
        fm = g.getFontMetrics();
        g.drawString(estadoMusica, px_der + (pw_der - fm.stringWidth(estadoMusica))/2, 491);

        // === GAME OVER ===
        if (juegoTerminado) {
            g2.setColor(new Color(0,0,0,190));
            g2.fillRect(tx, 0, anchoTablero, getHeight());
            g2.setColor(new Color(30,30,70,230));
            g2.fillRoundRect(tx+20, 240, anchoTablero-40, 150, 20, 20);
            g2.setColor(new Color(220,30,30));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(tx+20, 240, anchoTablero-40, 150, 20, 20);
            g2.setStroke(new BasicStroke(1));

            g.setColor(new Color(220,30,30));
            g.setFont(new Font("Arial", Font.BOLD, 32));
            fm = g.getFontMetrics();
            g.drawString("GAME OVER", tx + (anchoTablero - fm.stringWidth("GAME OVER"))/2, 290);

            g.setColor(COLOR_TITULO);
            g.setFont(new Font("Arial", Font.BOLD, 15));
            String fs = "Puntos: " + puntuacion + "  |  Nivel: " + nivel;
            fm = g.getFontMetrics();
            g.drawString(fs, tx + (anchoTablero - fm.stringWidth(fs))/2, 320);

            g.setColor(COLOR_TEXTO);
            g.setFont(new Font("Arial", Font.PLAIN, 13));
            String ls = "Líneas completadas: " + lineasTotales;
            fm = g.getFontMetrics();
            g.drawString(ls, tx + (anchoTablero - fm.stringWidth(ls))/2, 345);

            String ts = "Tiempo: " + String.format("%02d:%02d", tiempoSegundos/60, tiempoSegundos%60);
            fm = g.getFontMetrics();
            g.drawString(ts, tx + (anchoTablero - fm.stringWidth(ts))/2, 365);
        }
    }
}