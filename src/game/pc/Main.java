package game.pc;

import behaviors.FPSBehavior;
import behaviors.QuitOnEscapeBehavior;
import engine.Core;
import engine.Input;
import engine.Settings;
import game.Player;
import game.World;
import static game.World.BLOCK_HEIGHT;
import static game.World.BLOCK_WIDTH;
import static graphics.Camera.camera3d;
import graphics.passes.RenderPipeline;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static util.math.MathUtils.clamp;
import util.math.Vec3d;

public class Main {

    public static void main(String[] args) {
        Settings.SHOW_OPENGL_DEBUG_INFO = false;
        Settings.SHOW_CURSOR = false;
        Core.init();

        new FPSBehavior().create();
        new QuitOnEscapeBehavior().create();

        World world = new World();
        world.create();

//        Player p = new Player();
//        p.position.position = new Vec3d(10, 10, 10);
//        p.physics.world = world;
//        p.create();
//
//        UPDATE.onStep(() -> {
//            moveCamera(p);
//        });
//        AssimpModel gunModel = AssimpModel.load("Cerberus.fbx");
//        PBRTexture gunTexture = PBRTexture.loadFromFolder("Cerberus", "tga");
//        PBRModel gun = new PBRModel(gunModel, gunTexture);
//        UPDATE.onStep(() -> gun.t = Transformation.create(camera3d.position.add(camera3d.facing().mul(2)),
//                Quaternion.fromEulerAngles(camera3d.horAngle, camera3d.vertAngle, 0), .01));
//        createRB(gun);
        Frozone f = new Frozone();
        f.player.position.position = new Vec3d(8 * BLOCK_WIDTH - 10, 2 * BLOCK_HEIGHT - 10, 10);
        f.player.physics.world = world;
        f.create();

//        Frozone f2 = new Frozone();
//        f2.player.position.position = new Vec3d(7 * BLOCK_WIDTH - 10, 2 * BLOCK_HEIGHT - 10, 10);
//        f2.player.physics.world = world;
//        f2.player.cameraOffset = null;
//        f2.isPlayer = false;
//        f2.create();
        RenderPipeline rp = new RenderPipeline();
        rp.create();

        Core.run();
    }

    public static void moveCamera(Player p) {
        camera3d.horAngle -= Input.mouseDelta().x * 16. / 3;
        camera3d.vertAngle -= Input.mouseDelta().y * 3;
        camera3d.vertAngle = clamp(camera3d.vertAngle, -1.55, 1.55);

        double flySpeed = 20;
        Vec3d vel = new Vec3d(0, 0, 0);
        if (Input.keyDown(GLFW_KEY_W)) {
            vel = vel.add(camera3d.facing().setLength(flySpeed));
        }
        if (Input.keyDown(GLFW_KEY_A)) {
            vel = vel.add(camera3d.facing().cross(camera3d.up).setLength(-flySpeed));
        }
        if (Input.keyDown(GLFW_KEY_S)) {
            vel = vel.add(camera3d.facing().setLength(-flySpeed));
        }
        if (Input.keyDown(GLFW_KEY_D)) {
            vel = vel.add(camera3d.facing().cross(camera3d.up).setLength(flySpeed));
        }
        if (Input.keyDown(GLFW_KEY_SPACE)) {
            vel = vel.add(camera3d.up.setLength(flySpeed));
        }
        if (Input.keyDown(GLFW_KEY_LEFT_SHIFT)) {
            vel = vel.add(camera3d.up.setLength(-flySpeed));
        }
        p.velocity.velocity = vel;
    }
}
