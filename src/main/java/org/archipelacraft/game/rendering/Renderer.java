package org.archipelacraft.game.rendering;

import org.archipelacraft.Main;
import org.archipelacraft.game.world.World;
import org.joml.*;
import org.archipelacraft.engine.*;
import org.archipelacraft.engine.Window;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL46.*;

public class Renderer {
    public static ShaderProgram scene;
    public static ShaderProgram debug;
    public static int sceneVaoId;
    public static int debugVaoId;

    public static int rasterFBOId;

    public static boolean showUI = true;
    public static boolean shadowsEnabled = true;
    public static boolean reflectionShadows = false;
    public static boolean reflectionsEnabled = true;
    public static int renderDistanceMul = 8; //3
    public static int aoQuality = 2;
    public static float timeOfDay = 0.5f;
    public static double time = 0.5d;

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
        glBufferData(GL_ARRAY_BUFFER, new float[]{
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
        }, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
    }

    public static void initiallyFillTextures(Window window) throws IOException {
        glBindTexture(GL_TEXTURE_2D, Textures.rasterColor.id);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32F, window.getWidth(), window.getHeight());
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, Textures.rasterColor.id, 0);

        glBindTexture(GL_TEXTURE_2D, Textures.rasterDepth.id);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_DEPTH_COMPONENT32F, window.getWidth(), window.getHeight());
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, Textures.rasterDepth.id, 0);

        BufferedImage atlasImage = ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/textures/atlas.png"));
        glBindTexture(GL_TEXTURE_3D, Textures.atlas.id);
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, Textures.atlas.width, Textures.atlas.height, ((Texture3D)Textures.atlas).depth, 0, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(atlasImage));

        glBindTexture(GL_TEXTURE_3D, Textures.blocks.id);
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA16I, Textures.blocks.width, Textures.blocks.height, ((Texture3D)Textures.blocks).depth, 0, GL_RGBA_INTEGER, GL_SHORT, World.blocks);
        glTexImage3D(GL_TEXTURE_3D, 2, GL_RGBA16I, Textures.blocks.width/4, Textures.blocks.height/4, ((Texture3D)Textures.blocks).depth/4, 0, GL_RED_INTEGER, GL_SHORT, World.blocksLOD);

        glBindTexture(GL_TEXTURE_3D, Textures.lights.id);
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA4, Textures.lights.width, Textures.lights.height, ((Texture3D)Textures.lights).depth, 0, GL_RGBA, GL_BYTE, World.lights.flip());
    }

    public static void bindTextures() {
        glBindTextureUnit(0, Textures.rasterColor.id);
        glBindTextureUnit(1, Textures.rasterDepth.id);
        glBindTextureUnit(2, Textures.atlas.id);
        glBindTextureUnit(3, Textures.blocks.id);
        glBindTextureUnit(4, Textures.lights.id);
    }

    public static void init(Window window) throws Exception {
        createGLDebugger();
        scene = new ShaderProgram("scene.vert", new String[]{"scene.frag"},
                new String[]{"res", "projection", "view", "selected", "selected", "ui", "renderDistance", "aoQuality", "timeOfDay", "time", "shadowsEnabled", "reflectionShadows", "sun"});
        debug = new ShaderProgram("debug.vert", new String[]{"debug.frag"},
                new String[]{"res", "projection", "view", "model", "selected", "ui", "renderDistance", "aoQuality", "timeOfDay", "time", "shadowsEnabled", "reflectionShadows", "sun"});
        generateVaos();

        rasterFBOId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, rasterFBOId);

        Textures.generate();
        initiallyFillTextures(window);
    }

    public static void  updateUniforms(ShaderProgram program, Window window) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(program.uniforms.get("projection"), false, window.updateProjectionMatrix().get(stack.mallocFloat(16)));
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(program.uniforms.get("view"), false, new Matrix4f(Main.player.getCameraMatrix()).get(stack.mallocFloat(16)));
        }
        Vector3f selected = null;
        if (selected == null) {
            selected = new Vector3f(-1000, -1000, -1000);
        }
        glUniform3i(program.uniforms.get("selected"), (int) selected.x, (int) selected.y, (int) selected.z);
        glUniform1i(program.uniforms.get("ui"), showUI ? 1 : 0);
        glUniform1i(program.uniforms.get("renderDistance"), 200 + (100 * renderDistanceMul));
        glUniform1i(program.uniforms.get("aoQuality"), aoQuality);
        glUniform1f(program.uniforms.get("timeOfDay"), timeOfDay);
        glUniform1d(program.uniforms.get("time"), time);
        glUniform1i(program.uniforms.get("shadowsEnabled"), shadowsEnabled ? 1 : 0);
        glUniform1i(program.uniforms.get("reflectionShadows"), reflectionShadows ? 1 : 0);
        float halfSize = World.size / 2f;
        Vector3f sunPos = new Vector3f(halfSize, 0, halfSize);
        sunPos.rotateY((float) time);
        sunPos = new Vector3f(sunPos.x + halfSize, World.height + 64, sunPos.z + halfSize);
        glUniform3f(program.uniforms.get("sun"), sunPos.x, sunPos.y, sunPos.z);
    }

    public static void draw() {
        glBindVertexArray(sceneVaoId);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glDisableVertexAttribArray(0);
    }
    public static void drawDebug() {
        glBindVertexArray(debugVaoId);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_TRIANGLES, 0, 12*3);
        glDisableVertexAttribArray(0);
    }
    public static void drawLowerCorner() {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(-0.5f, -0.5f, -0.5f).get(stack.mallocFloat(16)));
        }
        drawDebug();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(-0.5f, 0.5f, -0.5f).get(stack.mallocFloat(16)));
        }
        drawDebug();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(-0.5f, -0.5f, 0.5f).get(stack.mallocFloat(16)));
        }
        drawDebug();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(0.5f, -0.5f, -0.5f).get(stack.mallocFloat(16)));
        }
        drawDebug();
    }
    public static void drawUpperCorner() {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(1024.5f, 320.5f, 1024.5f).get(stack.mallocFloat(16)));
        }
        drawDebug();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(1024.5f, 319.5f, 1024.5f).get(stack.mallocFloat(16)));
        }
        drawDebug();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(1024.5f, 320.5f, 1023.5f).get(stack.mallocFloat(16)));
        }
        drawDebug();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(debug.uniforms.get("model"), false, new Matrix4f().translate(1023.5f, 320.5f, 1024.5f).get(stack.mallocFloat(16)));
        }
        drawDebug();
    }
    public static void render(Window window) throws IOException {
        if (!Main.isClosing) {
            glBindFramebuffer(GL_FRAMEBUFFER, rasterFBOId);
            glClearColor(0, 0, 0, 0);
            glClearDepthf(0.f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            debug.bind();
            updateUniforms(debug, window);
            drawLowerCorner();
            drawUpperCorner();

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