package wfc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.Random;

public class WaveFunctionCollapse extends JPanel {
    public static SimulationState STATE;
    public static Random rnd = new Random();
    private final TextureMap tm;
    private final WaveCollapseProvider collapser;
    private JFrame window;

    public static void main(String[] args) throws IOException {
        new WaveFunctionCollapse(8, "Platformer").begin();
    }

    public WaveFunctionCollapse(int dim, String tilemap) throws IOException {
        tm = TextureMap.fromFileConfig(tilemap);
        STATE = new SimulationState();
        collapser = new WaveCollapseProvider(tm);
        setupGraphics(dim * tm.getTextureWidth(), dim * tm.getTextureWidth());
        Wave.initSpace(dim, tm);
    }


    private void setupGraphics(int width, int height) {
        window = new JFrame("Wave Function Collapse");
        window.setSize(width, height);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocationRelativeTo(null);
        window.setResizable(false);
        window.addKeyListener(STATE);
        window.addKeyListener(collapser);

        this.setSize(width, height);
        this.setBackground(Color.GRAY);
        this.addMouseListener(collapser);
        this.addMouseMotionListener(collapser);
        window.add(this);
    }

    public void begin() {
        window.setVisible(true);
    }

    int frames;
    long nowMillis, lastMillis;

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        frames++;
        nowMillis = System.currentTimeMillis();
        if (nowMillis - lastMillis >= 1000) {
            lastMillis = nowMillis;
            frames = 0;
        }

        Graphics2D g2d = (Graphics2D) g;
        if (STATE.inspectTiles) {
            TextureMap.getSocketProvider().paintSockets(g2d, 50, 100, tm.realities.get(STATE.realityInspectCounter));
        } else {
            for (Wave wave : Wave.allWaves)
                wave.paint(g2d);
            collapser.paint(g2d);
        }
        repaint();
    }

    class SimulationState implements KeyListener {
        private boolean debug, inspectTiles, textureFocus;
        private int realityInspectCounter;

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int realitySize = tm.realities.size();
            switch (e.getKeyCode()) {
                case KeyEvent.VK_T:
                    textureFocus = !textureFocus;
                    break;
                case KeyEvent.VK_D:
                    debug = !debug;
                    break;
                case KeyEvent.VK_F:
                    inspectTiles = !inspectTiles;
                    break;
                case KeyEvent.VK_N:
                    realityInspectCounter = (realityInspectCounter + 1) % realitySize;
                    break;
                case KeyEvent.VK_B:
                    realityInspectCounter = (realitySize + realityInspectCounter - 1) % realitySize;
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        public boolean isInspectTiles() {
            return inspectTiles;
        }

        public boolean isDebug() {
            return debug;
        }
    }
}
