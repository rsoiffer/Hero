package graphics.passes;

import graphics.Camera;
import graphics.Color;
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
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import util.math.Vec2d;
import util.math.Vec3d;

public class LightingPass implements Runnable {

    private static final Shader SHADER_LIGHTING = Shader.load("lighting_pass");
    private static final Texture BRDF_LUT = Texture.load("brdf_lut.png");

    static {
        SHADER_LIGHTING.setUniform("gPosition", 0);
        SHADER_LIGHTING.setUniform("gNormal", 1);
        SHADER_LIGHTING.setUniform("gAlbedo", 2);
        SHADER_LIGHTING.setUniform("gMRA", 3);
        SHADER_LIGHTING.setUniform("gEmissive", 4);
        SHADER_LIGHTING.setUniform("brdfLUT", 5);
        for (int i = 0; i < 5; i++) {
            SHADER_LIGHTING.setUniform("shadowMap[" + i + "]", 6 + i);
        }
        BRDF_LUT.num = 5;
    }

    public Camera camera;
    public Color skyColor;
    public Vec3d sunColor, sunDirection;

    private final GeometryPass gp;
    private final List<ShadowPass> spList;
    private final Framebuffer framebuffer;

    public LightingPass(Vec2d framebufferSize, GeometryPass gp, List<ShadowPass> spList) {
        this.gp = gp;
        this.spList = spList;

        if (framebufferSize == null) {
            framebuffer = new Framebuffer();
        } else {
            framebuffer = new Framebuffer(framebufferSize);
        }
        framebuffer.attachColorBuffer();
        glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0});
        framebuffer.attachDepthRenderbuffer();
        GLState.bindFramebuffer(null);

        GLState.bindShader(null);
        for (int i = 0; i < spList.size(); i++) {
            spList.get(i).bindShadowMap(6 + i);
        }

        GLState.bindShader(null);
        for (int i = 0; i < spList.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + 6 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }

    public Texture colorBuffer() {
        return framebuffer.colorBuffer;
    }

    @Override
    public void run() {
        framebuffer.clear(skyColor);

        GLState.bindShader(null);
        for (int i = 0; i < spList.size(); i++) {
            spList.get(i).bindShadowMap(6 + i);
        }
        SHADER_LIGHTING.setUniform("sunColor", sunColor);
        SHADER_LIGHTING.setUniform("sunDirection", sunDirection);
        for (int i = 0; i < spList.size(); i++) {
            SHADER_LIGHTING.setUniform("lightSpaceMatrix[" + i + "]", spList.get(i).getLightSpaceMatrix());
            SHADER_LIGHTING.setUniform("cascadeEndClipSpace[" + i + "]", (float) spList.get(i).zMax);
        }
        SHADER_LIGHTING.setUniform("projectionViewMatrix", camera.projectionMatrix().mul(camera.viewMatrix()));
        SHADER_LIGHTING.setUniform("camPos", camera.getPos());
        gp.bindGBuffer();
        bindAll(BRDF_LUT, FRAMEBUFFER_VAO);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        GLState.bindShader(null);
        for (int i = 0; i < spList.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + 6 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        GLState.bindFramebuffer(null);
    }
}
