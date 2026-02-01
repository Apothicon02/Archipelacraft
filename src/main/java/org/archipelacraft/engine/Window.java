package org.archipelacraft.engine;

import com.sun.jna.ptr.IntByReference;
import io.github.libsdl4j.api.SdlSubSystemConst;
import io.github.libsdl4j.api.event.SDL_Event;
import io.github.libsdl4j.api.event.events.SDL_KeyboardEvent;
import io.github.libsdl4j.api.keyboard.SdlKeyboard;
import io.github.libsdl4j.api.video.SDL_GLContext;
import io.github.libsdl4j.api.video.SDL_Window;
import org.archipelacraft.Main;
import org.archipelacraft.game.rendering.Renderer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.github.libsdl4j.api.Sdl.*;
import static io.github.libsdl4j.api.error.SdlError.*;
import static io.github.libsdl4j.api.event.SDL_EventType.*;
import static io.github.libsdl4j.api.event.SdlEvents.SDL_PollEvent;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_SPACE;
import static io.github.libsdl4j.api.log.SDL_LogCategory.*;
import static io.github.libsdl4j.api.log.SdlLog.SDL_LogCritical;
import static io.github.libsdl4j.api.video.SDL_GLattr.*;
import static io.github.libsdl4j.api.video.SDL_GLprofile.*;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.*;
import static io.github.libsdl4j.api.video.SdlVideo.*;
import static io.github.libsdl4j.api.video.SdlVideoConst.*;
import static org.lwjgl.opengl.GL46.*;

public class Window {
    public static SDL_Window window;
    public static SDL_GLContext context;
    private int height;
    private MouseInput mouseInput;
    private Callable<Void> resizeFunc;
    private int width;
    private final Matrix4f projectionMatrix;
    public List<Integer> keys = new ArrayList<>();

    public Window(String title, WindowOptions opts, Callable<Void> resizeFunc) {
        projectionMatrix = new Matrix4f();
        this.resizeFunc = resizeFunc;
        if (SDL_Init(SdlSubSystemConst.SDL_INIT_VIDEO) != 0) {
            throw new IllegalStateException("Unable to initialize SDL");
        }

        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 3);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 10);
        SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 10);
        SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 10);
        SDL_GL_SetAttribute(SDL_GL_ALPHA_SIZE, 2);
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

        mouseInput = new MouseInput(this);
    }

    public void cleanup() {

    }

    public int getHeight() {
        return height;
    }

    public MouseInput getMouseInput() {
        return mouseInput;
    }

    public int getWidth() {
        return width;
    }

    public long getWindowHandle() {
        return 0;
    }

    public boolean isKeyPressed(int keyCode) {
        return keys.contains(keyCode);
    }

    public void keyCallBack(int key, int action) {

    }

    public Vector2f displVec = new Vector2f(0);

    public void pollEvents(SDL_Event event) {
        displVec.x = 0;
        displVec.y = 0;
        while (SDL_PollEvent(event) != 0) {
            switch (event.type) {
                case SDL_QUIT:
                    Main.isClosing = true;
                    break;
                case SDL_MOUSEMOTION:
                    boolean rotateX = event.motion.x != 0;
                    boolean rotateY = event.motion.y != 0;
                    if (rotateX) {
                        displVec.y = event.motion.x;
                    }
                    if (rotateY) {
                        displVec.x = event.motion.y;
                    }
                case SDL_KEYDOWN:
                    if (!keys.contains(event.key.keysym.sym)) {
                        keys.add(event.key.keysym.sym);
                    }
                    break;
                case SDL_KEYUP:
                    if (keys.contains(event.key.keysym.sym)) {
                        for (int i = 0; i < keys.size(); i++) {
                            if (keys.get(i) == event.key.keysym.sym) {
                                keys.remove(i);
                                i--;
                            }
                        };
                    }
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

    public boolean windowShouldClose() {
        return false;
    }

    public static class WindowOptions {
        public boolean compatibleProfile;
        public int fps = Engine.TARGET_FPS;
        public int height;
        public int ups = Engine.TARGET_UPS;
        public int width;
    }
}