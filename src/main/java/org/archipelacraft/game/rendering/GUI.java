package org.archipelacraft.game.rendering;

import org.archipelacraft.Main;
import org.archipelacraft.engine.Utils;
import org.archipelacraft.engine.Window;
import org.archipelacraft.game.gameplay.HandManager;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
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

    public static int hotbarSizeX = 182;
    public static int hotbarSizeY = 22;
    public static int slotSizeX = 20;
    public static int slotSizeY = 22;

    public static void draw(Window window) {
        glBindTextureUnit(1, Textures.gui.id);
        width = window.getWidth();
        height = window.getHeight();
        aspectRatio = (float) width / height;
        float hotbarPosX = (0.5f-((182/2f)/guiScale));
        float hotbarPosY = 5.f/height;
        glUniform1i(Renderer.gui.uniforms.get("layer"), 0);
        if (isInventoryOpen) {
            glUniform4f(Renderer.gui.uniforms.get("color"), 0.85f, 0.85f, 0.85f, 0.85f);
            drawQuad(false, false, hotbarPosX, hotbarPosY + ((hotbarSizeY / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY);
            drawQuad(false, false, hotbarPosX, hotbarPosY + (((hotbarSizeY*2) / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY);
            drawQuad(false, false, hotbarPosX, hotbarPosY + (((hotbarSizeY*3) / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY);
        }
        glUniform4f(Renderer.gui.uniforms.get("color"), 1.f, 1.f, 1.f, 1.f);
        drawQuad(false, false, hotbarPosX, hotbarPosY, hotbarSizeX, hotbarSizeY);
        glUniform1i(Renderer.gui.uniforms.get("layer"), 1);

        Vector2f clampedPos = confineToMenu(hotbarPosX, hotbarPosY, hotbarSizeX, hotbarSizeY*4);
        HandManager.selectedSlot = isInventoryOpen ? new Vector2i((int)(clampedPos.x()*9), (int)(clampedPos.y()*4)) : new Vector2i(HandManager.hotbarSlot, 0);
        float selectedPosX = HandManager.selectedSlot.x()*(slotSizeX/guiScale);
        float selectedPosY = HandManager.selectedSlot.y()*((slotSizeY/guiScale)*aspectRatio);
        drawQuad(false, false, selectedPosX+hotbarPosX, selectedPosY+(hotbarPosY-(3.f/height)), 24, 23);
    }

    public static Vector2f confineToMenu(float posX, float posY, int sizeX, int sizeY) {
        return new Vector2f(
                relative(cursorX(), posX, sizeX/guiScale),
                relative(cursorY(), posY, (sizeY/guiScale)*aspectRatio)
        );
    }

    public static float relative(float cursor, float pos, float size) {
        return (Math.clamp(cursor, pos, pos+size-(1f/width))-pos)*(1/size);
    }

    public static float cursorX() {
        return Main.mouseInput.getCurrentPos().x()/width;
    }
    public static float cursorY() {
        return Math.abs(height-Main.mouseInput.getCurrentPos().y())/height;
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
