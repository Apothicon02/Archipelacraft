package org.archipelacraft.game.world.types;

import org.archipelacraft.Main;
import org.archipelacraft.engine.ArchipelacraftMath;
import org.archipelacraft.game.blocks.types.BlockTypes;
import org.archipelacraft.game.blocks.types.LightBlockType;
import org.archipelacraft.game.noise.Noises;
import org.archipelacraft.game.world.LightHelper;
import org.archipelacraft.game.world.shapes.Blob;
import org.archipelacraft.game.world.trees.*;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.nio.file.Path;
import java.util.Random;

import static org.archipelacraft.engine.Utils.condensePos;
import static org.archipelacraft.engine.Utils.distance;
import static org.archipelacraft.game.world.World.*;

public class JungleWorldType extends WorldType {
    private Path worldPath = Path.of(Main.mainFolder+"world0/jungle");
    public static Random seededRand = new Random(35311350L);

    @Override
    public Random rand() {return seededRand;}

    @Override
    public Path getWorldPath() {
        return worldPath;
    }

    @Override
    public String getWorldTypeName() {
        return "Jungle Crater";
    }

    @Override
    public void createNew() {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                float basePerlinNoise = (Noises.COHERERENT_NOISE.sample(x/2, z/2) + 0.5f) / 2;
                float baseCellularNoise = Noises.CELLULAR_NOISE.sample(x, z) / 2;
                float centDist = (float) (distance(x, z, size / 2, size / 2) / halfSize);
                basePerlinNoise *= Math.min(0.2f, 4f*(Math.max(0.2f, centDist)-0.2f))*10;
                float centDistExp = (Math.max(0.5f, centDist) - 0.5f);
                centDistExp *= centDistExp;
                int surface = (int) (((200 * (Math.max(0.1f, baseCellularNoise) * basePerlinNoise)) + 70) - (centDistExp * 300));
                surface = Math.max(8, surface);
                heightmap[condensePos(x, z)] = (short) (surface);
                for (int y = surface; y >= 0; y--) {
                    setBlock(x, y, z, 3, 0);
                }
            }
        }

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int maxSteepness = 0;
                int minNeighborY = height - 1;
                int condensedPos = condensePos(x, z);
                int surface = heightmap[condensedPos];
                for (int pos : new int[]{condensePos(Math.min(size - 1, x + 3), z), condensePos(Math.max(0, x - 3), z), condensePos(x, Math.min(size - 1, z + 3)), condensePos(x, Math.max(0, z - 3)),
                        condensePos(Math.max(0, x - 3), Math.max(0, z - 3)), condensePos(Math.min(size - 1, x + 3), Math.max(0, z - 3)), condensePos(Math.max(0, x - 3), Math.min(size - 1, z + 3)), condensePos(Math.min(size - 1, x + 3), Math.min(size - 1, z + 3))}) {
                    int nY = heightmap[pos];
                    minNeighborY = Math.min(minNeighborY, nY);
                    int steepness = Math.abs(surface - nY);
                    maxSteepness = Math.max(maxSteepness, steepness);
                }
                boolean flat = maxSteepness < 3;
                if (flat) {
                    if (surface < seaLevel) {
                        setBlock(x, seaLevel, z, 1, 13);
                        for (int y = 62; y > surface; y--) {
                            setBlock(x, y, z, 1, 15);
                        }
                    } else if (surface < seaLevel + 3) {
                        setBlock(x, surface, z, BlockTypes.getId(BlockTypes.SAND), 0);
                        for (int newY = surface - 1; newY >= surface - 5; newY--) {
                            setBlock(x, newY, z, BlockTypes.getId(BlockTypes.SANDSTONE), 0);
                        }
                    } else {
                        setBlock(x, surface, z, 2, 1);
                        double flowerChance = seededRand.nextDouble();
                        if (flowerChance < 0.0001f) {
                            setBlock(x, surface+1, z, BlockTypes.getId(BlockTypes.PORECAP), 0);
                        } else {
                            int flower = (flowerChance > 0.95f ? (flowerChance > 0.97f ? 14 : 1) : 0);
                            if (flower > 0 || seededRand.nextFloat() < 0.33f) { //grass has 33% chance to place
                                setBlock(x, surface + 1, z, 4 + flower, seededRand.nextInt(0, 3) + (flower == 0 ? 4 : 0));
                            }
                        }
                    }
                } else {
                    if (surface < seaLevel) {
                        setBlock(x, seaLevel, z, 1, 13);
                        for (int y = 62; y > surface; y--) {
                            setBlock(x, y, z, 1, 15);
                        }
                    }
                    for (int newY = surface; newY >= surface - 5; newY--) {
                        setBlock(x, newY, z, 55, 0);
                    }
                }
            }
        }

        for (int x = (size / 2) - 29; x <= size / 2; x++) {
            for (int z = (size / 2) - 29; z < size / 2; z++) {
                setBlock(x, 100, z, 37, 0);
                if (x == (size / 2) - 29 || x == (size / 2) || z == (size / 2) - 29 || z == (size / 2) - 1) {
                    setBlock(x, 99, z, 37, 0);
                    setBlock(x, 98, z, 37, 0);
                    setBlock(x, 97, z, 37, 0);
                    setBlock(x, 96, z, 37, 0);
                    setBlock(x, 95, z, 37, 0);
                    setBlock(x, 94, z, 37, 0);
                    setBlock(x, 93, z, 37, 0);
                    setBlock(x, 92, z, 37, 0);
                    setBlock(x, 91, z, 37, 0);
                    setBlock(x, 90, z, 37, 0);
                    setBlock(x, 89, z, 37, 0);
                    setBlock(x, 88, z, 37, 0);
                    setBlock(x, 87, z, 37, 0);
                    setBlock(x, 86, z, 37, 0);
                    setBlock(x, 85, z, 37, 0);
                    setBlock(x, 84, z, 37, 0);
                    setBlock(x, 83, z, 37, 0);
                    setBlock(x, 82, z, 37, 0);
                    setBlock(x, 81, z, 37, 0);
                    setBlock(x, 80, z, 37, 0);
                    setBlock(x, 79, z, 37, 0);
                    setBlock(x, 78, z, 37, 0);
                    setBlock(x, 77, z, 37, 0);
                    setBlock(x, 76, z, 37, 0);
                    setBlock(x, 75, z, 37, 0);
                    setBlock(x, 74, z, 37, 0);
                    setBlock(x, 73, z, 37, 0);
                    setBlock(x, 72, z, 37, 0);
                    setBlock(x, 71, z, 37, 0);
                    setBlock(x, 70, z, 37, 0);
                }
            }
        }
        setBlock(512, 95, 512, 37, 0);

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int surface = heightmap[(x * size) + z];
                Vector2i blockOn = getBlock(x, surface, z);
                float basePerlinNoise = Noises.COHERERENT_NOISE.sample(x, z);
                float randomNumber = seededRand.nextFloat();
                if (blockOn.x == 2) {
                    float foliageChanceExp = basePerlinNoise * basePerlinNoise;
                    if (randomNumber * 10 < foliageChanceExp || randomNumber < 0.0002f) { //tree
                        float foliageType = seededRand.nextFloat();
                        if (foliageType < 0.0015f) { //1.5% chance the tree is dead
                            int maxHeight = seededRand.nextInt(6) + 12;
                            DeadOakTree.generate(blockOn, x, surface, z, maxHeight, 47, 0);
                            Blob.generate(blockOn, x, surface, z, 3, 0, (int) ((rand().nextDouble() + 1) * 3), new int[]{2, 23}, true);
                        } else if (foliageType < ArchipelacraftMath.gradient(surface, 82, 100, 1, 0)) {
                            if (randomNumber < 0.2f) { //80% chance to not generate anything
                                int maxHeight = seededRand.nextInt(16) + 12;
                                int radius = seededRand.nextInt(2) + 3;
                                boolean overgrown = seededRand.nextInt(4) == 0;
                                JungleTree.generate(blockOn, x, surface, z, maxHeight, radius, BlockTypes.getId(BlockTypes.CHERRY_LOG), 0, BlockTypes.getId(BlockTypes.CHERRY_LEAVES), 0, overgrown);
                            }
                        } else {
                            if (basePerlinNoise > 0.2f) {
                                int maxHeight = seededRand.nextInt(42, 54);
                                int radius = seededRand.nextInt(3, 4);
                                int branchChance = seededRand.nextInt(4, 7);
                                RedwoodTree.generate(blockOn, x, surface, z, maxHeight, radius, 3, BlockTypes.getId(BlockTypes.REDWOOD_LOG), 0, 36, 0, branchChance);
                            } else {
                                int maxHeight = seededRand.nextInt(19) + 5;
                                PineTree.generate(blockOn, x, surface, z, maxHeight, 35, 0, 36, 0);
                            }
                        }
                    } else if ((randomNumber * 10) + 0.15f < basePerlinNoise - 0.2f || randomNumber < 0.0005f) { //bush
                        int maxHeight = (int) (rand().nextDouble() + 1);
                        OakShrub.generate(blockOn, x, surface, z, maxHeight, 3 + (maxHeight * 2), 16, 0, 17, 0);
                    }
                } else if (blockOn.x == 23) {
                    int foliageChance = seededRand.nextInt(0, 400);
                    if (foliageChance == 0) { //tree
                        PalmTree.generate(blockOn, x, surface, z, seededRand.nextInt(8, 22), 25, 0, 27, 0);
                    } else if (randomNumber < 0.001f) {
                        setBlock(x, surface + 1, z, BlockTypes.getId(BlockTypes.TORCH), 0);
                    }
                } else if (blockOn.x == 55) {
                    if (randomNumber < 0.08f) {
                        Blob.generate(blockOn, x, surface, z, randomNumber < 0.001f ? BlockTypes.getId(BlockTypes.KYANITE) : BlockTypes.getId(BlockTypes.STONE), 0, (int) (2 + (seededRand.nextFloat() * 8)));
                    }
                }
            }
        }

//        for (int x = 0; x < size; x++) {
//            for (int y= 0; y < height; y++) {
//                //setLightNullable(x, y, (size/2)+2, new Vector4i(15, 15, 0, -1));
//                setLightNullable(x, y, (size/2)-2, new Vector4i(0, 0, 15, -1));
//            }
//        }

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                updateHeightmap(x, z);
            }
        }

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                for (int y = seaLevel; y <= heightmap[(x * size) + z] + 1; y++) {
                    Vector2i thisBlock = getBlock(x, y, z);
                    if (BlockTypes.blockTypeMap.get(thisBlock.x) instanceof LightBlockType ||
                            getLight(x, y, z + 1, false).w() > 0 || getLight(x + 1, y, z, false).w() > 0 || getLight(x, y, z - 1, false).w() > 0 ||
                            getLight(x - 1, y, z, false).w() > 0 || getLight(x, y + 1, z, false).w() > 0 || getLight(x, y - 1, z, false).w() > 0) {
                        LightHelper.updateLight(new Vector3i(x, y, z), getBlock(x, y, z), getLight(x, y, z));
                    }
                }
            }
        }
    }
}
