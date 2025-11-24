package org.archipelacraft.game;

import org.archipelacraft.engine.Camera;
import org.joml.*;

import java.lang.Math;

public class Player {
    private final Camera camera = new Camera();
    public Vector3f pos = new Vector3f();
    public Vector3f selectedBlock = new Vector3f();
    public Vector3f prevSelectedBlock = new Vector3f();

    public static float scale = 1f;
    public float speed = Math.max(0.15f, 0.15f*scale);
    public boolean sprint = false;
    public boolean superSprint = false;
    public boolean forward = false;
    public boolean backward = false;
    public boolean rightward = false;
    public boolean leftward = false;
    public boolean upward = false;
    public boolean downward = false;

    public Player() {}

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