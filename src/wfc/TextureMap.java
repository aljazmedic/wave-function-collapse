package wfc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class TextureMap {

    private static SocketProvider socketProvider;

    public static void main(String[] args) throws IOException {
        TextureMap.fromFileConfig("pipes");
    }

    final BufferedImage source;
    Texture[] textures;
    List<Reality> realities;

    Map<String, Reality> realitiesByName;

    private final int textureWidth, tilemapRows, tilemapCols;

    public static TextureMap fromFileConfig(String path) throws IOException {
        BufferedImage source = ImageIO.read(new File(String.format("res/tiles/%s.png", path)));
        Scanner scn = new Scanner(new File(String.format("res/tiles/%s.configuration", path)));
        String line = scn.nextLine().strip();
        assert line.equals("#texturemap");
        int textureWidth = scn.nextInt();
        int numTextures = scn.nextInt();
        scn.nextLine(); // Consumes newline
        int tilemapRows = source.getWidth() / textureWidth;
        System.err.println("Detected rows: " + tilemapRows);
        int tilemapCols = source.getHeight() / textureWidth;
        System.err.println("Detected cols: " + tilemapCols);

        // Read textures from png
        Texture[] textures = new Texture[numTextures];
        int i = 0;
        for (int row = 0; row < tilemapRows && i < numTextures; row++)
            for (int col = 0; col < tilemapCols && i < numTextures; col++) {
                textures[i++] = new Texture(source.getSubimage(col * textureWidth, row * textureWidth, textureWidth, textureWidth), col, row);
            }

        // Read socket config
        String socketPixelPointLine = scn.nextLine().strip();
        socketProvider = SocketProvider.fromConfigLine(socketPixelPointLine, textureWidth);
        TextureMap tm = new TextureMap(source, textures, textureWidth, tilemapRows, tilemapCols);

        // Read tile config
        line = scn.nextLine();
        assert line.equals("#tiles");
        for (i = 0; i < numTextures; i++) {
            int rotateMap = scn.nextInt();
            int weight = scn.nextInt();
            String name = scn.nextLine().strip();
            Reality r = new Reality(name, weight, textures[i]);
            while (rotateMap > 0) {
                if ((rotateMap & 1) == 1) {
                    tm.realities.add(r);
                    tm.realitiesByName.put(r.name, r);
                    r = r.rotatedReality();
                }
                rotateMap >>= 1;
            }
        }
        if (scn.hasNextLine()) {
            line = scn.nextLine().strip();
            if (line.equals("#labels")) {
                while (scn.hasNextLine()) {
                    line = scn.nextLine().strip();
                    if (line.equals("#end"))
                        break;
                    String[] labelLine = line.split(" ");
                    socketProvider.addLabel(Integer.parseInt(labelLine[0]), labelLine[1]);
                }
            }else{
                System.err.format("Unknown line: %s%n", line);
            }
        }
        scn.close();
        System.err.format("Generated realities %d%n", tm.realities.size());
        for (Reality r : tm.realities)
            System.err.println(r);
        return tm;
    }

    public TextureMap(BufferedImage source, Texture[] textures, int textureWidth, int tilemapRows, int tilemapCols) {
        this.source = source;
        this.textures = textures;
        this.textureWidth = textureWidth;
        this.tilemapRows = tilemapRows;
        this.tilemapCols = tilemapCols;
        this.realities = new ArrayList<>();
        realitiesByName = new TreeMap<>();
    }

    public static SocketProvider getSocketProvider() {
        return socketProvider;
    }

    public Wave getWholeWave(int x, int y) {
        return new Wave(x, y, getTextureWidth(), realities);
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTilemapCols() {
        return tilemapCols;
    }

    public int getTilemapRows() {
        return tilemapRows;
    }

    static class Texture {
        private final int w, sourceX, sourceY;
        private final BufferedImage image;
        private final AffineTransformOp rotateTransform;

        Texture(BufferedImage bi, int sourceX, int sourceY) {
            this.image = bi;
            this.sourceX = sourceX;
            this.sourceY = sourceY;
            this.w = bi.getWidth();
            this.rotateTransform = new AffineTransformOp(AffineTransform.getRotateInstance(Math.PI / 2, (double) w / 2, (double) w / 2), AffineTransformOp.TYPE_BILINEAR);
        }

        public BufferedImage getImage() {
            return image;
        }

        public int getW() {
            return w;
        }

        public int getSourceX() {
            return sourceX;
        }

        public int getSourceY() {
            return sourceY;
        }

        public int getColor(int x, int y) {
            return image.getRGB(x, y);
        }


        public Texture rotate90() {
            BufferedImage bufferedImage = getImage();
            BufferedImage out = new BufferedImage(bufferedImage.getHeight(), bufferedImage.getWidth(), BufferedImage.TYPE_4BYTE_ABGR);
            rotateTransform.filter(bufferedImage, out);
            return new Texture(out, sourceX, sourceY);
        }

    }

    public void showCompatible(String r, int edge) {
        int otherE = (edge + 2) % 4;
        Reality real = realitiesByName.get(r);
        System.out.format("Compatible with %s on edge %d:%n", real, edge);
        for (Reality other : this.realities) {
            if (other.sockets[otherE] == real.sockets[edge])
                System.out.println(other);
        }
    }

    public static class SocketProvider {

        private final List<Integer> socketPoints;// = List.of(10, 40, 64, 128 - 10, 128 - 40);
        private final Map<Integer, String> socketLabels;
        private static final int MAD = 43;

        SocketProvider(List<Integer> socketPoints) {
            this.socketPoints = socketPoints;
            this.socketLabels = new HashMap<>();
        }

        public static SocketProvider fromConfigLine(String line, int textureWidth) {
            //Positioned after `#socket`
            List<Integer> socketPoints;
            String[] points = line.split(" ");
            socketPoints = new ArrayList<>(points.length);
            for (String point : points) {
                int pixel = ((Integer.parseInt(point) % textureWidth) + textureWidth) % textureWidth;
                socketPoints.add(pixel);
            }
            Collections.sort(socketPoints);
            return new SocketProvider(socketPoints);
        }

        public void addLabel(Integer i, String s) {
            this.socketLabels.put(i, s);
        }

        private String whichSocket(int sock) {
            if (!socketLabels.containsKey(sock)) {
                socketLabels.put(sock, "" + sock);
                return whichSocket(sock);
            }
            return socketLabels.get(sock);
        }

        public String format(int[] sockets) {
            StringBuilder sb = new StringBuilder("[");
            for (int i : sockets)
                sb.append(whichSocket(i)).append(',');

            sb.setCharAt(sb.length() - 1, ']');
            return sb.toString();

        }

        public String format(Collection<Integer> sockets) {
            Iterator<Integer> iter = sockets.iterator();
            StringBuilder sb = new StringBuilder("[");
            int i;
            while (iter.hasNext()) {
                i = iter.next();
                sb.append(whichSocket(i)).append(',');
            }
            sb.setCharAt(sb.length() - 1, ']');
            return sb.toString();
        }


        private int getColorTexture(Texture t, int x, int y) {
            int c = t.getColor(x, y);
            if (c == 16777215) return 0; // Weird bug with color in buffer
            if (c == -6299137) return -6889985;
            return c;
        }

        public int[] getSockets(Texture texture) {
            int w = texture.getW();
            int[] sockets = new int[4];
            w--;
            for (int p : socketPoints) {
                sockets[0] = sockets[0] * MAD + getColorTexture(texture, p, 0);
                sockets[1] = sockets[1] * MAD + getColorTexture(texture, w, p);
                sockets[2] = sockets[2] * MAD + getColorTexture(texture, p, w);
                sockets[3] = sockets[3] * MAD + getColorTexture(texture, 0, p);
            }
            return sockets;
        }

        public void paintMeasurePoint(Graphics2D g2d, int x, int y, Texture t) {
            g2d.drawRect(x, y, 1, 1);
            g2d.drawString("" + t.getColor(x, y), 4 + x, y - 4);
        }

        public void paintSockets(Graphics2D g2d, int x, int y, Reality t) {
            double scale = 3;
            g2d.translate(x, y);
            g2d.scale(scale, scale);
            g2d.drawImage(t.getImage(), 0, 0, null);
            int w = t.texture.getW();
            w--;
            for (int p : socketPoints) {
                paintMeasurePoint(g2d, p, 0, t.texture);
                paintMeasurePoint(g2d, w, p, t.texture);
                paintMeasurePoint(g2d, p, w, t.texture);
                paintMeasurePoint(g2d, 0, p, t.texture);
            }
            g2d.scale(1 / scale, 1 / scale);
            g2d.translate(-x, -y);

        }
    }
}
