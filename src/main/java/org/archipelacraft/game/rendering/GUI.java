package org.archipelacraft.game.rendering;

import org.archipelacraft.Main;
import org.archipelacraft.engine.Utils;
import org.archipelacraft.engine.Window;
import org.archipelacraft.game.gameplay.HandManager;
import org.archipelacraft.game.items.Item;
import org.archipelacraft.game.items.ItemType;
import org.archipelacraft.game.items.ItemTypes;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL45.glBindTextureUnit;

public class GUI {
    public static float guiScale = 600f;
    public static float aspectRatio = 0f;
    public static int width = 0;
    public static int height = 0;

    public static int hotbarSizeX = 182;
    public static int hotbarSizeY = 22;
    public static float hotbarPosX = 0.f;
    public static float hotbarPosY = 0.f;
    public static int slotSize = 20;
    public static int slotSizeY = 22;
    public static int enlargedSlotSize = 24;

    public static void draw(Window window) {
        glBindTextureUnit(1, Textures.gui.id);
        width = window.getWidth();
        height = window.getHeight();
        aspectRatio = (float) width / height;
        hotbarPosX = (0.5f-((182/2f)/guiScale));
        hotbarPosY = 5.f/height;
        glUniform2i(Renderer.gui.uniforms.get("atlasOffset"), 0, 0);
        glUniform1i(Renderer.gui.uniforms.get("layer"), 0); //inventory
        if (Main.player.inv.open) {
            glUniform4f(Renderer.gui.uniforms.get("color"), 0.85f, 0.85f, 0.85f, 0.85f);
            drawQuad(false, false, hotbarPosX, hotbarPosY + ((hotbarSizeY / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY, 1);
            drawQuad(false, false, hotbarPosX, hotbarPosY + (((hotbarSizeY*2) / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY, 1);
            drawQuad(false, false, hotbarPosX, hotbarPosY + (((hotbarSizeY*3) / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY, 1);
        }
        glUniform4f(Renderer.gui.uniforms.get("color"), 1.f, 1.f, 1.f, 1.f); //hotbar
        drawQuad(false, false, hotbarPosX, hotbarPosY, hotbarSizeX, hotbarSizeY, 1);

        glUniform1i(Renderer.gui.uniforms.get("layer"), 1); //selector
        Vector2f clampedPos = confineToMenu(hotbarPosX, hotbarPosY, hotbarSizeX, hotbarSizeY*4);
        Main.player.inv.selectedSlot = Main.player.inv.open ? new Vector2i((int)(clampedPos.x()*9), (int)(clampedPos.y()*4)) : new Vector2i(HandManager.hotbarSlot, 0);
        drawSlot(0, -1, Main.player.inv.selectedSlot.x(), Main.player.inv.selectedSlot.y(), enlargedSlotSize, 1.f);

        glUniform1i(Renderer.gui.uniforms.get("layer"), 0); //items
        glBindTextureUnit(1, Textures.items.id);
        for (int y = 0; y < (Main.player.inv.open ? 4 : 1); y++) {
            for (int x = 0; x < 9; x++) {
                Item item = Main.player.inv.getItem(x, y);
                if (item != null) {
                    ItemType itemType = item.type;
                    if (itemType != ItemTypes.AIR) {
                        glUniform2i(Renderer.gui.uniforms.get("atlasOffset"), itemType.atlasOffset.x(), itemType.atlasOffset.y());
                        drawSlot(3 + (x * slotSize), 3 + (y * slotSizeY), 0, 0, ItemTypes.itemTexSize, 1.f);
//                        if (item.amount > 1) {
//                            drawSlot(1 + (x * slotSize), 2 + (y * slotSizeY), 0, 0, ItemTypes.itemTexSize, 0.9f);
//                            if (item.amount > 2) {
//                                drawSlot(5 + (x * slotSize), 1 + (y * slotSizeY), 0, 0, ItemTypes.itemTexSize, 0.7f);
//                            }
//                        }
                    }
                }
            }
        }
        if (Main.player.inv.cursorItem != null) { //cursor item
            ItemType itemType = Main.player.inv.cursorItem.type;
            glUniform2i(Renderer.gui.uniforms.get("atlasOffset"), itemType.atlasOffset.x(), itemType.atlasOffset.y());
            drawQuad(true, true, Main.mouseInput.getCurrentPos().x()/width, Math.abs(1-(Main.mouseInput.getCurrentPos().y()/height)), ItemTypes.itemTexSize, ItemTypes.itemTexSize, 1.f);
        }
    }

    public static void drawSlot(float offsetX, float offsetY, int x, int y, int size, float quadScale) {
        float selectedPosX = x*(slotSize/guiScale);
        float selectedPosY = y*((slotSizeY/guiScale)*aspectRatio);
        drawQuad(false, false, selectedPosX+hotbarPosX+(offsetX/guiScale), selectedPosY+(hotbarPosY-(3.f/height))+((offsetY/guiScale)*aspectRatio), size, size, quadScale);
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

    public static void drawQuad(boolean centeredX, boolean centeredY, float x, float y, int scaleX, int scaleY, float quadScale) {
        float xScale = (scaleX/guiScale);
        float yScale = (scaleY/guiScale)*aspectRatio;
        float xOffset = ((x*2)-1)+(centeredX ? 0 : xScale);
        float yOffset = ((y*2)-1)+(centeredY ? 0 : yScale);
        glUniform2i(Renderer.gui.uniforms.get("offset"), (int)((x-(centeredX ? xScale/2 : 0))*width), (int)((y+(centeredY ? yScale/2 : 0))*height));
        glUniform2i(Renderer.gui.uniforms.get("size"), scaleX, scaleY);
        glUniform2i(Renderer.gui.uniforms.get("scale"), (int)(xScale*width), (int)(yScale*height));
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(Renderer.gui.uniforms.get("model"), false, new Matrix4f().translate(xOffset, yOffset, 0.f).scale(xScale*quadScale, yScale*quadScale, 1).get(stack.mallocFloat(16)));
        }
        glBindVertexArray(Models.CUBE.vaoId);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glDrawArrays(GL_TRIANGLES, 0, Models.CUBE.positions.length);
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
    }

    public static void fillTexture() throws IOException {
        glBindTexture(GL_TEXTURE_3D, Textures.gui.id);
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, Textures.gui.width, Textures.gui.height, ((Texture3D)(Textures.gui)).depth, 0, GL_RGBA, GL_FLOAT,
                new float[Textures.gui.width*Textures.gui.height*((Texture3D)(Textures.gui)).depth*4]);
        File guiTextureFolder = new File(Renderer.class.getClassLoader().getResource("assets/base/gui/texture").toString().substring(5));
        File[] guiTextures = guiTextureFolder.listFiles();
        int guiTextureDepth = 0;
        for (File file : guiTextures) {
            BufferedImage img = ImageIO.read(file);
            glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, guiTextureDepth, img.getWidth(), img.getHeight(), 1, GL_RGBA, GL_UNSIGNED_BYTE,
                    Utils.imageToBuffer(img));
            guiTextureDepth++;
        }

        ItemTypes.fillTexture();
    }
}
