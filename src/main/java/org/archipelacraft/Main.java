package org.archipelacraft;

import com.google.gson.Gson;
import io.github.libsdl4j.api.keyboard.SdlKeyboard;
import io.github.libsdl4j.api.video.SDL_Window;
import io.github.libsdl4j.api.video.SDL_WindowFlags;
import org.archipelacraft.engine.Window;
import org.archipelacraft.game.ScheduledTicker;
import org.archipelacraft.game.audio.BlockSFX;
import org.archipelacraft.game.audio.Source;
import org.archipelacraft.game.blocks.types.BlockTypes;
import org.archipelacraft.game.gameplay.HandManager;
import org.archipelacraft.game.gameplay.Player;
import org.archipelacraft.game.audio.AudioController;
import org.archipelacraft.game.rendering.Models;
import org.archipelacraft.game.world.LightHelper;
import org.archipelacraft.game.world.World;
import org.archipelacraft.game.noise.Noises;
import org.archipelacraft.game.rendering.Renderer;
import org.joml.*;
import org.archipelacraft.engine.*;
import org.lwjgl.opengl.GL;

import java.lang.Math;

import static io.github.libsdl4j.api.Sdl.SDL_Quit;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.*;
import static io.github.libsdl4j.api.video.SdlVideo.*;
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

        //load player
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
    boolean wasF11Down = false;
    public static boolean isClosing = false;
    public static boolean isFullScreen = false;
    public static long lastBlockBroken = 0L;
    public static int reach = 50;

    public void input(Window window, long timeMillis, long diffTimeMillis) {
        if (!isClosing) {
            if (window.isKeyPressed(SDLK_ESCAPE)) {
                isClosing = true;
            } else {
                window.getMouseInput().input(window);
                mouseInput = window.getMouseInput();
                boolean isLMBDown = mouseInput.isLeftButtonPressed();
                boolean isRMBDown = mouseInput.isRightButtonPressed();
                isLMBClick = wasLMBDown && !isLMBDown;
                isRMBClick = wasRMBDown & !isRMBDown;
                boolean isShiftDown = window.isKeyPressed(SDLK_LSHIFT);
                boolean isCtrlDown = window.isKeyPressed(SDLK_LCTRL);
                boolean isF11Down = window.isKeyPressed(SDLK_F11);

                if (wasTabDown && !window.isKeyPressed(SDLK_TAB)) {
                    player.inv.open = !player.inv.open;
                }

                if (!isF11Down && wasF11Down) {
                    if (!isFullScreen) {
                        isFullScreen = true;
                        SDL_SetWindowPosition(Window.window, 0, 0);
                        SDL_SetWindowSize(Window.window, 2560, 1440);
                        window.resized(2560, 1440);
                        //glfwSetWindowAttrib(window.getWindowHandle(), GLFW_DECORATED, GLFW_FALSE);
//                        glfwSetWindowPos(window.getWindowHandle(), 0, 0);
//                        GLFWVidMode mode = glfwGetVideoMode(glfwGetPrimaryMonitor());
//                        glfwSetWindowSize(window.getWindowHandle(), mode.width(), mode.height());
                        //glfwSetWindowMonitor(window.getWindowHandle(), glfwGetWindowMonitor(window.getWindowHandle()), 0, 0, 2560, 1440, GLFW_DONT_CARE);
                    } else {
                        isFullScreen = false;
                        SDL_SetWindowPosition(Window.window, 0, 32);
                        SDL_SetWindowSize(Window.window, (int) (2560*0.8f), (int) (1440*0.8f));
                        window.resized((int) (2560*0.8f), (int) (1440*0.8f));
                        //glfwSetWindowAttrib(window.getWindowHandle(), GLFW_DECORATED, GLFW_TRUE);
//                        glfwSetWindowPos(window.getWindowHandle(), 0, 32);
//                        glfwSetWindowSize(window.getWindowHandle(), Constants.width, Constants.height);
                        //glfwSetWindowMonitor(window.getWindowHandle(), glfwGetWindowMonitor(window.getWindowHandle()), 0, 0, 2560, 1440, GLFW_DONT_CARE);
                    }
                }

                if (player.inv.open) {
                    player.clearVars();
                    player.inv.tick(mouseInput);
                    if (wasQDown && !window.isKeyPressed(SDLK_Q)) {
                        //drop item mouse cursor is holding or hovering over.
                    }
                } else {
                    Vector2f displVec = new Vector2f(window.displVec).mul(0.001f);
                    player.rotate((float) Math.toRadians(displVec.x * MOUSE_SENSITIVITY),
                            (float) Math.toRadians(displVec.y * MOUSE_SENSITIVITY));
                    HandManager.useHands(timeMillis, mouseInput);

                    player.sprint = isShiftDown;
                    player.superSprint = window.isKeyPressed(SDLK_CAPSLOCK);
                    player.forward = window.isKeyPressed(SDLK_W);
                    player.backward = window.isKeyPressed(SDLK_S);
                    player.rightward = window.isKeyPressed(SDLK_D);
                    player.leftward = window.isKeyPressed(SDLK_A);
                    player.upward = window.isKeyPressed(SDLK_SPACE);
                    player.downward = isCtrlDown;
                    player.crouching = isCtrlDown;
                    if (window.isKeyPressed(SDLK_SPACE) && timeMillis - player.lastJump > 200) { //only jump at most five times a second
                        player.jump = timeMillis;
                    }
                    if (wasXDown && !window.isKeyPressed(SDLK_X)) {
                        player.flying = !player.flying;
                    }

                    if (wasQDown && !window.isKeyPressed(SDLK_Q)) {
                        //drop item in hand.
                    }

                    if (wasF1Down && !window.isKeyPressed(SDLK_F1)) {
                        Renderer.showUI = !Renderer.showUI;
                    }

                    if (wasTDown && !window.isKeyPressed(SDLK_T)) {
                        updateTime(100000L, 1);
                    }
                    if (wasUpDown && !window.isKeyPressed(SDLK_UP)) {
                        timeMul = Math.min(100, timeMul + (isShiftDown ? 10.f : 0.25f));
                    }
                    if (wasDownDown && !window.isKeyPressed(SDLK_DOWN)) {
                        timeMul = Math.max(0, timeMul - (isShiftDown ? 10.f : 0.25f));
                    }

                    if (window.isKeyPressed(SDLK_F3)) {
                        if (wasCDown && !window.isKeyPressed(SDLK_C)) {
                            player.creative = !player.creative;
                        }
                    }
                }

                mouseInput.scroll.set(0.d);
                wasLMBDown = isLMBDown;
                wasRMBDown = isRMBDown;
                wasF1Down = window.isKeyPressed(SDLK_F1);
                wasF4Down = window.isKeyPressed(SDLK_F4);
                wasF5Down = window.isKeyPressed(SDLK_F5);
                wasF11Down = isF11Down;
                wasQDown = window.isKeyPressed(SDLK_Q);
                wasEDown = window.isKeyPressed(SDLK_E);
                wasCDown = window.isKeyPressed(SDLK_C);
                wasSDown = window.isKeyPressed(SDLK_S);
                wasRDown = window.isKeyPressed(SDLK_R);
                wasWDown = window.isKeyPressed(SDLK_W);
                wasTDown = window.isKeyPressed(SDLK_T);
                wasXDown = window.isKeyPressed(SDLK_X);
                wasTabDown = window.isKeyPressed(SDLK_TAB);
                wasGDown = window.isKeyPressed(SDLK_G);
                wasLDown = window.isKeyPressed(SDLK_L);
                wasUpDown = window.isKeyPressed(SDLK_UP);
                wasDownDown = window.isKeyPressed(SDLK_DOWN);
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
            World.saveWorld(World.worldPath+"/");
            SDL_DestroyWindow(Window.window);
            SDL_Quit();
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