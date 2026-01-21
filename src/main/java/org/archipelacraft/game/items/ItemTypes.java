package org.archipelacraft.game.items;

import org.archipelacraft.engine.Utils;
import org.archipelacraft.game.rendering.Renderer;
import org.archipelacraft.game.rendering.Texture3D;
import org.archipelacraft.game.rendering.Textures;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;

public class ItemTypes {
    public static int itemTexSize = 16;
    public static Map<Integer, ItemType> itemTypeMap = new HashMap<>(Map.of());

    public static int getId(ItemType type) {
        int id = 0;
        for (ItemType mapBlocKType : itemTypeMap.values()) {
            if (mapBlocKType.equals(type)) {
                return id;
            }
            id++;
        }
        return 0;
    }

    public static ItemType
            AIR = create(new ItemType("misc/texture/air").maxStackSize(1)),
            STEEL_SCYTHE = create(new ItemType("tool/steel/texture/scythe").maxStackSize(1)),
            STEEL_PICK = create(new ItemType("tool/steel/texture/pick").maxStackSize(1)),
            STEEL_HATCHET = create(new ItemType("tool/steel/texture/hatchet").maxStackSize(1)),
            STEEL_SPADE = create(new ItemType("tool/steel/texture/spade").maxStackSize(1)),
            STEEL_HOE = create(new ItemType("tool/steel/texture/hoe").maxStackSize(1)),
            APPLE = create(new ItemType("food/texture/apple").maxStackSize(2)),
            ORANGE = create(new ItemType("food/texture/orange").maxStackSize(2)),
            CHERRY = create(new ItemType("food/texture/cherry").maxStackSize(2)),
            OAK_LOG = create(new ItemType("resource/texture/oak_log").maxStackSize(64)),
            STICK = create(new ItemType("resource/texture/stick").maxStackSize(64)),
            STONE = create(new ItemType("resource/texture/stone").maxStackSize(64));

    private static ItemType create(ItemType type) {
        itemTypeMap.put(itemTypeMap.size(), type);
        return type;
    }

    public static void fillTexture() throws IOException {
        glBindTexture(GL_TEXTURE_3D, Textures.items.id);
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, Textures.items.width, Textures.items.height, ((Texture3D)(Textures.items)).depth, 0, GL_RGBA, GL_FLOAT,
                new float[Textures.items.width*Textures.items.height*((Texture3D)(Textures.items)).depth*4]);

        int xOffset = 0;
        int yOffset = 0;
        for (ItemType itemType : itemTypeMap.values()) {
            glTexSubImage3D(GL_TEXTURE_3D, 0, xOffset, yOffset, 0, itemTexSize, itemTexSize, 1, GL_RGBA, GL_UNSIGNED_BYTE,
                    Utils.imageToBuffer(ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/item/"+itemType.name+".png"))));
            itemType.atlasOffset(xOffset, yOffset);
            xOffset += itemTexSize;
            if (xOffset >= 4096) {
                xOffset = 0;
                yOffset += itemTexSize;
            }
        }
    }
}
