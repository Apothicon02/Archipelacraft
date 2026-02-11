package org.archipelacraft.game.items;

import org.archipelacraft.engine.Utils;
import org.archipelacraft.game.audio.SFX;
import org.archipelacraft.game.audio.Sounds;
import org.archipelacraft.game.blocks.types.BlockTypes;
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
            STEEL_SCYTHE = create(new ItemType("tool/steel/texture/scythe").maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STEEL_PICK = create(new ItemType("tool/steel/texture/pick").maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STEEL_HATCHET = create(new ItemType("tool/steel/texture/hatchet").maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STEEL_SPADE = create(new ItemType("tool/steel/texture/spade").maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STEEL_HOE = create(new ItemType("tool/steel/texture/hoe").maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            APPLE = create(new ItemType("food/texture/apple").maxStackSize(2)),
            ORANGE = create(new ItemType("food/texture/orange").maxStackSize(2)),
            CHERRY = create(new ItemType("food/texture/cherry").maxStackSize(2)),
            OAK_LOG = create(new ItemType("resource/texture/oak_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.OAK_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            BIRCH_LOG = create(new ItemType("resource/texture/birch_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.BIRCH_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            CHERRY_LOG = create(new ItemType("resource/texture/cherry_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.CHERRY_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            MAHOGANY_LOG = create(new ItemType("resource/texture/mahogany_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.MAHOGANY_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            ACACIA_LOG = create(new ItemType("resource/texture/acacia_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.ACACIA_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            PALM_LOG = create(new ItemType("resource/texture/palm_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.PALM_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            SPRUCE_LOG = create(new ItemType("resource/texture/spruce_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.SPRUCE_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            WILLOW_LOG = create(new ItemType("resource/texture/willow_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.WILLOW_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            REDWOOD_LOG = create(new ItemType("resource/texture/redwood_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.REDWOOD_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            STICK = create(new ItemType("resource/texture/stick").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.STICK), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.33f, 1.33f))),
            STONE = create(new ItemType("resource/texture/stone").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.STONE), 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 0.75f))),
            MARBLE = create(new ItemType("resource/texture/marble").blockToPlace(BlockTypes.getId(BlockTypes.MARBLE), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.45f, 0.85f))),
            GLASS = create(new ItemType("component/texture/glass").blockToPlace(BlockTypes.getId(BlockTypes.GLASS), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            MAGENTA_STAINED_GLASS = create(new ItemType("component/texture/magenta_stained_glass").blockToPlace(BlockTypes.getId(BlockTypes.PURPLE_STAINED_GLASS), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            LIME_STAINED_GLASS = create(new ItemType("component/texture/lime_stained_glass").blockToPlace(BlockTypes.getId(BlockTypes.LIME_STAINED_GLASS), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            TORCH = create(new ItemType("block/texture/torch").blockToPlace(BlockTypes.getId(BlockTypes.TORCH), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.4f, 1.25f))),
            MAGMA = create(new ItemType("resource/texture/magma").blockToPlace(BlockTypes.getId(BlockTypes.MAGMA), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.SIZZLE1, Sounds.SIZZLE2}, 0.45f, 0.95f))),
            PORECAP = create(new ItemType("food/texture/porecap").blockToPlace(BlockTypes.getId(BlockTypes.PORECAP), 0).maxStackSize(2).sfx(new ItemSFX(new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 0.45f, 0.95f))),
            KYANITE = create(new ItemType("resource/texture/kyanite").blockToPlace(BlockTypes.getId(BlockTypes.KYANITE), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.6f, 1.2f)));

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
