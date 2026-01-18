package org.archipelacraft.game.rendering;

public class Texture3D extends Texture {
    public int depth;

    public Texture3D(int[] parameters, int width, int height, int depth) {
        super(parameters, width, height);
        this.depth = depth;
    }
}
