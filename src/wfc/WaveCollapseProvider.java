package wfc;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import static wfc.WaveFunctionCollapse.rnd;

public class WaveCollapseProvider implements MouseListener, MouseMotionListener, KeyListener {
    private final TextureMap tm;
    private Wave mouseSelectedWave;
    private int mouseSelectedX, mouseSelectedY;
    private final int w;

    public WaveCollapseProvider(TextureMap tm) {
        this.tm = tm;
        mouseSelectedWave = null;
        w = tm.getTextureWidth();
    }

    public void paint(Graphics2D g2d) {
        if (mouseSelectedWave == null) return;
        int cols = tm.getTilemapCols(), rows = tm.getTilemapRows();
        g2d.setColor(Color.RED);
        g2d.translate(mouseSelectedWave.getX() * w, mouseSelectedWave.getY() * w);
        g2d.drawRect(0, 0, w, w);
        g2d.scale((double) 1 / cols, (double) 1 / rows);
        g2d.drawRect(mouseSelectedX * w, mouseSelectedY * w, w, w);
        g2d.scale(cols, rows);
        g2d.translate(-mouseSelectedWave.getX() * w, -mouseSelectedWave.getY() * w);
    }

    public void collapseRandomWave() {
        Integer lowestRealities = null;
        for (Wave w : Wave.allWaves) {
            if (w.isCollapsed()) continue;
            int s = w.realities.size();
            if (lowestRealities == null || s < lowestRealities)
                lowestRealities = s;
        }
        if (lowestRealities == null) return;
        System.out.format("Lowest entropy: %d%n", lowestRealities);
        ArrayList<Wave> nextOne = new ArrayList<>();
        for (Wave w : Wave.allWaves) {
            if (w.realities.size() == lowestRealities && !w.isCollapsed())
                nextOne.add(w);
        }
        int i = (int) Math.floor(rnd.nextDouble() * nextOne.size());
        Wave w = nextOne.get(i);
        System.out.format("Chose Wave %s%n", w);
        w.collapse();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (mouseSelectedWave == null) return;
        System.out.println(e.getButton());
        if (e.getButton() == MouseEvent.BUTTON3) {
            mouseSelectedWave.collapseInto("Empty");
            return;
        }
        if (e.getButton() == MouseEvent.BUTTON1)
            mouseSelectedWave.collapseToCoords(mouseSelectedX, mouseSelectedY);
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        setMouseselectedWave(e);
        final int MB3Down = MouseEvent.getMaskForButton(MouseEvent.BUTTON3);
        final int MB1Down = MouseEvent.getMaskForButton(MouseEvent.BUTTON1);

        if (!mouseSelectedWave.isCollapsed()) {
            if ((e.getModifiersEx() & MB3Down) != 0)
                mouseSelectedWave.collapseInto("Empty");
            else if ((e.getModifiersEx() & MB1Down) != 0)
                mouseSelectedWave.collapse();
        }
    }



    private void setMouseselectedWave(MouseEvent e) {
        int i = e.getX() / w, j = e.getY() / w;
        mouseSelectedWave = Wave.at(i, j);
        if (mouseSelectedWave == null || mouseSelectedWave.isCollapsed()) return;
        int _i = e.getX() % w, _j = e.getY() % w;
        mouseSelectedX = (tm.getTilemapCols() * _i) / w;
        mouseSelectedY = (tm.getTilemapRows() * _j) / w;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        setMouseselectedWave(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_SPACE)
            collapseRandomWave();
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

}
