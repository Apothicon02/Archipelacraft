package org.archipelacraft.game.rendering;

import org.archipelacraft.Main;
import org.archipelacraft.game.noise.Noises;
import org.archipelacraft.game.world.World;
import org.joml.*;
import org.archipelacraft.engine.*;
import org.archipelacraft.engine.Window;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.Math;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL46.*;

public class Renderer {
    public static ShaderProgram scene;
    public static ShaderProgram debug;
    public static int sceneVaoId;
    public static int debugVaoId;
    public static int debugWheelVaoId;

    public static int rasterFBOId;

    public static int playerSSBOId;

    public static boolean showUI = true;
    public static boolean shadowsEnabled = true;
    public static boolean reflectionShadows = false;
    public static boolean reflectionsEnabled = true;
    public static int renderDistanceMul = 8; //3
    public static int aoQuality = 2;
    public static float timeOfDay = 0.5f;
    public static double time = 0.5f;

    public static boolean resized = false;

    public static void createGLDebugger() {
        glEnable(GL_DEBUG_OUTPUT);
        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
        glDebugMessageCallback((source, type, id, severity, length, message, userParam) -> {
            String msg = GLDebug.decode(source, type, id, severity, length, message, userParam);
            if (msg != null) {
                System.out.println(msg);
            }
        }, 0);
    }

    public static void generateVaos() {
        sceneVaoId = glGenVertexArrays();
        glBindVertexArray(sceneVaoId);
        glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ARRAY_BUFFER, new float[]{
                -1, -1, 0,
                3, -1, 0,
                -1, 3, 0
        }, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        debugVaoId = glGenVertexArrays();
        glBindVertexArray(debugVaoId);
        glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ARRAY_BUFFER, Models.CUBE.verts, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        debugWheelVaoId = glGenVertexArrays();
        glBindVertexArray(debugWheelVaoId);
        glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ARRAY_BUFFER, Models.WATER_WHEEL.verts, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
    }
    public static boolean[] collisionData = new boolean[(1024*1024)+1024];

    public static void initiallyFillTextures(Window window) throws IOException {
        glBindTexture(GL_TEXTURE_2D, Textures.rasterColor.id);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32F, window.getWidth(), window.getHeight());
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, Textures.rasterColor.id, 0);

        glBindTexture(GL_TEXTURE_2D, Textures.rasterDepth.id);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_DEPTH_COMPONENT32F, window.getWidth(), window.getHeight());
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, Textures.rasterDepth.id, 0);

        BufferedImage atlasImage = ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/textures/atlas.png"));
        for (int x = 0; x < Textures.atlas.width; x++) {
            for (int y = 0; y < 1024; y++) {
                Color color = new Color(atlasImage.getRGB(x, y), true);
                collisionData[(x*1024) + y] = color.getAlpha() != 0;
            }
        }
        glBindTexture(GL_TEXTURE_3D, Textures.atlas.id);
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, Textures.atlas.width, Textures.atlas.height, ((Texture3D)Textures.atlas).depth, 0, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(atlasImage));

        glBindTexture(GL_TEXTURE_3D, Textures.blocks.id);
        glTexStorage3D(GL_TEXTURE_3D, 5, GL_RGBA16I, Textures.blocks.width, Textures.blocks.height, ((Texture3D)Textures.blocks).depth);
        glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, 0, Textures.blocks.width, Textures.blocks.height, ((Texture3D)Textures.blocks).depth, GL_RG_INTEGER, GL_SHORT, World.blocks);
        glTexSubImage3D(GL_TEXTURE_3D, 2, 0, 0, 0, Textures.blocks.width/4, Textures.blocks.height/4, ((Texture3D)Textures.blocks).depth/4, GL_RED_INTEGER, GL_SHORT, World.blocksLOD);
        glTexSubImage3D(GL_TEXTURE_3D, 4, 0, 0, 0, Textures.blocks.width/16, Textures.blocks.height/16, ((Texture3D)Textures.blocks).depth/16, GL_RED_INTEGER, GL_SHORT, World.blocksLOD2);

        glBindTexture(GL_TEXTURE_3D, Textures.lights.id);
        glTexStorage3D(GL_TEXTURE_3D, 1, GL_RGBA4, Textures.lights.width, Textures.lights.height, ((Texture3D)Textures.lights).depth);
        glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, 0, Textures.lights.width, Textures.lights.height, ((Texture3D)Textures.lights).depth, GL_RGBA, GL_BYTE, ByteBuffer.allocateDirect(World.lights.length).put(World.lights).flip());

        float[] mergedNoises = new float[(Textures.noises.width*Textures.noises.height)*4];
        for (int x = 0; x < Textures.noises.width; x++) {
            for (int y = 0; y < Textures.noises.height; y++) {
                int pos = 4*((x*Textures.noises.height)+y);
                mergedNoises[pos] = Noises.COHERERENT_NOISE.sample(x, y);
                mergedNoises[pos+1] = Noises.WHITE_NOISE.sample(x, y);
            }
        }
        glBindTexture(GL_TEXTURE_2D, Textures.noises.id);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, Textures.noises.width, Textures.noises.height, 0, GL_RGBA, GL_FLOAT, mergedNoises);

    }

    public static void bindTextures() {
        glBindTextureUnit(0, Textures.rasterColor.id);
        glBindTextureUnit(1, Textures.rasterDepth.id);
        glBindTextureUnit(2, Textures.atlas.id);
        glBindTextureUnit(3, Textures.blocks.id);
//        long startTime = System.currentTimeMillis();
//        glBindTexture(GL_TEXTURE_3D, Textures.blocks.id);
//        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA16I, Textures.blocks.width, Textures.blocks.height, ((Texture3D)Textures.blocks).depth, 0, GL_RG_INTEGER, GL_SHORT, World.blocks);
//        glTexImage3D(GL_TEXTURE_3D, 2, GL_RGBA16I, Textures.blocks.width/4, Textures.blocks.height/4, ((Texture3D)Textures.blocks).depth/4, 0, GL_RED_INTEGER, GL_SHORT, World.blocksLOD);
//        glTexImage3D(GL_TEXTURE_3D, 4, GL_RGBA16I, Textures.blocks.width/16, Textures.blocks.height/16, ((Texture3D)Textures.blocks).depth/16, 0, GL_RED_INTEGER, GL_SHORT, World.blocksLOD2);
//        System.out.print(System.currentTimeMillis()-startTime);
        glBindTextureUnit(4, Textures.lights.id);
        glBindTextureUnit(5, Textures.noises.id);
    }

    public static void createBuffers() {
        playerSSBOId = glCreateBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, playerSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, playerSSBOId);
        glBufferStorage(GL_SHADER_STORAGE_BUFFER, new float[6], GL_CLIENT_STORAGE_BIT | GL_MAP_READ_BIT);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    public static void updateBuffers() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, playerSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, playerSSBOId);
        float[] data = new float[6];
        glGetBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, data);
        Main.player.selectedBlock.set(data[0], data[1], data[2]);
        Main.player.prevSelectedBlock.set(data[3], data[4], data[5]);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    public static void init(Window window) throws Exception {
        createGLDebugger();
        scene = new ShaderProgram("scene.vert", new String[]{"scene.frag"},
                new String[]{"res", "projection", "view", "selected", "ui", "renderDistance", "aoQuality", "timeOfDay", "time", "shadowsEnabled", "reflectionShadows", "sun", "mun"});
        debug = new ShaderProgram("debug.vert", new String[]{"debug.frag"},
                new String[]{"res", "projection", "view", "model", "selected", "color", "ui", "renderDistance", "aoQuality", "timeOfDay", "time", "shadowsEnabled", "reflectionShadows", "sun", "mun"});
        generateVaos();

        rasterFBOId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, rasterFBOId);

        createBuffers();

        Textures.generate();
        initiallyFillTextures(window);
    }

    public static Vector3f sunPos = new Vector3f(0, World.height*2, 0);
    public static Vector3f munPos = new Vector3f(0, World.height*-2, 0);

    public static void  updateUniforms(ShaderProgram program, Window window) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(program.uniforms.get("projection"), false, window.updateProjectionMatrix().get(stack.mallocFloat(16)));
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(program.uniforms.get("view"), false, new Matrix4f(Main.player.getCameraMatrix()).get(stack.mallocFloat(16)));
        }
        Vector3f selected = Main.player.selectedBlock;
        glUniform3i(program.uniforms.get("selected"), (int) selected.x, (int) selected.y, (int) selected.z);
        glUniform1i(program.uniforms.get("ui"), showUI ? 1 : 0);
        glUniform1i(program.uniforms.get("renderDistance"), 200 + (100 * renderDistanceMul));
        glUniform1i(program.uniforms.get("aoQuality"), aoQuality);
        glUniform1f(program.uniforms.get("timeOfDay"), timeOfDay);
        glUniform1d(program.uniforms.get("time"), time);
        glUniform1i(program.uniforms.get("shadowsEnabled"), shadowsEnabled ? 1 : 0);
        glUniform1i(program.uniforms.get("reflectionShadows"), reflectionShadows ? 1 : 0);
        sunPos.set(0, World.size*2, 0);
        sunPos.rotateZ((float) time);
        sunPos.set(sunPos.x+(World.size/2f), sunPos.y-World.size, sunPos.z+(World.size/2f));
        glUniform3f(program.uniforms.get("sun"), sunPos.x, sunPos.y, sunPos.z);
        munPos.set(0, World.size*-2, 0);
        munPos.rotateZ((float) time);
        munPos.set(munPos.x+(World.size/2f), munPos.y-World.size, munPos.z+(World.size/2f));
        glUniform3f(program.uniforms.get("mun"), munPos.x, munPos.y, munPos.z);
    }

    public static void draw() {
        glBindVertexArray(sceneVaoId);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glDisableVertexAttribArray(0);
    }
    public static void drawCube() {
        glBindVertexArray(debugVaoId);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_TRIANGLES, 0, Models.CUBE.verts.length*3);
        glDisableVertexAttribArray(0);
    }
    public static void drawDebugWheel() {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(Main.player.pos).translate(10, 0, 0).get(stack.mallocFloat(16)));
        }
        glUniform4f(debug.uniforms.get("color"), 0.5f, 0.5f, 0.5f, 1);
        glBindVertexArray(debugWheelVaoId);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_TRIANGLES, 0, Models.WATER_WHEEL.verts.length*3);
        glDisableVertexAttribArray(0);
    }
    public static void drawSunAndMoon() {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().rotateXYZ(0.5f, 0.5f, 0.5f).setTranslation(sunPos).scale(100).get(stack.mallocFloat(16)));
        }
        glUniform4f(debug.uniforms.get("color"), 1, 1, 0.05f, 10);
        drawCube();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().rotateXYZ(0.5f, 0.5f, 0.5f).setTranslation(munPos).scale(100).get(stack.mallocFloat(16)));
        }
        glUniform4f(debug.uniforms.get("color"), 0.63f, 0.58f, 0.66f, 10);
        drawCube();
    }
    public static Vector3f[] starColors = new Vector3f[]{new Vector3f(0.9f, 0.95f, 1.f), new Vector3f(1, 0.95f, 0.4f), new Vector3f(0.72f, 0.05f, 0), new Vector3f(0.42f, 0.85f, 1.f), new Vector3f(0.04f, 0.3f, 1.f), new Vector3f(1, 1, 0.1f)};
    public static int starDist = World.size+100;
    public static void drawStars() {
        Random starRand = new Random(911);
        for (int i = 0; i < 1024; i++) {
            Vector3f starPos = new Vector3f(0, starDist * 2, 0)
                    .rotateX(starRand.nextFloat() * 10)
                    .rotateY(starRand.nextFloat() * 10)
                    .rotateZ((float) time + starRand.nextFloat() * 10);
            starPos.set(starPos.x + (starDist / 2f), starPos.y - starDist, starPos.z + (starDist / 2f));
            float starSize = ((starRand.nextFloat()*12)+6)-Math.max(0, 30*(sunPos.y/World.size));
            try (MemoryStack stack = MemoryStack.stackPush()) {
                glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f()
                        .rotateXYZ(starRand.nextFloat(), starRand.nextFloat(), starRand.nextFloat())
                        .setTranslation(starPos)
                        .scale(starSize).get(stack.mallocFloat(16)));
            }
            Vector3f color = starRand.nextFloat() < 0.64f ? new Vector3f(0.97f, 0.98f, 1.f) : starColors[starRand.nextInt(starColors.length - 1)];
            if (starSize > 0.01f) {
                glUniform4f(debug.uniforms.get("color"), color.x, color.y, color.z, 10);
                drawCube();
            }
        }
    }
    public static void drawCenter() {
        glUniform4f(debug.uniforms.get("color"), 0.5f, 0.5f, 0.5f, 1);
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(512.5f, 319.5f, 512.5f).get(stack.mallocFloat(16)));
        }
        drawCube();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(512.5f, 269.5f, 512.5f).get(stack.mallocFloat(16)));
        }
        drawCube();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(512.5f, 219.5f, 512.5f).get(stack.mallocFloat(16)));
        }
        drawCube();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(512.5f, 169.5f, 512.5f).get(stack.mallocFloat(16)));
        }
        drawCube();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(512.5f, 119.5f, 512.5f).get(stack.mallocFloat(16)));
        }
        drawCube();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(512.5f, 95.5f, 512.5f).get(stack.mallocFloat(16)));
        }
        drawCube();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(512.5f, 95.5f, 516.5f).get(stack.mallocFloat(16)));
        }
        drawCube();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(512.5f, 95.5f, 519.5f).get(stack.mallocFloat(16)));
        }
        drawCube();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(512.5f, 95.5f, 522.5f).get(stack.mallocFloat(16)));
        }
        drawCube();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(512.5f, 95.5f, 525.5f).get(stack.mallocFloat(16)));
        }
        drawCube();
    }
    public static void render(Window window) throws IOException {
        if (!Main.isClosing) {
            glBindFramebuffer(GL_FRAMEBUFFER, rasterFBOId);
            glClearColor(0, 0, 0, 0);
            glClearDepthf(0.f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            debug.bind();
            updateBuffers();
            updateUniforms(debug, window);
            drawSunAndMoon();
            drawStars();
            drawCenter();
            //drawDebugWheel();

            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glClearColor(0, 0, 0, 0);
            glClearDepthf(0.f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            scene.bind();
            updateUniforms(scene, window);
            bindTextures();
            glUniform2i(scene.uniforms.get("res"), window.getWidth(), window.getHeight());

            draw();
        }
    }
}