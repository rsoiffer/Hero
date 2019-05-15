package graphics.passes;

import game.RenderableBehavior;
import graphics.Camera;
import graphics.opengl.Framebuffer;
import graphics.opengl.GLState;
import graphics.opengl.Shader;
import graphics.opengl.Texture;
import graphics.passes.RenderPipeline.RenderPass;
import java.util.LinkedList;
import java.util.List;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_BACK;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_FRONT;
import static org.lwjgl.opengl.GL11C.GL_GEQUAL;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_BORDER_COLOR;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glCullFace;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT32;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_COMPARE_FUNC;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_COMPARE_MODE;
import static org.lwjgl.opengl.GL30.GL_COMPARE_REF_TO_TEXTURE;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import physics.AABB;
import util.math.Transformation;
import util.math.Vec3d;

public class ShadowPass implements RenderPass {

    public static final Shader SHADER_SHADOW = Shader.load("shadow_pass");
    public static final Shader SHADER_SHADOW_ALPHA = Shader.load("shadow_pass_alpha");

    static {
        SHADER_SHADOW_ALPHA.setUniform("alphaMap", 6);
    }

    public static ShadowPass current;

    public List<Camera> cameras;
    public double zMin = -1, zMax = 1;
    public Vec3d sunDirection;

    private final Framebuffer shadowMap;
    private final Texture shadowTexture;
    private final Camera sunCam;

    public ShadowPass() {
        shadowMap = new Framebuffer(4096, 4096);
        shadowTexture = shadowMap.attachTexture(GL_DEPTH_COMPONENT32, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, GL_LINEAR, GL_DEPTH_ATTACHMENT);
        sunCam = new Camera() {
            @Override
            public Matrix4d projectionMatrix() {
                AABB aabb = frustumAABB(zMin, zMax, viewMatrix());
                return new Matrix4d().ortho(aabb.lower.x, aabb.upper.x, aabb.lower.y, aabb.upper.y, -aabb.upper.z - 300, -aabb.lower.z);
            }

            @Override
            public Matrix4d viewMatrix() {
                return new Matrix4d().lookAt(sunDirection.toJOML(), new Vector3d(0, 0, 0), new Vector3d(0, 0, 1));
            }
        };
        shadowTexture.setParameter(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        shadowTexture.setParameter(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        shadowTexture.setParameter(GL_TEXTURE_BORDER_COLOR, new float[]{1, 1, 1, 1});
    }

    public void bindShadowMap(int num) {
        shadowTexture.num = num;
        shadowTexture.bind();
        shadowTexture.setParameter(GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
        shadowTexture.setParameter(GL_TEXTURE_COMPARE_FUNC, GL_GEQUAL);
    }

    private AABB frustumAABB(double zMin, double zMax, Matrix4d sunView) {
        List<Vec3d> points = new LinkedList();
        for (Camera c : cameras) {
            Matrix4d m = c.projectionMatrix().mul(c.viewMatrix());
            m.invert();
            m = sunView.mul(m, new Matrix4d());
            for (int x = -1; x <= 1; x += 2) {
                for (int y = -1; y <= 1; y += 2) {
                    Vector4d v = new Vector4d(x, y, zMin, 1).mul(m);
                    points.add(new Vec3d(v.x / v.w, v.y / v.w, v.z / v.w));
                    v = new Vector4d(x, y, zMax, 1).mul(m);
                    points.add(new Vec3d(v.x / v.w, v.y / v.w, v.z / v.w));
                }
            }
        }
        return AABB.boundingBox(points);
    }

    public Matrix4d getLightSpaceMatrix() {
        return sunCam.projectionMatrix().mul(sunCam.viewMatrix());
    }

    @Override
    public void run() {
        Camera.current = sunCam;
        SHADER_SHADOW.setMVP(Transformation.IDENTITY);
        SHADER_SHADOW_ALPHA.setMVP(Transformation.IDENTITY);
        shadowMap.bind();
        GLState.enable(GL_DEPTH_TEST);
        glClear(GL_DEPTH_BUFFER_BIT);
        glCullFace(GL_FRONT);
        RenderableBehavior.allRenderables().forEach(r -> r.renderShadow());
        glCullFace(GL_BACK);
        GLState.bindFramebuffer(null);
    }
}
