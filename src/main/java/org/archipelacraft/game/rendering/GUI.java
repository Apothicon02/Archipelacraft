package org.archipelacraft.game.rendering;

import org.archipelacraft.engine.Utils;
import org.archipelacraft.engine.Window;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL45.glBindTextureUnit;

public class GUI {
    public static boolean isInventoryOpen = false;
    public static float guiScale = 600f;
    public static float aspectRatio = 0f;
    public static int width = 0;
    public static int height = 0;

    public static void drawQuad(boolean centeredX, boolean centeredY, float x, float y, int scaleX, int scaleY) {
        float xScale = (scaleX/guiScale);
        float yScale = (scaleY/guiScale)*aspectRatio;
        float xOffset = ((x*2)-1)+(centeredX ? 0 : xScale);
        float yOffset = ((y*2)-1)+(centeredY ? 0 : yScale);
        glUniform2i(Renderer.gui.uniforms.get("offset"), (int)((x-(centeredX ? xScale/2 : 0))*width), (int)((y+(centeredY ? yScale/2 : 0))*height));
        glUniform2i(Renderer.gui.uniforms.get("size"), scaleX, scaleY);
        glUniform2i(Renderer.gui.uniforms.get("scale"), (int)(xScale*width), (int)(yScale*height));
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(Renderer.gui.uniforms.get("model"), false, new Matrix4f().translate(xOffset, yOffset, 0.f).scale(xScale, yScale, 1).get(stack.mallocFloat(16)));
        }
        glBindVertexArray(Models.CUBE.vaoId);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glDrawArrays(GL_TRIANGLES, 0, Models.CUBE.positions.length);
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
    }

    public static void draw(Window window) {
        glBindTextureUnit(1, Textures.gui.id);
        width = window.getWidth();
        height = window.getHeight();
        aspectRatio = (float) width / height;
        glUniform1i(Renderer.gui.uniforms.get("layer"), 0);
        if (isInventoryOpen) {
            glUniform4f(Renderer.gui.uniforms.get("color"), 0.85f, 0.85f, 0.85f, 0.85f);
            drawQuad(true, false, 0.5f, (5.f / height) + ((22.f / guiScale) * aspectRatio), 182, 22);
            drawQuad(true, false, 0.5f, (5.f / height) + ((44.f / guiScale) * aspectRatio), 182, 22);
            drawQuad(true, false, 0.5f, (5.f / height) + ((66.f / guiScale) * aspectRatio), 182, 22);
        }
        glUniform4f(Renderer.gui.uniforms.get("color"), 1.f, 1.f, 1.f, 1.f);
        drawQuad(true, false, 0.5f, 5.f/height, 182, 22);
        glUniform1i(Renderer.gui.uniforms.get("layer"), 1);
        drawQuad(true, false, 0.5f, 3.f/height, 24, 23);
    }

    public static void fillTexture() throws IOException {
        glBindTexture(GL_TEXTURE_3D, Textures.gui.id);
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, Textures.gui.width, Textures.gui.height, ((Texture3D)(Textures.gui)).depth, 0, GL_RGBA, GL_FLOAT,
                new float[Textures.gui.width*Textures.gui.height*((Texture3D)(Textures.gui)).depth*4]);
//        glTexStorage3D(GL_TEXTURE_3D, 1, GL_RGBA32F, Textures.gui.width, Textures.gui.height, ((Texture3D)Textures.gui).depth);
        BufferedImage hotbarImg = ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/gui/texture/hotbar.png"));
        glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, 0, hotbarImg.getWidth(), hotbarImg.getHeight(), 1, GL_RGBA, GL_UNSIGNED_BYTE,
                Utils.imageToBuffer(hotbarImg));
        BufferedImage selectedSlotImg = ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/gui/texture/selected_slot.png"));
        glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, 1, selectedSlotImg.getWidth(), selectedSlotImg.getHeight(), 1, GL_RGBA, GL_UNSIGNED_BYTE,
                Utils.imageToBuffer(selectedSlotImg));
    }
}
