package graphics;

import engine.Behavior;
import engine.Layer;
import static engine.Layer.POSTUPDATE;
import static graphics.Color.BLACK;
import graphics.opengl.Framebuffer;
import static graphics.opengl.GLObject.bindAll;
import graphics.opengl.GLState;
import graphics.opengl.Shader;
import graphics.opengl.Texture;
import org.joml.Vector4d;
import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_NEAREST;
import static org.lwjgl.opengl.GL11C.GL_RGB;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT1;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT2;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT3;
import static org.lwjgl.opengl.GL30.GL_RGB16F;
import static org.lwjgl.opengl.GL30.GL_RGB32F;
import util.math.Transformation;
import util.math.Vec3d;

public class GeometryPass extends Behavior {

    public Runnable renderTask;

    private Shader shader;
    private Framebuffer gBuffer;
    private Texture gPosition, gNormal, gAlbedo, gMRA;

    public void bindGBuffer() {
        bindAll(gPosition, gNormal, gAlbedo, gMRA);
    }

    @Override
    public void createInner() {
        shader = Shader.load("geometry_pass");
        shader.setUniform("albedoMap", 0);
        shader.setUniform("normalMap", 1);
        shader.setUniform("metallicMap", 2);
        shader.setUniform("roughnessMap", 3);
        shader.setUniform("aoMap", 4);
        shader.setUniform("heightMap", 5);

        gBuffer = new Framebuffer();
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

    @Override
    public Layer layer() {
        return POSTUPDATE;
    }

    @Override
    public void step() {
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        gBuffer.clear(BLACK);
        shader.setMVP(Transformation.IDENTITY);
        Vector4d v = new Vector4d(0, 0, 0, 1).mul(Camera.current.viewMatrix().invert());
        shader.setUniform("camPos", new Vec3d(v.x, v.y, v.z));
        renderTask.run();
        GLState.bindFramebuffer(null);
    }
}
