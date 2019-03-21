package game;

import behaviors.FPSBehavior;
import engine.Core;
import static engine.Core.dt;
import engine.Input;
import static engine.Layer.PREUPDATE;
import static engine.Layer.UPDATE;
import graphics.AssimpModel;
import graphics.Camera;
import static graphics.Camera.camera3d;
import graphics.GeometryPass;
import graphics.LightingPass;
import graphics.PBRTexture;
import graphics.ShadowPass;
import graphics.Window;
import graphics.opengl.Framebuffer;
import graphics.opengl.GLState;
import java.util.LinkedList;
import java.util.List;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static util.math.MathUtils.clamp;
import util.math.Quaternion;
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

        AssimpModel m = AssimpModel.load("Cerberus.fbx");
        PBRTexture t = PBRTexture.loadFromFolder("Cerberus", "tga");

        Runnable renderTask = () -> {
            w.render();
            GLState.getShaderProgram().setUniform("model", Transformation.create(
                    Camera.camera3d.position.add(Camera.camera3d.facing().mul(2)), Quaternion.IDENTITY, .01).modelMatrix());
            m.draw(t);
        };

        Vec3d sunColor = new Vec3d(10, 8, 6);
        Vec3d sunDirection = new Vec3d(.4, -.2, 1);

        GeometryPass gp = new GeometryPass();
        gp.renderTask = renderTask;
        gp.create();

        List<ShadowPass> spList = new LinkedList();
        for (int i = 0; i < 5; i++) {
            ShadowPass sp = new ShadowPass();
            sp.renderTask = renderTask;
            sp.zMin = i == 0 ? -1 : (1 - Math.pow(.3, i + 2));
            sp.zMax = 1 - Math.pow(.3, i + 3);
            sp.sunDirection = sunDirection;
            sp.create();
            spList.add(sp);
        }

        LightingPass lp = new LightingPass();
        lp.clearColor = clearColor;
        lp.gp = gp;
        lp.spList = spList;
        lp.sunColor = sunColor;
        lp.sunDirection = sunDirection;
        lp.create();

//        Mutable<Double> time = new Mutable(0.);
//        UPDATE.onStep(() -> {
//            time.o += dt() * .1;
//            Vec3d sd = new Vec3d(Math.sin(time.o), Math.cos(time.o), 1);
//            for (int i = 0; i < 5; i++) {
//                spList.get(i).sunDirection = sd;
//            }
//            lp.sunDirection = sd;
//        });
        Core.run();
    }
}
