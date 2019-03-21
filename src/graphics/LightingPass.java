package graphics;

import engine.Behavior;
import engine.Layer;
import static engine.Layer.RENDER3D;
import graphics.opengl.Framebuffer;
import static graphics.opengl.Framebuffer.FRAMEBUFFER_VAO;
import static graphics.opengl.GLObject.bindAll;
import graphics.opengl.GLState;
import graphics.opengl.Shader;
import graphics.opengl.Texture;
import java.util.List;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import util.math.Vec3d;
import util.math.Vec4d;

public class LightingPass extends Behavior {

    public Vec4d clearColor;
    public GeometryPass gp;
    public List<ShadowPass> spList;
    public Vec3d sunColor, sunDirection;

    private Shader shader;
    private Texture brdfLUT;

    @Override
    public void createInner() {
        GLState.bindShader(null);
        for (int i = 0; i < spList.size(); i++) {
            spList.get(i).bindShadowMap(4 + i);
        }

        shader = Shader.load("lighting_pass");
        shader.setUniform("gPosition", 0);
        shader.setUniform("gNormal", 1);
        shader.setUniform("gAlbedo", 2);
        shader.setUniform("gMRA", 3);
        for (int i = 0; i < spList.size(); i++) {
            shader.setUniform("shadowMap[" + i + "]", 4 + i);
        }

        brdfLUT = Texture.load("brdf_lut.png");
        brdfLUT.num = 9;
        shader.setUniform("brdfLUT", 9);

        GLState.bindShader(null);
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

        GLState.bindShader(null);
        for (int i = 0; i < spList.size(); i++) {
            spList.get(i).bindShadowMap(4 + i);
        }
        shader.setUniform("sunColor", sunColor);
        shader.setUniform("sunDirection", sunDirection);
        for (int i = 0; i < spList.size(); i++) {
            shader.setUniform("lightSpaceMatrix[" + i + "]", spList.get(i).getLightSpaceMatrix());
            shader.setUniform("cascadeEndClipSpace[" + i + "]", (float) spList.get(i).zMax);
        }
        shader.setUniform("projectionViewMatrix", Camera.camera3d.projectionMatrix().mul(Camera.camera3d.viewMatrix()));
        shader.setUniform("camPos", Camera.camera3d.position);
        gp.bindGBuffer();
        bindAll(brdfLUT, FRAMEBUFFER_VAO);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        GLState.bindShader(null);
        for (int i = 0; i < spList.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + 4 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        Camera.current = Camera.camera3d;
    }
}
