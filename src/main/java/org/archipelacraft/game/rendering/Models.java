package org.archipelacraft.game.rendering;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Models {

    public static Model CUBE;
    public static Model TORUS;

    public static void loadModels() {
        CUBE = loadObj("cube");
        TORUS = loadObj("torus");
    }

    public static FloatArrayList verts = new FloatArrayList();
    public static FloatArrayList normals = new FloatArrayList();
    public static FloatArrayList vertPositions = new FloatArrayList();
    public static FloatArrayList vertNormals = new FloatArrayList();
    public static void clearArrays() {
        verts.clear();
        normals.clear();
        vertPositions.clear();
        vertNormals.clear();
    }

    public static Model loadObj(String name) {
        clearArrays();
        BufferedReader reader = new BufferedReader(new InputStreamReader(Renderer.class.getClassLoader().getResourceAsStream("assets/base/models/"+name+".obj")));
        reader.lines().forEach((String line) -> {
            String[] parts = line.split("\\s+");
            if (parts[0].equals("v")) {
                verts.addLast(Float.parseFloat(parts[1]));
                verts.addLast(Float.parseFloat(parts[2]));
                verts.addLast(Float.parseFloat(parts[3]));
            } else if (parts[0].equals("vn")) {
                normals.addLast(Float.parseFloat(parts[1]));
                normals.addLast(Float.parseFloat(parts[2]));
                normals.addLast(Float.parseFloat(parts[3]));
            } else if (parts[0].equals("f")) {
                createVertex(parts[1].split("//"));
                createVertex(parts[2].split("//"));
                createVertex(parts[3].split("//"));
            }
        });
        return new Model(vertPositions.toFloatArray(), vertNormals.toFloatArray());
    }
    public static void createVertex(String[] vertex) {
        int vertId = (Integer.parseInt(vertex[0])-1)*3;
        vertPositions.addLast(verts.get(vertId));
        vertPositions.addLast(verts.get(1+vertId));
        vertPositions.addLast(verts.get(2+vertId));
        int normId = (Integer.parseInt(vertex[1])-1)*3;
        vertNormals.addLast(normals.get(normId));
        vertNormals.addLast(normals.get(1+normId));
        vertNormals.addLast(normals.get(2+normId));
    }
}
