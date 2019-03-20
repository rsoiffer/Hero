package graphics;

import engine.Behavior;
import engine.Layer;
import static engine.Layer.RENDER3D;
import graphics.opengl.Framebuffer;
import static graphics.opengl.Framebuffer.FRAMEBUFFER_VAO;
import static graphics.opengl.GLObject.bindAll;
import graphics.opengl.GLState;
import graphics.opengl.ShaderProgram;
import java.util.List;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import util.Resources;
import util.math.Vec4d;

public class LightingPass extends Behavior {

    public Vec4d clearColor;
    public GeometryPass gp;
    public List<ShadowPass> spList;

    private ShaderProgram lightingPass;

    @Override
    public void createInner() {
        GLState.bindShaderProgram(null);
        for (int i = 0; i < spList.size(); i++) {
            spList.get(i).bindShadowMap(4 + i);
        }

        lightingPass = Resources.loadShaderProgram("lighting_pass");
        lightingPass.setUniform("gPosition", 0);
        lightingPass.setUniform("gNormal", 1);
        lightingPass.setUniform("gAlbedo", 2);
        lightingPass.setUniform("gMRAH", 3);
        for (int i = 0; i < spList.size(); i++) {
            lightingPass.setUniform("shadowMap[" + i + "]", 4 + i);
        }

        GLState.bindShaderProgram(null);
        for (int i = 0; i < spList.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + 4 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }

    @Override
    public Layer layer() {
        return RENDER3D;
    }

    @Override
    public void step() {
        Camera.current = Camera.camera2d;

        Framebuffer.clearWindow(clearColor);

        GLState.bindShaderProgram(null);
        for (int i = 0; i < spList.size(); i++) {
            spList.get(i).bindShadowMap(4 + i);
        }
        for (int i = 0; i < spList.size(); i++) {
            lightingPass.setUniform("lightSpaceMatrix[" + i + "]", spList.get(i).getLightSpaceMatrix());
            lightingPass.setUniform("cascadeEndClipSpace[" + i + "]", (float) spList.get(i).zMax);
        }
        lightingPass.setUniform("projectionViewMatrix", Camera.camera3d.projectionMatrix().mul(Camera.camera3d.viewMatrix()));
        lightingPass.setUniform("camPos", Camera.camera3d.position);
        gp.bindGBuffer();
        bindAll(FRAMEBUFFER_VAO);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        GLState.bindShaderProgram(null);
        for (int i = 0; i < spList.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + 4 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        Camera.current = Camera.camera3d;
    }
}
