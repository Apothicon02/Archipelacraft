package org.archipelacraft.game.rendering;

import org.archipelacraft.Main;
import org.archipelacraft.engine.Engine;
import org.archipelacraft.engine.Utils;
import org.archipelacraft.engine.Window;
import org.archipelacraft.game.gameplay.HandManager;
import org.archipelacraft.game.items.Item;
import org.archipelacraft.game.items.ItemType;
import org.archipelacraft.game.items.ItemTypes;
import org.archipelacraft.game.world.World;
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
    public static float guiScaleMul = 4f; //even-though it's a float, should always be set to a round number to prevent distortion of pixel-art
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

    public static void updateGUI(Window window) {
        glBindTextureUnit(0, Textures.sceneColor.id);
        glBindTextureUnit(1, Textures.blurred.id);
        glBindTextureUnit(2, Textures.gui.id);
        glBindTextureUnit(3, Textures.items.id);
        width = window.getWidth();
        height = window.getHeight();
        guiScale = width/guiScaleMul;
        aspectRatio = (float) width / height;
    }

    public static void drawAlwaysVisible(Window window) {
        if (Main.isSaving) {
            glUniform4f(Renderer.gui.uniforms.get("color"), 1.f, 1.f, 1.f, 1.f);
            glUniform1i(Renderer.gui.uniforms.get("tex"), 0); //use gui atlas
            Vector2i border = new Vector2i((int) ((32*(width/3840f))/guiScaleMul), (int) ((32*(height/2180f))/guiScaleMul));
            drawText(0, 0, 2+border.x(), 2+border.y(), "Saving data...".toCharArray());
            if (Main.isSwappingWorldType) {
                drawText(0, 1, 2+border.x(), -2 - border.y() - charHeight, ("Travelling to "+World.nextWorldType.getWorldTypeName()+" from "+World.worldType.getWorldTypeName()).toCharArray());
                glUniform1i(Renderer.gui.uniforms.get("layer"), 3); //frame
                glUniform2i(Renderer.gui.uniforms.get("size"), 3840, 2160);
                glUniform2i(Renderer.gui.uniforms.get("scale"), width, (int)(height*aspectRatio));
                glUniform2i(Renderer.gui.uniforms.get("atlasOffset"), 0, 0);
                try(MemoryStack stack = MemoryStack.stackPush()) {
                    glUniformMatrix4fv(Renderer.gui.uniforms.get("model"), false, new Matrix4f().get(stack.mallocFloat(16)));
                }
                Renderer.draw();
            }
        }
    }

    public static void drawDebug(Window window) {
        glUniform4f(Renderer.gui.uniforms.get("color"), 1.f, 1.f, 1.f, 0.5f);
        if (!Main.isSwappingWorldType) {
            glUniform1i(Renderer.gui.uniforms.get("tex"), 0); //use gui atlas
            drawText(0, 1, 2, -2 - charHeight, ((long) (Engine.avgMS) + "fps ").toCharArray());
            drawText(0, 1, 2, -2 - (charHeight * 2), (String.format("%.1f", 1000d / (Engine.avgMS)) + "ms").toCharArray());
            drawText(0, 1, 2, -2 - (charHeight * 3), ((int) Main.player.pos.x + "x," + (int) Main.player.pos.y + "y," + (int) Main.player.pos.z + "z").toCharArray());
        }
    }

    public static void drawText(float offsetX, float offsetY, float offsetPX, float offsetPY, char[] chars) {
        float offset = 0;
        for (char character : chars) {
            int charAtlasOffset = getCharAtlasOffset(character);
            if (charAtlasOffset >= 0) {
                glUniform2i(Renderer.gui.uniforms.get("atlasOffset"), charAtlasOffset, 0);
                drawSlot(offsetX, offsetY, offsetPX+offset, offsetPY, 0, 0, charWidth, charHeight);
            }
            offset += charWidth;
        }
    }

    public static void draw(Window window) {
        hotbarPosX = (0.5f-((182/2f)/guiScale));
        hotbarPosY = 5.f/height;
        glUniform2i(Renderer.gui.uniforms.get("atlasOffset"), 0, 0);
        glUniform1i(Renderer.gui.uniforms.get("tex"), 0);
        glUniform1i(Renderer.gui.uniforms.get("layer"), 1); //inventory
        float containerPosY = 0.5f;
        if (Main.player.inv.open) {
            glUniform4f(Renderer.gui.uniforms.get("color"), 0.85f, 0.85f, 0.85f, 0.85f);
            drawQuad(false, false, hotbarPosX, hotbarPosY + ((hotbarSizeY / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY);
            drawQuad(false, false, hotbarPosX, hotbarPosY + (((hotbarSizeY*2) / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY);
            drawQuad(false, false, hotbarPosX, hotbarPosY + (((hotbarSizeY*3) / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY);
            if (Main.player.creative) {
                glUniform4f(Renderer.gui.uniforms.get("color"), 1, 1, 1, 1);
                drawQuad(false, false, hotbarPosX, containerPosY, hotbarSizeX, hotbarSizeY);
                drawQuad(false, false, hotbarPosX, containerPosY + ((hotbarSizeY / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY);
                drawQuad(false, false, hotbarPosX, containerPosY + (((hotbarSizeY * 2) / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY);
                drawQuad(false, false, hotbarPosX, containerPosY + (((hotbarSizeY * 3) / guiScale) * aspectRatio), hotbarSizeX, hotbarSizeY);
            }
        }
        glUniform4f(Renderer.gui.uniforms.get("color"), 1.f, 1.f, 1.f, 1.f);
        drawQuad(false, false, hotbarPosX, hotbarPosY, hotbarSizeX, hotbarSizeY); //hotbar
        glUniform1i(Renderer.gui.uniforms.get("layer"), 2); //selector
        Vector2i selSlot;
        if (Main.player.inv.open) {
            Vector2f clampedPos = confineToMenu(hotbarPosX, hotbarPosY, hotbarSizeX, hotbarSizeY*4);
            if (clampedPos.x() > -1 && clampedPos.y() > -1) {
                Main.player.inv.selectedSlot = new Vector2i((int) (clampedPos.x() * 9), (int) (clampedPos.y() * 4));
                selSlot = Main.player.inv.selectedSlot;
            } else {
                Main.player.inv.selectedSlot = null;
                clampedPos = confineToMenu(hotbarPosX, containerPosY, hotbarSizeX, hotbarSizeY*4);
                Main.player.inv.selectedContainerSlot = new Vector2i((int) (clampedPos.x() * 9), (int) (clampedPos.y() * 4));
                selSlot = Main.player.inv.selectedContainerSlot;
            }
        } else {
            Main.player.inv.selectedSlot = new Vector2i(HandManager.hotbarSlot, 0);
            selSlot = Main.player.inv.selectedSlot;
        }
        if (selSlot.x() < 0 || selSlot.y() < 0) {
            selSlot.set(-1, -1);
        } else {
            drawSlot(hotbarPosX, selSlot == Main.player.inv.selectedContainerSlot ? containerPosY : hotbarPosY, 0, -1, selSlot.x(), selSlot.y(), enlargedSlotSize, enlargedSlotSize);
        }

        glUniform1i(Renderer.gui.uniforms.get("layer"), 0); //items
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
                            float startOffset = 16-(chars.length*charWidth);
                            drawText(hotbarPosX, hotbarPosY, offX+startOffset, offY+1, chars);
                        }
                    }
                }
            }
        }
        if (Main.player.inv.open && Main.player.creative) {
            boolean isFirstSlot = true;
            int itemId = 0;
            done:
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 9; x++) {
                    ItemType itemType = ItemTypes.itemTypeMap.get(itemId);
                    glUniform1i(Renderer.gui.uniforms.get("tex"), isFirstSlot ? 0 : 1); //use item atlas unless first slot then use gui atlas
                    if (isFirstSlot) {
                        glUniform1i(Renderer.gui.uniforms.get("layer"), 4); //trash
                        glUniform2i(Renderer.gui.uniforms.get("atlasOffset"), 0, 0);
                    } else {
                        glUniform1i(Renderer.gui.uniforms.get("layer"), 0); //items
                        glUniform2i(Renderer.gui.uniforms.get("atlasOffset"), itemType.atlasOffset.x(), itemType.atlasOffset.y());
                    }
                    int offX = 3 + (x * slotSize);
                    int offY = 3 + (y * slotSizeY);
                    drawSlot(hotbarPosX, containerPosY, offX, offY, 0, 0, ItemTypes.itemTexSize, ItemTypes.itemTexSize);
                    itemId++;
                    if (itemId >= ItemTypes.itemTypeMap.size()) {
                        break done;
                    }
                    isFirstSlot = false;
                }
            }
        }
        glUniform1i(Renderer.gui.uniforms.get("tex"), 1); //use item atlas
        if (Main.player.inv.cursorItem != null) { //cursor item
            ItemType itemType = Main.player.inv.cursorItem.type;
            glUniform2i(Renderer.gui.uniforms.get("atlasOffset"), itemType.atlasOffset.x(), itemType.atlasOffset.y());
            float offX = Engine.window.currentPos.x()/width;
            float offY = Math.abs(height-(Engine.window.currentPos.y()))/height;
            drawQuad(true, true, offX, offY, ItemTypes.itemTexSize, ItemTypes.itemTexSize);
            if (Main.player.inv.cursorItem.amount > 1) {
                glUniform1i(Renderer.gui.uniforms.get("tex"), 0); //use gui atlas
                char[] chars = String.valueOf(Main.player.inv.cursorItem.amount).toCharArray();
                float startOffset = 16-(chars.length*charWidth);
                drawText(offX, offY, 1+startOffset-(charWidth*1.5f), 1-charHeight, chars);
            }
        }
        if (Main.showDebug) {
            drawDebug(window);
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
        return (cut(cursor, pos, pos+size-(1f/width))-pos)*(1/size);
    }
    public static float cut(float in, float min, float max) {
        if (in < min || in > max) {
            return -1;
        }
        return in;
    }
    public static float cursorX() {
        return Engine.window.currentPos.x()/width;
    }
    public static float cursorY() {
        return Math.abs(height-Engine.window.currentPos.y())/height;
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
        glBindVertexArray(Models.QUAD_UNNORMALIZED.vaoId);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glDrawArrays(GL_TRIANGLES, 0, Models.QUAD_UNNORMALIZED.positions.length);
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
    }

    public static int charWidth = 6;
    public static int charHeight = 8;
    public static char[] alphabet = """
                0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz.!?$:,;`'"()[]{}*=+-/\\^%&#~<>|
                """.toCharArray();
    public static Map<Character, Integer> charAtlasOffsetIndex = new HashMap<>();
    public static char space = " ".toCharArray()[0];
    public static int getCharAtlasOffset(char character) {
        return character == space ? -1 : charAtlasOffsetIndex.get(character);
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
        loadImage("texture/frame");
        loadImage("texture/trash");

        ItemTypes.fillTexture();
    }

    public static int guiTexDepth = 0;
    public static void loadImage(String path) throws IOException {
        BufferedImage img = ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/gui/"+path+".png"));
        glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, guiTexDepth, img.getWidth(), img.getHeight(), 1, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(img));
        guiTexDepth++;
    }
}
