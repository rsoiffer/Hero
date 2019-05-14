package game.vr;

import behaviors.FPSBehavior;
import behaviors.QuitOnEscapeBehavior;
import engine.Behavior;
import engine.Core;
import static engine.Layer.UPDATE;
import engine.Settings;
import game.Player;
import game.World;
import static game.World.BLOCK_HEIGHT;
import static game.World.BLOCK_WIDTH;
import graphics.Camera;
import graphics.passes.RenderPipeline;
import util.Mutable;
import static util.math.MathUtils.floor;
import static util.math.MathUtils.mod;
import util.math.Vec2d;
import util.math.Vec3d;
import vr.Vive;
import vr.ViveInput;
import static vr.ViveInput.MENU;
import static vr.ViveInput.TRACKPAD;

public class MainVR {

    public static void main(String[] args) {
        Settings.SHOW_OPENGL_DEBUG_INFO = false;
        Settings.ENABLE_VSYNC = false;
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
        world.create();

        Player p = new Player();
        p.position.position = new Vec3d(8 * BLOCK_WIDTH - 10, 2 * BLOCK_HEIGHT - 10, 10);
        p.physics.world = world;
        p.cameraOffset = new Vec3d(0, 0, -1);
        p.create();

//        Frozone f2 = new Frozone();
//        f2.player.position.position = new Vec3d(7 * BLOCK_WIDTH - 10, 2 * BLOCK_HEIGHT - 10, 10);
//        f2.player.physics.world = world;
//        f2.player.cameraOffset = null;
//        f2.isPlayer = false;
//        f2.create();
//        Class[] c = {WebSlinger.class, Thruster.class, Hookshot.class, IceCaster.class,
//            Wing.class, Hand.class, Explosion.class, Teleport.class};
        Class[] c = {WebSlinger.class, Thruster.class, IceCaster.class,
            Wing.class, Hand.class, Teleport.class};
        Mutable<Integer> leftType = new Mutable(1);
        Mutable<Behavior> left = new Mutable(null);
        Mutable<Integer> rightType = new Mutable(1);
        Mutable<Behavior> right = new Mutable(null);

        UPDATE.onStep(() -> {
            if (ViveInput.LEFT.buttonJustPressed(TRACKPAD)) {
                if (left.o != null) {
                    left.o.destroy();
                    left.o = null;
                }
                Vec2d v = ViveInput.LEFT.trackpad();
                leftType.o = floor(mod(Math.atan2(v.y, v.x) / (2 * Math.PI), 1) * c.length);
            }
            if (left.o == null) {
                try {
                    left.o = (Behavior) c[leftType.o].newInstance();
                } catch (InstantiationException | IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
                left.o.get(ControllerBehavior.class).controller = ViveInput.LEFT;
                left.o.get(ControllerBehavior.class).player = p;
                left.o.create();
            }
            if (ViveInput.RIGHT.buttonJustPressed(TRACKPAD)) {
                if (right.o != null) {
                    right.o.destroy();
                    right.o = null;
                }
                Vec2d v = ViveInput.RIGHT.trackpad();
                rightType.o = floor(mod(Math.atan2(v.y, -v.x) / (2 * Math.PI), 1) * c.length);
            }
            if (right.o == null) {
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

        RenderPipeline rp = new RenderPipeline();
        rp.isVR = true;
        rp.create();

        Core.run();
    }
}
