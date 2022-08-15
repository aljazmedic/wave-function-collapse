package wfc;

import java.awt.*;
import java.util.List;
import java.util.*;

import static wfc.WaveFunctionCollapse.STATE;
import static wfc.WaveFunctionCollapse.rnd;

public class Wave {
    private static Wave[][] grid;
    static final int TOP = 0, RIGHT = 1, BOTTOM = 2, LEFT = 3;
    static Map<Integer, Color> cmap = new HashMap<>();
    static TextureMap tm;
    static Iterable<Wave> allWaves = WaveIterator::new;

    List<Reality> realities;
    List<Map<Integer, Integer>> edgeSocketSet;
    private final int x, y, w;
    int lastIterPropagation;

    static Color getRNDColor() {
        return Color.getHSBColor((float) rnd.nextDouble(), 0.5f, 0.5f);
    }

    static void initSpace(int dim, TextureMap tm) {
        Wave.tm = tm;
        Wave.grid = new Wave[dim][dim];
        for (int i = 0; i < dim; i++)
            for (int j = 0; j < dim; j++) {
                Wave.grid[j][i] = tm.getWholeWave(i, j);
            }
    }

    static Wave at(int x, int y) {
        if (x < 0 || y < 0) return null;
        if (x >= grid.length || y >= grid.length) return null;
        return grid[y][x];
    }

    public Wave(int x, int y, int w, List<Reality> realities) {
        this.realities = new ArrayList<>(realities);
        assert !this.realities.isEmpty();
        this.x = x;
        this.y = y;
        this.w = w;
        lastIterPropagation = 1;
        edgeSocketSet = new ArrayList<>(
                List.of(new TreeMap<>(), new TreeMap<>(), new TreeMap<>(), new TreeMap<>())
        );
        updateEdgeSockets();
    }

    private void updateEdgeSockets() {
        for (Map<Integer, Integer> t : edgeSocketSet)
            t.clear();

        for (Reality r : realities)
            for (int i = 0; i < 4; i++) {
                Map<Integer, Integer> edgeSpecimens = edgeSocketSet.get(i);
                Integer numSpecimens = edgeSocketSet.get(i).getOrDefault(r.sockets[i], 0);
                edgeSpecimens.put(r.sockets[i], numSpecimens + 1);
            }
    }

    Color getWriteColor() {
        if (!cmap.containsKey(lastIterPropagation)) {
            cmap.put(lastIterPropagation, getRNDColor());
        }
        return cmap.get(lastIterPropagation);
    }


    public Wave neighbour(int edge) {
        switch (edge) {
            case TOP:
                return Wave.at(x, y - 1);
            case RIGHT:
                return Wave.at(x + 1, y);
            case BOTTOM:
                return Wave.at(x, y + 1);
            case LEFT:
                return Wave.at(x - 1, y);
        }
        return null;
    }

    public void collapseToCoords(int x, int y) {
        for (Reality r : realities) {
            TextureMap.Texture t = r.getTexture();
            if (t.getSourceX() == x && t.getSourceY() == y) {
                collapseInto(r);
            }
        }
    }

    private void removeReality(Reality rel) {
        this.realities.remove(rel);
        for (int edge = 0; edge < 4; edge++) {
            int specimen = rel.sockets[edge];
            Map<Integer, Integer> edgeSpecimens = edgeSocketSet.get(edge);
            Integer specimensNum = edgeSpecimens.getOrDefault(specimen, 1);
            specimensNum--;
            if (specimensNum == 0)
                edgeSpecimens.remove(specimen);
            else
                edgeSpecimens.put(specimen, specimensNum);
        }
    }


    private boolean dropNonMatching(Set<Integer> allowedSockets, int side) {
        if (isCollapsed()) return false;
        int numRels = this.realities.size();
        boolean changed = false;
        //System.out.format("Started %d %d with %d%n",x,y,numRels);
        for (int i = numRels - 1; i >= 0; i--) {
            Reality r = realities.get(i);
            if (!allowedSockets.contains(r.sockets[side])) {
                this.realities.remove(r);
                changed = true;
            }
        }
        numRels = this.realities.size();
        if (numRels == 0)
            System.out.println("Ended with !!!!!!!!!!!!!!!!!!!!!!! " + numRels);
        updateEdgeSockets();
        return changed;
    }


    private String getSocketSetForEdge(int edge) {
        Set<Integer> allowed = this.edgeSocketSet.get(edge).keySet();
        return TextureMap.getSocketProvider().format(allowed);
    }

    void paintSockets(Graphics2D g) {
        g.drawString(getSocketSetForEdge(0), (0.4f + x) * w, (0.2f + y) * w);
        g.drawString(getSocketSetForEdge(1), (0.9f + x) * w, (0.6f + y) * w);
        g.drawString(getSocketSetForEdge(2), (0.4f + x) * w, (0.8f + y) * w);
        g.drawString(getSocketSetForEdge(3), (0.1f + x) * w, (0.6f + y) * w);
    }

    public void paintUncertain(Graphics2D g) {
        // Draw inCellGrid
        Iterator<Reality> iter = realities.iterator();
        int cols = tm.getTilemapCols(), rows = tm.getTilemapRows();
        g.scale((double) 1 / cols, (double) 1 / rows);
        Reality r;
        while (iter.hasNext()) {
            r = iter.next();
            TextureMap.Texture t = r.getTexture();
            g.drawImage(t.getImage(), (x * cols + t.getSourceX()) * w, (y * rows + t.getSourceY()) * w, null);
        }
        g.scale(cols, rows);
    }

    public void paint(Graphics2D g) {
        if (isCollapsed()) {
            Reality rel = realities.get(0);
            g.drawImage(rel.getImage(), x * w, y * w, null);
        } else {
            paintUncertain(g);
        }
        if (STATE.isDebug()) {
            g.setColor(getWriteColor());
            g.setFont(new Font("Roboto", Font.PLAIN, 30));
            paintSockets(g);
            g.drawString(String.format("(%d,%d):%d", this.x, this.y, this.realities.size()), (0.2f + x) * w, (0.5f + y) * w);
        }
    }

    @Override
    public String toString() {
        return "Wave{" +
                "realities=" + realities.size() +
                ", x=" + x +
                ", y=" + y +
                '}';
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Reality collapseInto(Reality r) {
        if(isCollapsed()) return realities.get(0);
        this.realities = new ArrayList<>(List.of(r));
        updateEdgeSockets();
        propagateChanges();
        return r;
    }

    Reality collapseInto(String s) {
        for (Reality reality : this.realities)
            if (reality.name.equals(s)) {
                return collapseInto(reality);
            }
        System.err.format("Cannot find reality %s%n", s);
        return null;
    }

    public void collapse() {
        System.out.format("collapse %d %d%n", x, y);
        int i = (int) Math.floor(rnd.nextDouble() * this.realities.size());
        collapseInto(this.realities.get(i));
    }

    public boolean isCollapsed() {
        return realities.size() == 1;
    }

    public void propagateChanges() {
        LinkedList<Wave> queue = new LinkedList<>();
        queue.add(this);
        lastIterPropagation++;
        while (!queue.isEmpty()) {
            Wave p = queue.pop();
            for (int edge = 0; edge < 4; edge++)
                p.propagateForward(edge, queue);
        }
    }

    private void propagateForward(int edge, LinkedList<Wave> queue) {
        Wave neighbour = neighbour(edge);
        if (neighbour == null) return;
        if (neighbour.isCollapsed()) {
            neighbour.lastIterPropagation = lastIterPropagation;
            return;
        }

        // Apply my changes to other
        // neighbour's edge is (edge+2) % 4
        int neighboursEdge = (edge + 2) % 4;

        boolean changed = neighbour.dropNonMatching(
                this.edgeSocketSet.get(edge).keySet(),
                neighboursEdge // other point of view
        );
        if (changed) {
            queue.push(neighbour);
        }
    }

    static Wave createFixed(int x, int y, int w, Reality reality) {
        return new Wave(x, y, w, List.of(reality));
    }

    static class WaveIterator implements Iterator<Wave> {
        int x = 0, y = 0;

        @Override
        public boolean hasNext() {
            return x != grid.length || y != grid.length - 1;
        }

        @Override
        public Wave next() {
            if (x >= grid.length) {
                y++;
                x = 0;
            }

            return grid[y][x++];
        }
    }

}
