package org.archipelacraft.engine;

import com.sun.jna.ptr.IntByReference;
import io.github.libsdl4j.api.SdlSubSystemConst;
import io.github.libsdl4j.api.event.SDL_Event;
import io.github.libsdl4j.api.event.events.SDL_KeyboardEvent;
import io.github.libsdl4j.api.keyboard.SdlKeyboard;
import io.github.libsdl4j.api.mouse.SDL_Button;
import io.github.libsdl4j.api.mouse.SDL_ButtonMask;
import io.github.libsdl4j.api.video.SDL_GLContext;
import io.github.libsdl4j.api.video.SDL_Window;
import org.archipelacraft.Main;
import org.archipelacraft.game.rendering.Renderer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.tinylog.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.github.libsdl4j.api.Sdl.*;
import static io.github.libsdl4j.api.error.SdlError.*;
import static io.github.libsdl4j.api.event.SDL_EventType.*;
import static io.github.libsdl4j.api.event.SdlEvents.SDL_PollEvent;
import static io.github.libsdl4j.api.hints.SdlHints.SDL_SetHint;
import static io.github.libsdl4j.api.hints.SdlHintsConst.SDL_HINT_MOUSE_AUTO_CAPTURE;
import static io.github.libsdl4j.api.keyboard.SdlKeyboard.SDL_GetKeyboardState;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_SPACE;
import static io.github.libsdl4j.api.log.SDL_LogCategory.*;
import static io.github.libsdl4j.api.log.SdlLog.SDL_LogCritical;
import static io.github.libsdl4j.api.mouse.SdlMouse.SDL_GetMouseState;
import static io.github.libsdl4j.api.mouse.SdlMouse.SDL_SetRelativeMouseMode;
import static io.github.libsdl4j.api.video.SDL_GLattr.*;
import static io.github.libsdl4j.api.video.SDL_GLprofile.*;
import static io.github.libsdl4j.api.video.SDL_WindowEventID.SDL_WINDOWEVENT_RESIZED;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.*;
import static io.github.libsdl4j.api.video.SdlVideo.*;
import static io.github.libsdl4j.api.video.SdlVideoConst.*;
import static org.lwjgl.opengl.GL46.*;

public class Window {
    public static SDL_Window window;
    public static SDL_GLContext context;
    private int height;
    private Callable<Void> resizeFunc;
    private int width;
    private final Matrix4f projectionMatrix;
    public byte[] keys;

    public Window(String title, WindowOptions opts, Callable<Void> resizeFunc) {
        projectionMatrix = new Matrix4f();
        this.resizeFunc = resizeFunc;
        if (SDL_Init(SdlSubSystemConst.SDL_INIT_VIDEO) != 0) {
            throw new IllegalStateException("Unable to initialize SDL");
        }

        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 3);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        int monitorBitDepth = env.getDefaultScreenDevice().getDisplayMode().getBitDepth();
        if (monitorBitDepth > 32) {
            SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 10);
            SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 10);
            SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 10);
            SDL_GL_SetAttribute(SDL_GL_ALPHA_SIZE, 2);
        }
        SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, 24);
        SDL_GL_SetAttribute(SDL_GL_FRAMEBUFFER_SRGB_CAPABLE, 0);
        SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);

        if (opts.width > 0 && opts.height > 0) {
            this.width = opts.width;
            this.height = opts.height;
        } else {
            width = Constants.width;
            height = Constants.height;
        }

        window = SDL_CreateWindow("Archipelacraft", SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED, width, height, SDL_WINDOW_SHOWN | SDL_WINDOW_OPENGL);
        if (window == null) {
            SDL_LogCritical(SDL_LOG_CATEGORY_APPLICATION, "Failed to create OpenGL window: %s\n", SDL_GetError());
            SDL_Quit();
        }

        context = SDL_GL_CreateContext(window);
        if (context == null) {
            SDL_LogCritical(SDL_LOG_CATEGORY_APPLICATION, "Failed to create OpenGL context: %s\n", SDL_GetError());
            SDL_DestroyWindow(window);
            SDL_Quit();
        }

        SDL_GL_SetSwapInterval(0); //disable vsync
        SDL_GL_MakeCurrent(Window.window, Window.context);

        SDL_SetWindowResizable(window, true);
        SDL_SetRelativeMouseMode(true);
        input();
    }

    public void cleanup() {
        SDL_DestroyWindow(Window.window);
        SDL_Quit();
    }

    public int getHeight() {
        return height;
    }
    public int getWidth() {
        return width;
    }

    public boolean isKeyPressed(int keyCode) {
        return keys[keyCode] > 0;
    }

    public boolean leftButtonPressed = false;
    public boolean middleButtonPressed = false;
    public boolean rightButtonPressed = false;
    public Vector2f scroll = new Vector2f(0);
    public Vector2f displVec = new Vector2f(0);
    public Vector2f currentPos = new Vector2f(0);

    public void input() {
        leftButtonPressed = (SDL_GetMouseState(null, null)&SDL_ButtonMask.SDL_BUTTON_LMASK) > 0;
        rightButtonPressed = (SDL_GetMouseState(null, null)&SDL_ButtonMask.SDL_BUTTON_RMASK) > 0;
        middleButtonPressed = (SDL_GetMouseState(null, null)&SDL_ButtonMask.SDL_BUTTON_MMASK) > 0;
        IntByReference length = new IntByReference();
        keys = SDL_GetKeyboardState(length).getByteArray(0, length.getValue());
    }

    public void pollEvents(SDL_Event event) {
        displVec.x = 0;
        displVec.y = 0;
        scroll.set(0);
        while (SDL_PollEvent(event) != 0) {
            switch (event.type) {
                case SDL_QUIT:
                    Main.isClosing = true;
                    break;
                case SDL_WINDOWEVENT:
                    if (event.window.event == SDL_WINDOWEVENT_RESIZED) {
                        resized(event.window.data1, event.window.data2);
                    }
                    break;
                case SDL_MOUSEMOTION:
                    displVec.y += event.motion.xrel;
                    displVec.x += event.motion.yrel;
                    currentPos.x = event.motion.x;
                    currentPos.y = event.motion.y;
                    break;
                case SDL_MOUSEWHEEL:
                    scroll.x = event.wheel.x;
                    scroll.y = event.wheel.y;
                    break;
                default:
                    break;
            }
        }
    }

    public void resized(int width, int height) {
        this.width = width;
        this.height = height;
        try {
            glViewport(0, 0, width, height);
            resizeFunc.call();
            Renderer.initiallyFillTextures(this, true);
        } catch (Exception excp) {
            Logger.error("Error calling resize callback", excp);
        }
    }

    public void update() {
        SDL_GL_SwapWindow(window);
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
    public Matrix4f updateProjectionMatrix() {
        float aspectRatio = (float) width /height;
        projectionMatrix.identity();
        projectionMatrix.set(
                1.f/Constants.FOV, 0.f, 0.f, 0.f,
                0.f, aspectRatio/Constants.FOV, 0.f, 0.f,
                0.f, 0.f, 0.f, -1.f,
                0.f, 0.f, Constants.Z_NEAR, 0.f
        );
        return projectionMatrix;
    }

    public boolean shouldClose = false;
    public boolean windowShouldClose() {
        return shouldClose;
    }

    public static class WindowOptions {
        public boolean compatibleProfile;
        public int fps = Engine.TARGET_FPS;
        public int height;
        public int ups = Engine.TARGET_UPS;
        public int width;
    }
}