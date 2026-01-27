package org.archipelacraft.game.gameplay;

import org.archipelacraft.engine.MouseInput;
import org.archipelacraft.game.audio.BlockSFX;
import org.archipelacraft.game.blocks.Fluids;
import org.archipelacraft.game.blocks.Tags;
import org.archipelacraft.game.blocks.drops.BlockDrops;
import org.archipelacraft.game.blocks.types.BlockProperties;
import org.archipelacraft.game.blocks.types.BlockType;
import org.archipelacraft.game.blocks.types.BlockTypes;
import org.archipelacraft.game.blocks.types.FullBucketBlockType;
import org.archipelacraft.game.items.Item;
import org.archipelacraft.game.world.World;
import org.joml.*;

import java.lang.Math;

import static org.archipelacraft.Main.player;

public class HandManager {
    public static long lastBlockBrokenOrPlaced = 0L;
    public static long lastBlockPlaced = 0L;
    public static long lastBlockBreakCheck = 0;
    public static int hotbarSlot = 0;
    public static Vector4i blockStartedBreaking = new Vector4i();

    public static void useHands(long timeMillis, MouseInput mouseInput) {
        if (mouseInput.scroll.y > 0) {
            hotbarSlot++;
            if (hotbarSlot > 8) {
                hotbarSlot = 0;
            }
        } else if (mouseInput.scroll.y < 0) {
            hotbarSlot--;
            if (hotbarSlot < 0) {
                hotbarSlot = 8;
            }
        }
        boolean lmbDown = mouseInput.isLeftButtonPressed();
        boolean mmbDown = mouseInput.isMiddleButtonPressed();
        boolean rmbDown = mouseInput.isRightButtonPressed();
        if (!lmbDown) {
            player.breakingSource.stop();
            blockStartedBreaking.set(0, 0, 0, 0);
        }
        if ((!player.creative || (timeMillis - lastBlockBrokenOrPlaced >= 200)) && (!rmbDown || timeMillis - lastBlockPlaced >= 200)) { //two tenth second minimum delay between breaking blocks in creative or when placing blocks
            if (lmbDown || mmbDown || rmbDown) {
                Item selectedItem = player.inv.getItem(player.inv.selectedSlot);
                Vector2i blockToPlace = selectedItem == null ? new Vector2i(0) : selectedItem.place();
                Vector2i handBlock = new Vector2i(blockToPlace.x, blockToPlace.y);
                BlockType handType = BlockTypes.blockTypeMap.get(blockToPlace.x);
                Vector3f pos = lmbDown || mmbDown ? player.selectedBlock : player.prevSelectedBlock;
                if (pos != null && World.inBounds((int) pos.x, (int) pos.y, (int) pos.z)) {
                    if (mmbDown) {
                        Vector2i block = World.getBlock(pos.x, pos.y, pos.z);
                        if (block != null) {
                            if (BlockTypes.blockTypeMap.get(block.x).blockProperties.isFluid) {
                                block.x = Fluids.fluidBucketMap.get(block.x);
                            }
                            if (player.creative) {
                                //StackManager.setFirstEntryInStack(block);
                            } else {
                                //StackManager.cycleToEntryInStack(block);
                            }
                        }
                    } else  {
                        lastBlockBrokenOrPlaced = timeMillis;
                        int cornerData = World.getCorner((int) pos.x, (int) pos.y, (int) pos.z);
                        int cornerIndex = (pos.y < (int)(pos.y)+0.5 ? 0 : 4) + (pos.z < (int)(pos.z)+0.5 ? 0 : 2) + (pos.x < (int)(pos.x)+0.5 ? 0 : 1);
                        if (lmbDown) {
                            cornerData |= (1 << (cornerIndex - 1));
                            Vector2i blockBreaking = World.getBlock(pos.x, pos.y, pos.z);
                            BlockType breakingType = BlockTypes.blockTypeMap.get(blockBreaking.x);
                            if (cornerData == -2147483521 || !player.crouching) {
                                Vector3i intBreakingPos = new Vector3i((int) pos.x, (int) pos.y, (int) pos.z);
                                boolean canBreak = breakingType.whilePlayerBreaking(intBreakingPos, blockBreaking, handBlock);
                                if (canBreak && !Tags.cantBreakBlocks.tagged.contains(blockToPlace.x)) {
                                    if (!player.creative) {
                                        boolean sameBlock = blockStartedBreaking.x == (int)(pos.x) && blockStartedBreaking.y == (int)(pos.y) && blockStartedBreaking.z == (int)(pos.z);
                                        if (sameBlock) {
                                            if (blockStartedBreaking.w > 0) {
                                                canBreak = false;
                                                blockStartedBreaking.sub(0, 0, 0, (int) ((System.currentTimeMillis()-lastBlockBreakCheck)*breakingType.getTTBSpeed(blockToPlace.x)));
                                                lastBlockBreakCheck = System.currentTimeMillis();
                                            }
                                        } else {
                                            canBreak = false;
                                            lastBlockBreakCheck = System.currentTimeMillis();
                                            blockStartedBreaking.set((int) pos.x, (int) pos.y, (int) pos.z, breakingType.getTTB());
                                            BlockSFX sfx = breakingType.blockProperties.blockSFX;
                                            player.breakingSource.setPos(pos);
                                            player.breakingSource.setGain(sfx.placeGain);
                                            player.breakingSource.setPitch(sfx.placePitch*(1+(Math.abs(1-breakingType.getTTBSpeed(blockToPlace.x))*0.8f)), 0);
                                            player.breakingSource.play(sfx.placeIds[(int) (Math.random() * sfx.placeIds.length)], true);
                                        }
                                    }
                                    if (canBreak) {
                                        if (!player.creative) {
                                            for (Item item : BlockDrops.getDrops(blockBreaking)) {
                                                World.items.add(item.moveTo(pos));
                                            }
                                        }
                                        blockStartedBreaking.set(0, 0, 0, 0);
                                        player.breakingSource.stop();
                                        //World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, 0);
                                        World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, 0, 0, true, false, 1, false);
                                        //BlockBreaking.blockBroken(blockBreaking, handBlock);
                                    }
                                }
                            } else if (breakingType.blockProperties.isSolid) {
                                //World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, cornerData);
                            }
                        } else if (rmbDown) {
                            lastBlockPlaced = System.currentTimeMillis();
                            if (cornerData != 0) {
                                cornerData &= (~(1 << (cornerIndex - 1)));
                                //World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, cornerData);
                            } else if (blockToPlace.x > 0) {//player.stack[0] > 0) {
                                Vector2i oldBlock = World.getBlock((int) pos.x, (int) pos.y, (int) pos.z);
                                BlockProperties oldType = BlockTypes.blockTypeMap.get(oldBlock.x).blockProperties;
                                BlockType blockType = BlockTypes.blockTypeMap.get(blockToPlace.x);
                                if (blockType instanceof FullBucketBlockType && !player.crouching) {
                                    blockToPlace.x = Fluids.fluidBucketMap.get(blockToPlace.x);
                                    blockType = BlockTypes.blockTypeMap.get(blockToPlace.x);
                                }
                                if (oldType.isFluidReplaceable || (oldType.isFluid && !Tags.buckets.tagged.contains(blockToPlace.x) && !blockType.blockProperties.isFluidReplaceable && !blockType.blockProperties.isFluid)) {
                                    World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockToPlace.x, blockToPlace.y, true, false, 1, false);
                                    if (!player.creative) {
                                        if (blockType.blockProperties.isFluid) {
                                            blockToPlace.x = BlockTypes.getId(BlockTypes.BUCKET);
                                            blockToPlace.y = 0;
                                            //StackManager.setFirstEntryInStack(new Vector2i(blockTypeId, blockSubtypeId));
                                        } else {
                                            //StackManager.removeFirstEntryInStack();
                                        }
                                    }
                                } else if (oldType.isFluid && blockToPlace.x == oldBlock.x) { //merge liquid
                                    int room = 15-oldBlock.y;
                                    int flow = Math.min(room, blockToPlace.y);
                                    World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockToPlace.x, oldBlock.y+flow, true, false, 1, false);
                                    if (!player.creative) {
                                        blockToPlace.y -= flow;
                                        if (blockToPlace.y < 1) {
                                            blockToPlace.x = BlockTypes.getId(BlockTypes.BUCKET);
                                            blockToPlace.y = 0;
                                        }
                                        //StackManager.setFirstEntryInStack(new Vector2i(blockTypeId, blockSubtypeId));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
