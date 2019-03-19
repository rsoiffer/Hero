package game;

import behaviors.FPSBehavior;
import engine.Core;
import static engine.Core.dt;
import engine.Input;
import static engine.Layer.POSTUPDATE;
import static engine.Layer.PREUPDATE;
import static engine.Layer.UPDATE;
import graphics.Camera;
import static graphics.Camera.camera3d;
import static graphics.PBRTexture.PBR;
import graphics.Window;
import graphics.opengl.Framebuffer;
import graphics.opengl.GLState;
import graphics.opengl.ShaderProgram;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.opengl.GL11C.GL_BACK;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_FRONT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glCullFace;
import util.Resources;
import static util.math.MathUtils.clamp;
import util.math.Transformation;
import util.math.Vec3d;
import util.math.Vec4d;

public class Main {

    public static void main(String[] args) {
        Core.init();

        new FPSBehavior().create();
        Camera.current = Camera.camera3d;
        Vec4d clearColor = new Vec4d(.4, .7, 1, 1);
        Window.window.setCursorEnabled(false);

        PREUPDATE.onStep(() -> {
            Framebuffer.clearWindow(clearColor);
        });

        UPDATE.onStep(() -> {
            if (Input.keyJustPressed(GLFW_KEY_ESCAPE)) {
                Core.stopGame();
            }

            camera3d.horAngle -= Input.mouseDelta().x * 16. / 3;
            camera3d.vertAngle -= Input.mouseDelta().y * 3;
            camera3d.vertAngle = clamp(camera3d.vertAngle, -1.55, 1.55);

            double flySpeed = 20;
            if (Input.keyDown(GLFW_KEY_W)) {
                camera3d.position = camera3d.position.add(camera3d.facing().setLength(dt() * flySpeed));
            }
            if (Input.keyDown(GLFW_KEY_A)) {
                camera3d.position = camera3d.position.add(camera3d.facing().cross(camera3d.up).setLength(-dt() * flySpeed));
            }
            if (Input.keyDown(GLFW_KEY_S)) {
                camera3d.position = camera3d.position.add(camera3d.facing().setLength(-dt() * flySpeed));
            }
            if (Input.keyDown(GLFW_KEY_D)) {
                camera3d.position = camera3d.position.add(camera3d.facing().cross(camera3d.up).setLength(dt() * flySpeed));
            }
            if (Input.keyDown(GLFW_KEY_SPACE)) {
                camera3d.position = camera3d.position.add(camera3d.up.setLength(dt() * flySpeed));
            }
            if (Input.keyDown(GLFW_KEY_LEFT_SHIFT)) {
                camera3d.position = camera3d.position.add(camera3d.up.setLength(-dt() * flySpeed));
            }
        });

        World w = new World();
        w.create();
        camera3d.position = new Vec3d(10, 10, 10);

        ShaderProgram shadow = Resources.loadShaderProgram("color", "empty");
        Framebuffer shadowMap = new Framebuffer(4096, 4096).attachDepthStencilBuffer();
        Camera sunCam = new Camera() {
            @Override
            public Matrix4d projectionMatrix() {
                return new Matrix4d().ortho(-300, 300, -300, 300, -200, 200);
            }

            @Override
            public Matrix4d viewMatrix() {
                return new Matrix4d().lookAt(new Vector3d(.4, .7, 1).add(Camera.camera3d.position.toJOML()), Camera.camera3d.position.toJOML(), new Vector3d(0, 0, 1));
            }
        };
        shadowMap.depthStencilBuffer.num = 6;
        shadowMap.depthStencilBuffer.setParameter(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        shadowMap.depthStencilBuffer.setParameter(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        POSTUPDATE.onStep(() -> {
            Camera.current = sunCam;
            shadow.setMVP(Transformation.IDENTITY);
            shadowMap.bind();
            glClear(GL_DEPTH_BUFFER_BIT);
            glCullFace(GL_FRONT);
            w.step();
            glCullFace(GL_BACK);
            GLState.bindFramebuffer(null);
            Camera.current = Camera.camera3d;
            PBR.setUniform("lightSpaceMatrix", sunCam.projectionMatrix().mul(sunCam.viewMatrix()));
            shadowMap.depthStencilBuffer.bind();
        });

        Core.run();
    }
}
