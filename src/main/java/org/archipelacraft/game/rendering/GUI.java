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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL45.glBindTextureUnit;

public class GUI {
    public static float guiScale = 1;
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
        guiScale = width/4f;
        aspectRatio = (float) width / height;
        hotbarPosX = (0.5f-((182/2f)/guiScale));
        hotbarPosY = 5.f/height;
        glUniform2i(Renderer.gui.uniforms.get("atlasOffset"), 0, 0);
        glUniform1i(Renderer.gui.uniforms.get("tex"), 0);
        glUniform1i(Renderer.gui.uniforms.get("layer"), 1); //inventory
        if (Main.player.inv.open) {
            glUniform4f(Renderer.gui.uniforms.get("color"), 0.85f, 0.85f, 0.85f, 0.85f);
            drawQuad(false, false, hotbarPosX, hotbarPosY + ((hotbarSizeY / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY);
            drawQuad(false, false, hotbarPosX, hotbarPosY + (((hotbarSizeY*2) / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY);
            drawQuad(false, false, hotbarPosX, hotbarPosY + (((hotbarSizeY*3) / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY);
        }
        glUniform4f(Renderer.gui.uniforms.get("color"), 1.f, 1.f, 1.f, 1.f); //hotbar
        drawQuad(false, false, hotbarPosX, hotbarPosY, hotbarSizeX, hotbarSizeY);

        glUniform1i(Renderer.gui.uniforms.get("layer"), 2); //selector
        Vector2f clampedPos = confineToMenu(hotbarPosX, hotbarPosY, hotbarSizeX, hotbarSizeY*4);
        Main.player.inv.selectedSlot = Main.player.inv.open ? new Vector2i((int)(clampedPos.x()*9), (int)(clampedPos.y()*4)) : new Vector2i(HandManager.hotbarSlot, 0);
        drawSlot(hotbarPosX, hotbarPosY, 0, -1, Main.player.inv.selectedSlot.x(), Main.player.inv.selectedSlot.y(), enlargedSlotSize, enlargedSlotSize);

        glUniform1i(Renderer.gui.uniforms.get("layer"), 0); //items
        glBindTextureUnit(2, Textures.items.id);
        for (int y = 0; y < (Main.player.inv.open ? 4 : 1); y++) {
            for (int x = 0; x < 9; x++) {
                Item item = Main.player.inv.getItem(x, y);
                if (item != null) {
                    ItemType itemType = item.type;
                    if (itemType != ItemTypes.AIR) {
                        glUniform1i(Renderer.gui.uniforms.get("tex"), 1); //use item atlas
                        glUniform2i(Renderer.gui.uniforms.get("atlasOffset"), itemType.atlasOffset.x(), itemType.atlasOffset.y());
                        int offX = 3 + (x * slotSize);
                        int offY = 3 + (y * slotSizeY);
                        drawSlot(hotbarPosX, hotbarPosY, offX, offY, 0, 0, ItemTypes.itemTexSize, ItemTypes.itemTexSize);
                        if (item.amount > 1) {
                            glUniform1i(Renderer.gui.uniforms.get("tex"), 0); //use gui atlas
                            char[] chars = String.valueOf(item.amount).toCharArray();
                            float startOffset = 16-(chars.length*(charWidth*0.8f));
                            for (char character : chars) {
                                int charAtlasOffset = getCharAtlasOffset(character);
                                glUniform2i(Renderer.gui.uniforms.get("atlasOffset"), charAtlasOffset, 0);
                                drawSlot(hotbarPosX, hotbarPosY, offX+startOffset, offY+1, 0, 0, charWidth, charHeight);
                                startOffset += charWidth*0.8f;
                            }
                        }
                    }
                }
            }
        }
        glUniform1i(Renderer.gui.uniforms.get("tex"), 1); //use item atlas
        if (Main.player.inv.cursorItem != null) { //cursor item
            ItemType itemType = Main.player.inv.cursorItem.type;
            glUniform2i(Renderer.gui.uniforms.get("atlasOffset"), itemType.atlasOffset.x(), itemType.atlasOffset.y());
            float offX = Main.mouseInput.getCurrentPos().x()/width;
            float offY = Math.abs(height-(Main.mouseInput.getCurrentPos().y()))/height;
            drawQuad(true, true, offX, offY, ItemTypes.itemTexSize, ItemTypes.itemTexSize);
            if (Main.player.inv.cursorItem.amount > 1) {
                glUniform1i(Renderer.gui.uniforms.get("tex"), 0); //use gui atlas
                char[] chars = String.valueOf(Main.player.inv.cursorItem.amount).toCharArray();
                float startOffset = 16-(chars.length*(charWidth*0.8f));
                for (char character : chars) {
                    int charAtlasOffset = getCharAtlasOffset(character);
                    glUniform2i(Renderer.gui.uniforms.get("atlasOffset"), charAtlasOffset, 0);
                    drawSlot(offX, offY, 1+startOffset-(charWidth*1.5f), 1-charHeight, 0, 0, charWidth, charHeight);
                    startOffset += charWidth*0.8f;
                }
            }
        }
    }

    public static void drawSlot(float offsetX, float offsetY, float offPxX, float offPxY, int x, int y, int sizeX, int sizeY) {
        float selectedPosX = x*(slotSize/guiScale);
        float selectedPosY = y*((slotSizeY/guiScale)*aspectRatio);
        drawQuad(false, false, selectedPosX+offsetX+(offPxX/guiScale), selectedPosY+(offsetY-(3.f/height))+((offPxY/guiScale)*aspectRatio), sizeX, sizeY);
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

    public static int charWidth = 6;
    public static int charHeight = 7;
    public static char[] alphabet = """
                0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz.!?$:,;`'"()[]{}*=+-/\\^%&#~<>|
                """.toCharArray();
    public static Map<Character, Integer> charAtlasOffsetIndex = new HashMap<>();
    public static int getCharAtlasOffset(char character) {
        return charAtlasOffsetIndex.get(character);
    }

    public static void fillTexture() throws IOException {
        int i = 0;
        for (char character : alphabet) {
            charAtlasOffsetIndex.put(character, i);
            i+=charWidth;
        }
        glBindTexture(GL_TEXTURE_3D, Textures.gui.id);
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, Textures.gui.width, Textures.gui.height, ((Texture3D)(Textures.gui)).depth, 0, GL_RGBA, GL_FLOAT,
                new float[Textures.gui.width*Textures.gui.height*((Texture3D)(Textures.gui)).depth*4]);
        loadImage("texture/font");
        loadImage("texture/hotbar");
        loadImage("texture/selected_slot");

        ItemTypes.fillTexture();
    }

    public static int guiTexDepth = 0;
    public static void loadImage(String path) throws IOException {
        BufferedImage img = ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/gui/"+path+".png"));
        glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, guiTexDepth, img.getWidth(), img.getHeight(), 1, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(img));
        guiTexDepth++;
    }
}
