package game;

import behaviors.FPSBehavior;
import engine.Behavior;
import engine.Core;
import engine.Input;
import static engine.Layer.POSTRENDER;
import static engine.Layer.PREUPDATE;
import static engine.Layer.UPDATE;
import graphics.Camera;
import graphics.Color;
import graphics.opengl.Framebuffer;
import graphics.opengl.GLState;
import graphics.sprites.Sprite;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import util.Mutable;
import util.math.Transformation;
import util.math.Vec2d;
import util.math.Vec3d;
import util.math.Vec4d;
import vr.Vive;
import vr.ViveInput;
import static vr.ViveInput.GRIP;
import static vr.ViveInput.MENU;

public class MainVR {

    public static void main(String[] args) {
        Core.init();

        new FPSBehavior().create();
        Camera.current = Camera.camera3d;
        Vec4d clearColor = new Vec4d(.4, .7, 1, 1);
        Vive.init();
        Vive.initRender(clearColor);

        PREUPDATE.onStep(() -> {
            Framebuffer.clearWindow(Color.BLACK);
        });

        POSTRENDER.onStep(() -> {
            GLState.inTempState(() -> {
                Camera.current = Camera.camera2d;
                GLState.disable(GL_DEPTH_TEST);
                Framebuffer.clearWindow(Color.BLACK);
                Sprite.drawTexture(Vive.leftEye.colorBuffer, Transformation.create(new Vec2d(.25, .5), new Vec2d(.48, 0), new Vec2d(0, .96)), Color.WHITE);
                Sprite.drawTexture(Vive.rightEye.colorBuffer, Transformation.create(new Vec2d(.75, .5), new Vec2d(.48, 0), new Vec2d(0, .96)), Color.WHITE);
                Camera.current = Camera.camera3d;
            });
        });

        UPDATE.onStep(() -> {
            if (Input.keyJustPressed(GLFW_KEY_ESCAPE)) {
                Core.stopGame();
            }
            ViveInput.update();
            if (ViveInput.LEFT.buttonDown(MENU) && ViveInput.RIGHT.buttonDown(MENU)) {
                ViveInput.resetRightLeft();
                Vive.resetSeatedZeroPose();
            }
        });

        World w = new World();
        w.create();

        Player p = new Player();
        p.position.position = new Vec3d(10, 10, 10);
        p.physics.world = w;
        p.create();

        Class[] c = {WebSlinger.class, Thruster.class, Hookshot.class};
        Mutable<Integer> leftType = new Mutable(0);
        Mutable<Behavior> left = new Mutable(null);
        Mutable<Integer> rightType = new Mutable(0);
        Mutable<Behavior> right = new Mutable(null);

        UPDATE.onStep(() -> {
            if (left.o == null || ViveInput.LEFT.buttonJustPressed(GRIP)) {
                if (left.o != null) {
                    left.o.destroy();
                }
                leftType.o = (leftType.o + 1) % c.length;
                try {
                    left.o = (Behavior) c[leftType.o].newInstance();
                } catch (InstantiationException | IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
                left.o.get(ControllerBehavior.class).controller = ViveInput.LEFT;
                left.o.get(ControllerBehavior.class).player = p;
                left.o.create();
            }
            if (right.o == null || ViveInput.RIGHT.buttonJustPressed(GRIP)) {
                if (right.o != null) {
                    right.o.destroy();
                }
                rightType.o = (rightType.o + 1) % c.length;
                try {
                    right.o = (Behavior) c[rightType.o].newInstance();
                } catch (InstantiationException | IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
                right.o.get(ControllerBehavior.class).controller = ViveInput.RIGHT;
                right.o.get(ControllerBehavior.class).player = p;
                right.o.create();
            }
        });

        Core.run();
    }
}
