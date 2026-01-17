package org.archipelacraft.game.noise;

import org.archipelacraft.game.rendering.Renderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Noises {
    public static Map<Integer, Noise> noiseMap = new HashMap<>(Map.of());
    public static Noise COHERERENT_NOISE;
    public static Noise CELLULAR_NOISE;
    public static Noise WHITE_NOISE;
    public static Noise NOODLE_NOISE;
    public static Noise CLOUD_NOISE;
    public static Noise SPIRAL_NOISE;

    public static void init() throws IOException {
        COHERERENT_NOISE = create(new Noise(loadImage("generic/texture/coherent_noise")));
        CELLULAR_NOISE = create(new Noise(loadImage("generic/texture/cellular_noise")));
        WHITE_NOISE = create(new Noise(loadImage("generic/texture/white_noise")));
        NOODLE_NOISE = create(new Noise(loadImage("generic/texture/noodle_noise")));
        CLOUD_NOISE = create(new Noise(loadImage("generic/texture/cloud_noise")));
        SPIRAL_NOISE = create(new Noise(loadImage("generic/texture/spiral_noise")));
    }

    private static Noise create(Noise type) {
        noiseMap.put(noiseMap.size(), type);
        return type;
    }

    private static BufferedImage loadImage(String name) throws IOException {
        return ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/"+name+".png"));
    }
}