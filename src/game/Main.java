package game;

import behaviors.FPSBehavior;
import engine.Core;
import static engine.Core.dt;
import engine.Input;
import static engine.Layer.PREUPDATE;
import static engine.Layer.RENDER3D;
import static engine.Layer.UPDATE;
import graphics.Camera;
import static graphics.Camera.camera3d;
import graphics.Color;
import graphics.GeometryPass;
import graphics.LightingPass;
import graphics.ShadowPass;
import graphics.Window;
import graphics.opengl.Framebuffer;
import graphics.voxels.VoxelModel;
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

//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException ex) {
//                ex.printStackTrace();
//            }
        });

        RENDER3D.onStep(() -> {
            VoxelModel.load("controller.vox").render(Transformation.create(new Vec3d(5, 5, 1), Quaternion.IDENTITY, 1 / 32.), Color.WHITE);
        });

        World w = new World();
        w.create();
        camera3d.position = new Vec3d(10, 10, 10);

        GeometryPass gp = new GeometryPass();
        gp.w = w;
        gp.create();

        List<ShadowPass> spList = new LinkedList();
        for (int i = 0; i < 5; i++) {
            ShadowPass sp = new ShadowPass();
            sp.w = w;
            sp.zMin = i == 0 ? -1 : (1 - Math.pow(.3, i + 2));
            sp.zMax = 1 - Math.pow(.3, i + 3);
            sp.create();
            spList.add(sp);
        }

        LightingPass lp = new LightingPass();
        lp.clearColor = clearColor;
        lp.gp = gp;
        lp.spList = spList;
        lp.create();

        Core.run();
    }
}
