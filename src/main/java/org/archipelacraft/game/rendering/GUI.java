package org.archipelacraft.game.rendering;

import org.archipelacraft.Main;
import org.archipelacraft.engine.ShaderProgram;
import org.archipelacraft.engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class GUI {
    public static void drawQuad(float aspectRatio) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            //glUniformMatrix4fv(Renderer.raster.uniforms.get("model"), false, Main.player.getCameraMatrix().invert().translate(0, 0, -0.2f).scale(0.2f, 0.1f, 0.2f).get(stack.mallocFloat(16)));
            glUniformMatrix4fv(Renderer.raster.uniforms.get("model"), false, new Matrix4f().translate(0.f, -0.94f, 0.f).scale(0.3f, 0.03f*aspectRatio, 1).get(stack.mallocFloat(16)));
        }
        glBindVertexArray(Models.CUBE.vaoId);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glDrawArrays(GL_TRIANGLES, 0, Models.CUBE.positions.length);
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
    }

    public static void draw(Window window, ShaderProgram program) {
        glUniform1i(program.uniforms.get("screenSpace"), 1);
        drawQuad((float) window.getWidth() / window.getHeight());
        glUniform1i(program.uniforms.get("screenSpace"), 0);
    }
}
