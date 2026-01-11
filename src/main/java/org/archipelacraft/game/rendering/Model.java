package org.archipelacraft.game.rendering;

public class Model {
    public float[] positions;
    public float[] normals;

    public Model(float[] verts, float[] normals) {
        this.positions = verts;
        this.normals = normals;
    }
}
