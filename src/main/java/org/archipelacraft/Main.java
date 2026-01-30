package org.archipelacraft;

import com.google.gson.Gson;
import org.archipelacraft.game.ScheduledTicker;
import org.archipelacraft.game.audio.BlockSFX;
import org.archipelacraft.game.audio.Source;
import org.archipelacraft.game.blocks.types.BlockTypes;
import org.archipelacraft.game.gameplay.HandManager;
import org.archipelacraft.game.gameplay.Player;
import org.archipelacraft.game.audio.AudioController;
import org.archipelacraft.game.rendering.GUI;
import org.archipelacraft.game.rendering.Models;
import org.archipelacraft.game.world.LightHelper;
import org.archipelacraft.game.world.World;
import org.archipelacraft.game.noise.Noises;
import org.archipelacraft.game.rendering.Renderer;
import org.joml.*;
import org.archipelacraft.engine.*;
import org.lwjgl.opengl.GL;

import java.lang.Math;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL45.*;

public class Main {
    public static String mainFolder = System.getenv("APPDATA")+"/Archipelacraft/";
    public static String resourcesPath = mainFolder+"resources/";
    public static Gson gson = new Gson();
    public static Player player;
    private static final float MOUSE_SENSITIVITY = 0.01f;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        Engine gameEng = new Engine("Archipelacraft", new Window.WindowOptions(), main);
        gameEng.start();
    }

    public void init(Window window) throws Exception {
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_GEQUAL);
        glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);
        glFrontFace(GL_CW);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);

        AudioController.init();
        AudioController.setListenerData(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new float[6]);

        Noises.init();
        World.generate();
        Models.loadModels();

        player = new Player(new Vector3f(522, 97, 500));
        player.setCameraMatrix(new Matrix4f().get(new float[16]));
        player.inv.init();
    }

    public static MouseInput mouseInput = null;
    public static boolean isLMBClick = false;
    public static boolean isRMBClick = false;
    public static boolean wasLMBDown = false;
    public static boolean wasRMBDown = false;
    boolean wasTabDown = false;
    boolean wasXDown = false;
    boolean wasTDown = false;
    boolean wasGDown = false;
    boolean wasLDown = false;
    boolean wasUpDown = false;
    boolean wasDownDown = false;
    boolean wasEDown = false;
    boolean wasCDown = false;
    boolean wasSDown = false;
    boolean wasRDown = false;
    boolean wasWDown = false;
    boolean wasQDown = false;
    boolean wasF1Down = false;
    boolean wasF4Down = false;
    boolean wasF5Down = false;
    public static boolean isClosing = false;

    public static long lastBlockBroken = 0L;
    public static int reach = 50;

    public void input(Window window, long timeMillis, long diffTimeMillis) {
        if (!isClosing) {
            if (window.isKeyPressed(GLFW_KEY_ESCAPE, GLFW_PRESS)) {
                isClosing = true;
            } else {
                window.getMouseInput().input(window);
                mouseInput = window.getMouseInput();
                boolean isLMBDown = mouseInput.isLeftButtonPressed();
                boolean isRMBDown = mouseInput.isRightButtonPressed();
                isLMBClick = wasLMBDown && !isLMBDown;
                isRMBClick = wasRMBDown & !isRMBDown;
                boolean isShiftDown = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT, GLFW_PRESS);
                boolean isCtrlDown = window.isKeyPressed(GLFW_KEY_LEFT_CONTROL, GLFW_PRESS);

                if (wasTabDown && !window.isKeyPressed(GLFW_KEY_TAB, GLFW_PRESS)) {
                    player.inv.open = !player.inv.open;
                }

                if (window.isKeyPressed(GLFW_KEY_F11, GLFW_PRESS)) {
                    glfwSetWindowPos(window.getWindowHandle(), 0, 0);
                    glfwSetWindowSize(window.getWindowHandle(), 2560, 1440);
                    //glfwSetWindowMonitor(window.getWindowHandle(), glfwGetWindowMonitor(window.getWindowHandle()), 0, 0, 2560, 1440, GLFW_DONT_CARE);
                }

                if (player.inv.open) {
                    player.clearVars();
                    player.inv.tick(mouseInput);
                    if (wasQDown && !window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS)) {
                        //drop item mouse cursor is holding or hovering over.
                    }
                } else {
                    Vector2f displVec = mouseInput.getDisplVec();
                    player.rotate((float) Math.toRadians(displVec.x * MOUSE_SENSITIVITY),
                            (float) Math.toRadians(displVec.y * MOUSE_SENSITIVITY));
                    HandManager.useHands(timeMillis, mouseInput);

                    player.sprint = isShiftDown;
                    player.superSprint = window.isKeyPressed(GLFW_KEY_CAPS_LOCK, GLFW_PRESS);
                    player.forward = window.isKeyPressed(GLFW_KEY_W, GLFW_PRESS);
                    player.backward = window.isKeyPressed(GLFW_KEY_S, GLFW_PRESS);
                    player.rightward = window.isKeyPressed(GLFW_KEY_D, GLFW_PRESS);
                    player.leftward = window.isKeyPressed(GLFW_KEY_A, GLFW_PRESS);
                    player.upward = window.isKeyPressed(GLFW_KEY_SPACE, GLFW_PRESS);
                    player.downward = isCtrlDown;
                    player.crouching = isCtrlDown;
                    if (window.isKeyPressed(GLFW_KEY_SPACE, GLFW_PRESS) && timeMillis - player.lastJump > 200) { //only jump at most five times a second
                        player.jump = timeMillis;
                    }
                    if (wasXDown && !window.isKeyPressed(GLFW_KEY_X, GLFW_PRESS)) {
                        player.flying = !player.flying;
                    }

                    if (wasQDown && !window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS)) {
                        //drop item in hand.
                    }

                    if (wasF1Down && !window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS)) {
                        Renderer.showUI = !Renderer.showUI;
                    }

                    if (wasTDown && !window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS)) {
                        updateTime(100000L, 1);
                    }
                    if (wasUpDown && !window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS)) {
                        timeMul = Math.min(100, timeMul + (isShiftDown ? 10.f : 0.25f));
                    }
                    if (wasDownDown && !window.isKeyPressed(GLFW_KEY_DOWN, GLFW_PRESS)) {
                        timeMul = Math.max(0, timeMul - (isShiftDown ? 10.f : 0.25f));
                    }

                    if (window.isKeyPressed(GLFW_KEY_F3, GLFW_PRESS)) {
                        if (wasCDown && !window.isKeyPressed(GLFW_KEY_C, GLFW_PRESS)) {
                            player.creative = !player.creative;
                        }
                    }
                }

                mouseInput.scroll.set(0.d);
                wasLMBDown = isLMBDown;
                wasRMBDown = isRMBDown;
                wasF1Down = window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS);
                wasF4Down = window.isKeyPressed(GLFW_KEY_F4, GLFW_PRESS);
                wasF5Down = window.isKeyPressed(GLFW_KEY_F5, GLFW_PRESS);
                wasQDown = window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS);
                wasEDown = window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS);
                wasCDown = window.isKeyPressed(GLFW_KEY_C, GLFW_PRESS);
                wasSDown = window.isKeyPressed(GLFW_KEY_S, GLFW_PRESS);
                wasRDown = window.isKeyPressed(GLFW_KEY_R, GLFW_PRESS);
                wasWDown = window.isKeyPressed(GLFW_KEY_W, GLFW_PRESS);
                wasTDown = window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS);
                wasXDown = window.isKeyPressed(GLFW_KEY_X, GLFW_PRESS);
                wasTabDown = window.isKeyPressed(GLFW_KEY_TAB, GLFW_PRESS);
                wasGDown = window.isKeyPressed(GLFW_KEY_G, GLFW_PRESS);
                wasLDown = window.isKeyPressed(GLFW_KEY_L, GLFW_PRESS);
                wasUpDown = window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS);
                wasDownDown = window.isKeyPressed(GLFW_KEY_DOWN, GLFW_PRESS);
            }
        }
    }

    public static int meridiem = 1;

    public void updateTime(long diffTimeMillis, float mul) {
        float inc = (diffTimeMillis/600000f)*mul;
        Renderer.time += inc;
        float time = Renderer.timeOfDay+(inc*meridiem);
        if (time < 0f) {
            time = 0;
            meridiem = 1;
        } else if (time > 1f) {
            time = 1;
            meridiem = -1;
        }
        Renderer.timeOfDay = time;
    }

    public static boolean renderingEnabled = false;
    public static double interpolationTime = 0;
    public static double timePassed = 0;
    public static double timeMul = 1;
    public static double tickTime = 50;
    public static long timeMS;
    public static long currentTick = 0;

    public void update(Window window, long diffTimeMillis, long time) throws Exception {
        tickTime=50/timeMul;
        timeMS = time;
        if (isClosing) {
            glfwSetWindowShouldClose(window.getWindowHandle(), true);
        } else {
            if (!renderingEnabled) {
                renderingEnabled = true;
                Renderer.init(window);
            }
            if (renderingEnabled) {
                updateTime(diffTimeMillis, (float) timeMul);
                int ticksDone = 0;
                float factor = (float) (0.0002f*timePassed);
                while (timePassed >= tickTime) {
                    ticksDone++;
                    currentTick++;
                    timePassed -= tickTime;
                    player.tick();
                    ScheduledTicker.tick();
                    AudioController.disposeSources();
                    if (ticksDone >= 3) {
                        timePassed = tickTime-1;
                    }
                }
                interpolationTime = timePassed/tickTime;
                float speed = Utils.getInterpolatedFloat(player.dynamicSpeedOld, player.dynamicSpeed);
                float dFOV = (float) Math.toRadians(65+(30*Math.min(0.3f, speed*1.5f)));
                Constants.FOV = Constants.FOV > dFOV ? Math.max(dFOV, Constants.FOV-(factor*1.5f)) : (Constants.FOV < dFOV ? Math.min(dFOV, Constants.FOV+(factor*1.5f)) : Constants.FOV);
                if (player.onGround) {
                    float bobbingInc = Math.min(0.009f, 0.75f*speed*(player.height*((float) (factor*(1.5f+Math.random())))));
                    if (player.bobbingDir) {
                        player.bobbing += bobbingInc;
                        if (player.bobbing >= 0) {
                            player.bobbing = 0;
                            player.bobbingDir = false;
                        }
                    } else {
                        player.bobbing -= bobbingInc;
                        if (player.bobbing <= player.height*-0.05f) {
                            player.bobbing = player.height*-0.05f;
                            player.bobbingDir = true;
                            if (player.onGround) {
                                BlockSFX stepSFX = BlockTypes.blockTypeMap.get(player.blockOn.x).blockProperties.blockSFX;
                                Source stepSource = new Source(player.oldPos, (float) (stepSFX.stepGain+((stepSFX.stepGain*Math.random())/3)), (float) (stepSFX.stepPitch+((stepSFX.stepPitch*Math.random())/3)), 0, 0);
                                AudioController.disposableSources.add(stepSource);
                                stepSource.setVel(new Vector3f(player.vel).add(player.movement));
                                stepSource.play((stepSFX.stepIds[(int) (Math.random() * stepSFX.stepIds.length)]), true);
                            }
                        }
                    }
                }
                Renderer.render(window);
                LightHelper.iterateLightQueue();
                timePassed += diffTimeMillis;
            }
        }
    }
}