package org.archipelacraft.game.gameplay;

import org.archipelacraft.engine.Camera;
import org.archipelacraft.game.audio.Source;
import org.joml.*;

import java.lang.Math;

public class Player {
    private final Camera camera = new Camera();
    public Vector3f pos = new Vector3f();
    public Vector3f selectedBlock = new Vector3f();
    public Vector3f prevSelectedBlock = new Vector3f();
    public int[] stack = new int[32];

    public static float scale = 1f;
    public float speed = Math.max(0.15f, 0.15f*scale);
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
    public boolean creative = false;

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
        camera.setPosition(newPos.x, newPos.y, newPos.z);
    }

    public void tick() {
        camera.oldPosition.set(camera.position);
        float modifiedSpeed = speed;
        if (sprint) {
            modifiedSpeed *= 10;
        }
        if (superSprint) {
            modifiedSpeed *= 100;
        }
        if (forward) {
            camera.moveForward(modifiedSpeed);
        } else if (backward) {
            camera.moveBackwards(modifiedSpeed);
        }
        if (rightward) {
            camera.moveRight(modifiedSpeed);
        } else if (leftward) {
            camera.moveLeft(modifiedSpeed);
        }
        if (upward) {
            camera.moveUp(modifiedSpeed);
        } else if (downward) {
            camera.moveDown(modifiedSpeed);
        }
        pos = camera.getPosition();
    }

    public Matrix4f getCameraMatrix() {
        return camera.getViewMatrix();
    }

    public void rotate(float pitch, float yaw) {
        camera.addRotation(pitch, yaw);
    }
}