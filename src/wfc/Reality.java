package wfc;

import java.awt.image.BufferedImage;

public class Reality {

    String name;
    TextureMap.Texture texture;
    int[] sockets;
    int weight;

    public Reality(String name, int weight, TextureMap.Texture t) {
        this.name = name;
        this.texture = t;
        this.weight = weight;
        sockets = TextureMap.getSocketProvider().getSockets(t);
    }

    public BufferedImage getImage() {
        return texture.getImage();
    }

    Reality rotatedReality() {
        StringBuilder sb = new StringBuilder(name);
        if (name.length() == 1)
            sb.append('1');
        else {
            char r = name.charAt(1);
            if (r == '3') sb.deleteCharAt(1);
            else sb.setCharAt(1, (char) (r + 1));
        }
        return new Reality(sb.toString(), this.weight, texture.rotate90());
    }


    @Override
    public String toString() {
        return "Reality{" +this.name + ", "+ TextureMap.getSocketProvider().format(sockets) + '}';
    }

    public TextureMap.Texture getTexture() {
        return  texture;
    }

}
