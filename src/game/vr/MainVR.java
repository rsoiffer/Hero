package game.vr;

import behaviors.FPSBehavior;
import behaviors.QuitOnEscapeBehavior;
import engine.Core;
import static engine.Layer.UPDATE;
import game.Player;
import game.World;
import graphics.Camera;
import graphics.Renderable;
import graphics.passes.RenderPipeline;
import java.util.Arrays;
import java.util.List;
import util.math.Vec3d;
import vr.Vive;
import vr.ViveInput;
import static vr.ViveInput.MENU;

public class MainVR {

    public static void main(String[] args) {
        Core.init();

        new FPSBehavior().create();
        new QuitOnEscapeBehavior().create();
        Camera.current = Camera.camera3d;
        Vive.init();

        UPDATE.onStep(() -> {
            ViveInput.update();
            if (ViveInput.LEFT.buttonDown(MENU) && ViveInput.RIGHT.buttonDown(MENU)) {
                ViveInput.resetRightLeft();
                Vive.resetSeatedZeroPose();
            }
        });

        World world = new World();

        Player p = new Player();
        p.position.position = new Vec3d(10, 10, 10);
        p.physics.world = world;
        p.cameraOffset = new Vec3d(0, 0, -1);
        p.create();

//        Class[] c = {WebSlinger.class, Thruster.class, Hookshot.class};
//        Mutable<Integer> leftType = new Mutable(0);
//        Mutable<Behavior> left = new Mutable(null);
//        Mutable<Integer> rightType = new Mutable(0);
//        Mutable<Behavior> right = new Mutable(null);
//
//        UPDATE.onStep(() -> {
//            if (left.o == null || ViveInput.LEFT.buttonJustPressed(GRIP)) {
//                if (left.o != null) {
//                    left.o.destroy();
//                }
//                leftType.o = (leftType.o + 1) % c.length;
//                try {
//                    left.o = (Behavior) c[leftType.o].newInstance();
//                } catch (InstantiationException | IllegalAccessException ex) {
//                    throw new RuntimeException(ex);
//                }
//                left.o.get(ControllerBehavior.class).controller = ViveInput.LEFT;
//                left.o.get(ControllerBehavior.class).player = p;
//                left.o.create();
//            }
//            if (right.o == null || ViveInput.RIGHT.buttonJustPressed(GRIP)) {
//                if (right.o != null) {
//                    right.o.destroy();
//                }
//                rightType.o = (rightType.o + 1) % c.length;
//                try {
//                    right.o = (Behavior) c[rightType.o].newInstance();
//                } catch (InstantiationException | IllegalAccessException ex) {
//                    throw new RuntimeException(ex);
//                }
//                right.o.get(ControllerBehavior.class).controller = ViveInput.RIGHT;
//                right.o.get(ControllerBehavior.class).player = p;
//                right.o.create();
//            }
//        });
        List<Renderable> renderTask = Arrays.asList(world);

        RenderPipeline rp = new RenderPipeline();
        rp.renderTask = renderTask;
        rp.create();

        Core.run();
    }
}
