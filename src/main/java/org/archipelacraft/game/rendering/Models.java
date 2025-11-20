package org.archipelacraft.game.rendering;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Models {

    public static Model CUBE = new Model(new float[]{
            -0.5f,-0.5f,-0.5f, // triangle 1 : begin
            -0.5f,-0.5f, 0.5f,
            -0.5f, 0.5f, 0.5f, // triangle 1 : end
            0.5f, 0.5f,-0.5f, // triangle 2 : begin
            -0.5f,-0.5f,-0.5f,
            -0.5f, 0.5f,-0.5f, // triangle 2 : end
            0.5f,-0.5f, 0.5f,
            -0.5f,-0.5f,-0.5f,
            0.5f,-0.5f,-0.5f,
            0.5f, 0.5f,-0.5f,
            0.5f,-0.5f,-0.5f,
            -0.5f,-0.5f,-0.5f,
            -0.5f,-0.5f,-0.5f,
            -0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f,-0.5f,
            0.5f,-0.5f, 0.5f,
            -0.5f,-0.5f, 0.5f,
            -0.5f,-0.5f,-0.5f,
            -0.5f, 0.5f, 0.5f,
            -0.5f,-0.5f, 0.5f,
            0.5f,-0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f,-0.5f,-0.5f,
            0.5f, 0.5f,-0.5f,
            0.5f,-0.5f,-0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f,-0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f,-0.5f,
            -0.5f, 0.5f,-0.5f,
            0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f,-0.5f,
            -0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f, 0.5f,
            0.5f,-0.5f, 0.5f
    });
    public static Model WATER_WHEEL;

    public static void loadModels() {
        WATER_WHEEL = new Model(loadObj("water_wheel"));
    }

    public static float[] loadObj(String name) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(Renderer.class.getClassLoader().getResourceAsStream("assets/base/models/"+name+".obj")));
        FloatArrayList model = new FloatArrayList();
        reader.lines().forEach((String line) -> {
            String[] parts = line.split("\\s+");
            if (parts[0].equals("v")) {
                model.addLast(Float.parseFloat(parts[1]));
                model.addLast(Float.parseFloat(parts[2]));
                model.addLast(Float.parseFloat(parts[3]));
            }
        });
        float[] floatArr = model.toFloatArray(new float[3]);
        return floatArr;
    }
}
