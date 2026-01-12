package org.archipelacraft.game.gameplay;

import org.archipelacraft.Main;
import org.archipelacraft.engine.Camera;
import org.archipelacraft.engine.Constants;
import org.archipelacraft.engine.Utils;
import org.archipelacraft.game.audio.Sounds;
import org.archipelacraft.game.audio.Source;
import org.archipelacraft.game.blocks.Tags;
import org.archipelacraft.game.blocks.types.BlockTypes;
import org.archipelacraft.game.rendering.Renderer;
import org.archipelacraft.game.world.World;
import org.joml.*;

import java.lang.Math;

public class Player {
    private final Camera camera = new Camera();
    public Vector3f pos = new Vector3f();
    public Vector3f selectedBlock = new Vector3f();
    public Vector3f prevSelectedBlock = new Vector3f();
    public int[] stack = new int[32];

    public boolean bobbingDir = true;
    public float bobbing = 0f;
    public float dynamicSpeedOld = 0;
    public float dynamicSpeed = 0;
    public float scale = 1f;
    public float baseEyeHeight = 1.625f*scale;
    public float eyeHeight = baseEyeHeight;
    public float baseHeight = eyeHeight+(0.175f*scale);
    public float height = baseHeight;
    public float width = 0.4f*scale;
    public float baseSpeed = Math.max(0.15f, 0.15f*scale);
    public float speed = baseSpeed;
    public float sprintSpeed = 1.5f;
    public boolean onGround = false;
    public boolean crawling = false;
    public boolean crouching = false;
    public boolean sprint = false;
    public boolean superSprint = false;
    public boolean forward = false;
    public boolean backward = false;
    public boolean rightward = false;
    public boolean leftward = false;
    public boolean upward = false;
    public boolean downward = false;
    public boolean flying = true;
    public boolean creative = true;

    public final Source breakingSource;
    public final Source jumpSource;
    public final Source passthroughSource;
    public final Source swimSource;
    public final Source splashSource;
    public final Source submergeSource;
    public final Source waterFlowingSource;
    public final Source magmaSource;
    public final Source windSource;

    public Player(Vector3f newPos) {
        breakingSource = new Source(newPos, 1, 1, 0, 1);
        jumpSource = new Source(newPos, 1, 1, 0, 0);
        passthroughSource = new Source(newPos, 1, 1, 0, 0);
        swimSource = new Source(newPos, 0.5f, 1, 0, 0);
        splashSource = new Source(newPos, 1, 1, 0, 0);
        submergeSource = new Source(newPos, 1, 1, 0, 0);
        waterFlowingSource = new Source(newPos, 0, 1, 0, 1);
        windSource = new Source(newPos, 0, 1, 0, 1);
        magmaSource = new Source(newPos, 0, 1, 0, 1);
        setPos(newPos);
        oldPos = newPos;
    }

    public Vector2i blockOn = new Vector2i(0);
    public Vector2i blockIn = new Vector2i(0);
    public Vector2i blockBreathing = new Vector2i(0);
    boolean submerged = false;

    public float bounciness = 0.66f;
    public float friction = 0.75f;
    public float grav = 0.05f;
    public float jumpStrength = Math.max(0.33f, 0.33f*scale);
    public long lastJump = 1000;
    public long jump = 0;
    public boolean setSolidBlockOn = false;
    public boolean solid(float x, float y, float z, boolean recordFriction, boolean recordBounciness) {
        Vector2i block = World.getBlockNotNull(x, y, z);
        if (block != null) {
            int typeId = block.x;
            if (BlockTypes.blockTypeMap.get(typeId).blockProperties.isCollidable) {
                int cornerData = World.getCorner((int) x, (int) y, (int) z);
                int cornerIndex = (y < (int)(y)+0.5 ? 0 : 4) + (z < (int)(z)+0.5 ? 0 : 2) + (x < (int)(x)+0.5 ? 0 : 1);
                int temp = cornerData;
                temp &= (~(1 << (cornerIndex - 1)));
                if (temp == cornerData) {
                    if (Renderer.collisionData[(1024 * ((typeId * 8) + (int) ((x - Math.floor(x)) * 8))) + (block.y() * 64) + ((Math.abs(((int) ((y - Math.floor(y)) * 8)) - 8) - 1) * 8) + (int) ((z - Math.floor(z)) * 8)]) {
                        if (recordFriction) {
                            if (typeId == 7) { //kyanite
                                friction = Math.min(friction, 0.95f);
                            } else if (typeId == 11 || typeId == 12 || typeId == 13) { //glass
                                friction = Math.min(friction, 0.85f);
                            } else if (Tags.planks.tagged.contains(block.x)) { //wood
                                friction = Math.min(friction, 0.5f);
                            } else if (BlockTypes.blockTypeMap.get(typeId).blockProperties.isCollidable) {
                                friction = Math.min(friction, 0.75f);
                            }
                            blockOn = block;
                            setSolidBlockOn = true;
                        }
                        if (recordBounciness) {
                            if (typeId == 7) { //kyanite
                                bounciness = Math.min(bounciness, -2f);
                            } else if (typeId == 11 || typeId == 12 || typeId == 13) { //glass
                                bounciness = Math.min(bounciness, -0.33f);
                            }
                        }
                        return true;
                    }
                }
            }
            if (!setSolidBlockOn && recordFriction) {
                blockOn = block;
            }
        }
        return false;
    }
    public boolean solid(float x, float y, float z, float w, float h, boolean recordFriction, boolean recordBounciness) {
        setSolidBlockOn = false;
        boolean returnValue = false;
        for (float newX = x-w; newX <= x+w; newX+=0.125f) {
            for (float newY = y; newY <= y + h; newY += 0.125f) {
                for (float newZ = z - w; newZ <= z + w; newZ += 0.125f) {
                    if (solid(newX, newY, newZ, recordFriction, recordBounciness)) {
                        if (recordFriction) {
                            returnValue = true;
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
        return returnValue;
    }

    public final Vector3f oldCamOffset = new Vector3f();
    public Vector3f oldPos;
    public Vector3i blockPos;
    public Vector3f vel = new Vector3f(0f);
    public Vector3f movement = new Vector3f(0f);
    public float prevStrafe = 0.f;
    public float strafe = 0.f;
    public float prevTilt = 0.f;
    public float tilt = 0.f;
    public void tick() {
        if (!creative) {
            flying = false;
        }
        friction = 1f;
        boolean prevOnGround = onGround;
        onGround = solid(pos.x, pos.y-0.125f, pos.z, width, 0.125f, true, false);
        if (flying || !onGround) {
            onGround = false;
            crouching = false;
            crawling = false;
        } else if (!prevOnGround) { //when landing from a fall
            bobbing = height*-0.05f;
            bobbingDir = false;
        }
        prevStrafe = strafe;
        prevTilt = tilt;
        if (onGround) {
            if (rightward) {
                strafe = sprint ? -0.02f : -0.01f;
            } else if (leftward) {
                strafe = sprint ? 0.02f : 0.01f;
            } else {
                strafe = 0.f;
            }
            if (forward) {
                tilt = sprint ? -0.02f : -0.01f;
            } else if (backward) {
                tilt = sprint ? 0.02f : 0.01f;
            } else {
                tilt = 0.f;
            }
        }
        new Matrix4f(camera.getViewMatrix()).getTranslation(oldCamOffset);
        blockBreathing = World.getBlockNotNull(blockPos.x, blockPos.y+eyeHeight, blockPos.z);
        speed = baseSpeed;
        boolean hittingCeiling = solid(pos.x, pos.y+height, pos.z, width, 0.125f, false, false);
        boolean mightBeCrawling = false;
        if (!crouching && !hittingCeiling) {
            height = Math.min(height+0.125f, baseHeight);
            eyeHeight = Math.min(eyeHeight+0.125f, baseEyeHeight);
        } else {
            mightBeCrawling = true;
        }
        if (crouching || mightBeCrawling) {
            if (sprint) {
                crawling = true;
            } else {
                crawling = false;
            }
            if (!crawling) {
                if (height < baseHeight * 0.83f) {
                    speed = baseSpeed*0.5f;
                    if (!hittingCeiling) {
                        height = Math.min(height + 0.125f, baseHeight * 0.83f);
                        eyeHeight = Math.min(eyeHeight + 0.125f, baseEyeHeight * 0.83f);
                    }
                } else {
                    speed = baseSpeed * 0.75f;
                    if (crouching) {
                        height = Math.max(height - 0.125f, baseHeight * 0.83f);
                        eyeHeight = Math.max(eyeHeight - 0.125f, baseEyeHeight * 0.83f);
                    }
                }
            } else {
                speed = baseSpeed*0.5f;
                height = Math.max(height-0.125f, baseHeight*0.5f);
                eyeHeight = Math.max(eyeHeight-0.125f, baseEyeHeight*0.5f);
            }

            sprint = false;
            superSprint = false;
        }
        blockIn = World.getBlockNotNull(blockPos.x, blockPos.y, blockPos.z);
        submerged = BlockTypes.blockTypeMap.get(blockBreathing.x).blockProperties.isFluid;
        if (blockIn.x == 0 && onGround && sprint) {
            Vector2i blockBelow = World.getBlockNotNull(blockPos.x, blockPos.y-1, blockPos.z);
            if (blockBelow.x == BlockTypes.getId(BlockTypes.GRASS)) {
                World.setBlock(blockPos.x, blockPos.y-1, blockPos.z, BlockTypes.getId(BlockTypes.DIRT), 0, true, false, 1, false);
            }
        }
        float modifiedSpeed = speed;
        float modifiedGrav = grav;
        if (blockIn.x == 1) { //water
            modifiedGrav *= 0.2f;
            friction *= 0.9f;
            modifiedSpeed *= 0.5f;
        } else if (Tags.leaves.tagged.contains(blockIn.x)) { //leaves
            if (blockIn.y == 0) {
                friction *= 0.5f;
                modifiedSpeed *= 0.5f;
            } else {
                friction *= 0.9f;
                modifiedSpeed *= 0.9f;
            }
        }
        friction = Math.min(0.99f, friction); //1-airFriction=maxFriction
        if (flying) {
            modifiedSpeed = speed;
            modifiedGrav = grav;
            friction = 0.75f;
        }
        Vector3f newMovement = new Vector3f(0f);
        boolean canMove = (flying || onGround || blockIn.x == 1);
        if (forward || backward) {
            Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(0, 0, (modifiedSpeed * (canMove ? 1 : 0.1f)) * (sprint || superSprint ? (backward ? (superSprint && sprint ? 100 : (superSprint ? 10 : 1.25f)) : (flying ? (superSprint ? 100 : 10) : sprintSpeed)) : 1) * (forward ? 1.25f : -1)).getTranslation(new Vector3f());
            newMovement.add(pos.x - translatedPos.x,0, pos.z - translatedPos.z);
        }
        if (rightward || leftward) {
            Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate((modifiedSpeed * (canMove ? 1 : 0.1f)) * (sprint || superSprint ? (flying ? (superSprint ? 100 : 10) : sprintSpeed) : 1) * (rightward ? -0.85f : 0.85f), 0, 0).getTranslation(new Vector3f());
            newMovement.add(pos.x - translatedPos.x, 0, pos.z - translatedPos.z);
        }
        if (upward || downward) {
            if (flying) {
                Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(0, speed * (downward ? (sprint || superSprint ? (superSprint ? 50 : 5) : 1f) : -1.2f * (sprint || superSprint ? (superSprint ? 50 : 5) : 1)), 0).getTranslation(new Vector3f());
                newMovement.add(0, pos.y - translatedPos.y, 0);
            } else if (blockIn.x == 1 && submerged) {
                Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(0, speed * (upward ? -2 : downward ? 1 : 0), 0).getTranslation(new Vector3f());
                newMovement.add(0, pos.y - translatedPos.y, 0);
            }
        }
        movement = new Vector3f(Utils.furthestFromZero(newMovement.x, movement.x*friction), Utils.furthestFromZero(newMovement.y, movement.y*friction), Utils.furthestFromZero(newMovement.z, movement.z*friction));
        vel = new Vector3f(vel.x*friction, vel.y*friction, vel.z*friction);

        if (!flying && vel.y >= -1+modifiedGrav && !onGround) {
            vel.set(vel.x, vel.y-modifiedGrav, vel.z);
        }

        if (Main.timeMS-jump < 100 && !flying) { //prevent jumping when space bar was pressed longer than 0.1s ago or when flying
            if ((onGround || (blockIn.x == 1 && solid(pos.x, pos.y, pos.z, width*1.125f, height, false, false))) && !submerged) {
                bobbing = height*-0.05f;
                bobbingDir = false;
                jump = 1000;
                lastJump = Main.timeMS;
                vel.y = Math.max(vel.y, jumpStrength);
            }
        }
        vel = new Vector3f(Math.clamp(vel.x, -1, 1), Math.clamp(vel.y, -1, 1), Math.clamp(vel.z, -1, 1));
        float mX = vel.x + movement.x;
        float mY = vel.y + movement.y;
        float mZ = vel.z + movement.z;
        Vector3f hitPos = pos;
        Vector3f destPos = new Vector3f(mX+pos.x, mY+pos.y, mZ+pos.z);
        Float maxX = null;
        Float maxY = null;
        Float maxZ = null;
        float detail = 1+Math.max(Math.abs(mX*256f), Math.max(Math.abs(mY*256f), Math.abs(mZ*256f)));
        for (int i = 0; i <= detail; i++) {
            Vector3f rayPos = new Vector3f(maxX != null ? maxX : pos.x+((destPos.x-pos.x)*(i/detail)), maxY != null ? maxY : pos.y+((destPos.y-pos.y)*(i/detail)), maxZ != null ? maxZ : pos.z+((destPos.z-pos.z)*(i/detail)));
            if (crouching && onGround) {
                if (!solid(rayPos.x, hitPos.y - 0.125f, hitPos.z, width, 0.125f, true, false)) {
                    maxX = hitPos.x;
                    rayPos.x = hitPos.x;
                }
                if (!solid(hitPos.x, hitPos.y - 0.125f, rayPos.z, width, 0.125f, true, false)) {
                    maxZ = hitPos.z;
                    rayPos.z = hitPos.z;
                }
            }
            if (solid(rayPos.x, rayPos.y, rayPos.z, width, height, false, false)) {
                if (maxX == null) {
                    bounciness = 0.66f;
                    if (solid(rayPos.x, hitPos.y, hitPos.z, width, height, false, !flying)) {
                        bounciness = Math.max(bounciness, -0.66f);
                        maxX = hitPos.x;
                        vel.x *= bounciness;
                        movement.x *= bounciness;
                    }
                }
                if (maxY == null) {
                    bounciness = 0.66f;
                    if (solid(hitPos.x, rayPos.y, hitPos.z, width, height, false, !flying)) {
                        bounciness = (upward && mY <= 0.f) ? bounciness : Math.max(bounciness, -0.66f); //dont limit bounciness if jumping and moving downwards
                        maxY = hitPos.y;
                        vel.y *= bounciness;
                        movement.y *= bounciness;
                    }
                }
                if (maxZ == null) {
                    bounciness = 0.66f;
                    if (solid(hitPos.x, hitPos.y, rayPos.z, width, height, false, !flying)) {
                        bounciness = Math.max(bounciness, -0.66f);
                        maxZ = hitPos.z;
                        vel.z *= bounciness;
                        movement.z *= bounciness;
                    }
                }
                if (maxX != null && maxY != null && maxZ != null) {
                    break;
                }
            } else {
                hitPos = rayPos;
            }
        }
        setPos(hitPos);
        dynamicSpeedOld = dynamicSpeed;
        dynamicSpeed = Math.min(1.f, Math.max(Math.abs(mX), Math.abs(mZ)));
    }

    public void setPos(Vector3f newPos) {
        oldPos = pos;
        pos = newPos;
        blockPos = new Vector3i((int) newPos.x, (int) newPos.y, (int) newPos.z);
        doSounds();
    }

    public long timeSinceAmbientSoundAttempt = 0;
    public float windGain = 0f;
    int ambientWind = 0;
    public void doSounds() {
        long currentTime = System.currentTimeMillis();
        if (currentTime-timeSinceAmbientSoundAttempt >= 1000) {
            timeSinceAmbientSoundAttempt = currentTime;
            if (windSource.soundPlaying == -1) {
                windSource.play(Sounds.WIND);
            }
            if (World.inBounds(blockPos.x, blockPos.y, blockPos.z)) {
                int sunLight = World.getLight(blockPos).w;
                if (waterFlowingSource.soundPlaying == -1) {
                    waterFlowingSource.play(Sounds.FLOW);
                }
                ambientWind = Math.min(333, ambientWind + sunLight);
                windGain = ambientWind/333f;
            }
        }
        float velocity = (Math.max(Math.abs(vel.x+movement.x), Math.max(Math.abs(vel.y+movement.y), Math.abs(vel.z+movement.z))));
        windSource.setGain(Math.clamp(windGain+(Math.max(0.05f, velocity/10)-0.05f), 0, 1));
        ambientWind = Math.max(0, ambientWind-1);
    }

    public void setCameraMatrix(float[] matrix) {
        camera.setViewMatrix(matrix);
        camera.move(0, camera.getViewMatrix().getTranslation(new Vector3f()).y(), 0, false);
    }
    public Matrix4f getCameraMatrixWithoutPitch() {
        Vector3f camOffset = new Vector3f();
        Matrix4f camMatrix = new Matrix4f(camera.getViewMatrixWithoutPitch());
        camMatrix.getTranslation(camOffset);
        return camMatrix.setTranslation(camOffset.x+pos.x, camOffset.y+pos.y, camOffset.z+pos.z);
    }
    public Matrix4f getCameraMatrix() {
        Vector3f camOffset = new Vector3f();
        Matrix4f camMatrix = new Matrix4f(camera.getViewMatrix());
        camMatrix.getTranslation(camOffset);
        camOffset = Utils.getInterpolatedVec(oldCamOffset, camOffset);
        Vector3f interpolatedPos = Utils.getInterpolatedVec(oldPos, pos);
        return camMatrix.setTranslation(camOffset.x+interpolatedPos.x, camOffset.y+eyeHeight+interpolatedPos.y+bobbing, camOffset.z+interpolatedPos.z)
                .rotateX(Utils.getInterpolatedFloat(prevTilt, tilt)).rotateZ(Utils.getInterpolatedFloat(prevStrafe, strafe)).invert();
    }

    public void rotate(float pitch, float yaw) {
        camera.rotate(pitch, yaw);
    }
}