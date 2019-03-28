package graphics.passes;

import graphics.Camera;
import static graphics.Color.BLACK;
import graphics.Renderable;
import graphics.opengl.Framebuffer;
import static graphics.opengl.GLObject.bindAll;
import graphics.opengl.GLState;
import graphics.opengl.Shader;
import graphics.opengl.Texture;
import java.util.List;
import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_NEAREST;
import static org.lwjgl.opengl.GL11C.GL_RGB;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT1;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT2;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT3;
import static org.lwjgl.opengl.GL30.GL_RGB16F;
import static org.lwjgl.opengl.GL30.GL_RGB32F;
import util.math.Transformation;
import util.math.Vec2d;

public class GeometryPass implements Runnable {

    public static final Shader SHADER_DIFFUSE = Shader.load("geometry_pass_diffuse");
    public static final Shader SHADER_PBR = Shader.load("geometry_pass_pbr");

    static {
        SHADER_PBR.setUniform("albedoMap", 0);
        SHADER_PBR.setUniform("normalMap", 1);
        SHADER_PBR.setUniform("metallicMap", 2);
        SHADER_PBR.setUniform("roughnessMap", 3);
        SHADER_PBR.setUniform("aoMap", 4);
        SHADER_PBR.setUniform("heightMap", 5);
    }

    public List<Renderable> renderTask;
    public Camera camera;

    private final Framebuffer gBuffer;
    private final Texture gPosition, gNormal, gAlbedo, gMRA;

    public GeometryPass(Vec2d framebufferSize) {
        if (framebufferSize == null) {
            gBuffer = new Framebuffer();
        } else {
            gBuffer = new Framebuffer(framebufferSize);
        }
        gBuffer.bind();
        gPosition = gBuffer.attachTexture(GL_RGB32F, GL_RGB, GL_FLOAT, GL_NEAREST, GL_COLOR_ATTACHMENT0);
        gNormal = gBuffer.attachTexture(GL_RGB16F, GL_RGB, GL_FLOAT, GL_NEAREST, GL_COLOR_ATTACHMENT1);
        gAlbedo = gBuffer.attachTexture(GL_RGB, GL_RGB, GL_UNSIGNED_BYTE, GL_NEAREST, GL_COLOR_ATTACHMENT2);
        gMRA = gBuffer.attachTexture(GL_RGB, GL_RGB, GL_UNSIGNED_BYTE, GL_NEAREST, GL_COLOR_ATTACHMENT3);
        glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3});
        gBuffer.attachDepthRenderbuffer();
        GLState.bindFramebuffer(null);

        gPosition.num = 0;
        gNormal.num = 1;
        gAlbedo.num = 2;
        gMRA.num = 3;
    }

    public void bindGBuffer() {
        bindAll(gPosition, gNormal, gAlbedo, gMRA);
    }

    @Override
    public void run() {
        Camera.current = camera;
        GLState.enable(GL_DEPTH_TEST);
        GLState.disable(GL_BLEND);
        gBuffer.clear(BLACK);
        updateShaderUniforms();
        for (Renderable r : renderTask) {
            r.renderGeom();
        }
        GLState.bindFramebuffer(null);
    }

    public static void updateShaderUniforms() {
        SHADER_DIFFUSE.setMVP(Transformation.IDENTITY);
        SHADER_PBR.setMVP(Transformation.IDENTITY);
        SHADER_PBR.setUniform("camPos", Camera.current.getPos());
    }
}