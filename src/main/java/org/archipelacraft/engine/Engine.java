package org.archipelacraft.engine;

import io.github.libsdl4j.api.event.SDL_Event;
import org.archipelacraft.Main;

import java.util.ArrayList;
import java.util.List;

import static io.github.libsdl4j.api.video.SdlVideo.SDL_SetWindowTitle;

public class Engine {

    public static final int TARGET_UPS = 75;
    public static final int TARGET_FPS = 360;
    private final Main main;
    public final Window window;
    private boolean running;
    private int targetFps;
    private int targetUps;

    public Engine(String windowTitle, Window.WindowOptions opts, Main main) throws Exception {
        window = new Window(windowTitle, opts, () -> {
            resize();
            return null;
        });
        targetFps = opts.fps;
        targetUps = opts.ups;
        this.main = main;
        main.init(window);
        running = true;
    }

    private void cleanup() {
        window.cleanup();
    }

    private void resize() {
        //resize stuff
    }

    public List<Long> frameTimes = new ArrayList<>(List.of());

    private void run() throws Exception {
        window.getMouseInput().input(window);
        Main.mouseInput = window.getMouseInput();

        long initialNanoTime = System.nanoTime();
        long initialTime = System.currentTimeMillis();
        float timeU = 1000.0f / targetUps;
        float timeR = targetFps > 0 ? 1000.0f / targetFps : 0;
        float deltaUpdate = 0;
        float deltaFps = 0;
        int framesUntilUpdate = 0;
        while (frameTimes.size() < 60) {
            frameTimes.add(15000000L);
        }

        SDL_Event event = new SDL_Event();
        long updateTime = initialTime;
        while (running && !window.windowShouldClose()) {
            window.pollEvents(event);

            long now = System.currentTimeMillis();
            deltaUpdate += (now - initialTime) / timeU;
            deltaFps += (now - initialTime) / timeR;

            if (targetFps <= 0 || deltaFps >= 1) {
                main.input(window, now,  now - initialTime);
            }

            long diffTimeMillis = now - updateTime;
            main.update(window, diffTimeMillis, now);
            updateTime = now;
            if (deltaUpdate >= 1) {
                deltaUpdate--;
            }

            if (targetFps <= 0 || deltaFps >= 1) {
                deltaFps--;
                window.update();
                framesUntilUpdate--;
                if (framesUntilUpdate <= 0) {
                    double avgMS = 1000000000d/ArchipelacraftMath.averageLongs(frameTimes);
                    SDL_SetWindowTitle(Window.window, "Archipelacraft | " +
                            (int)Main.player.pos.x+"x,"+(int)Main.player.pos.y+"y,"+(int)Main.player.pos.z+"z | " +
                            (long)(avgMS) + "fps " +
                            String.format("%.1f", 1000d/(avgMS)) + "ms");
                    framesUntilUpdate = 40;
                }
                if (Main.renderingEnabled) {
                    long diffTimeNanos = (System.nanoTime() - initialNanoTime);
                    frameTimes.addLast(diffTimeNanos);
                    if (frameTimes.size() > 60) {
                        frameTimes.removeFirst();
                    }
                }
                initialNanoTime = System.nanoTime();
            }
            initialTime = now;
        }

        cleanup();
    }

    public void start() throws Exception {
        running = true;
        run();
    }

    public void stop() {
        running = false;
    }

}
