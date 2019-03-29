package game.pc;

import behaviors.FPSBehavior;
import behaviors.QuitOnEscapeBehavior;
import engine.Core;
import static engine.Core.dt;
import engine.Input;
import static engine.Layer.UPDATE;
import engine.Settings;
import game.Player;
import game.World;
import static graphics.Camera.camera3d;
import graphics.PBRTexture;
import graphics.Renderable;
import graphics.Renderable.RenderablePBR;
import graphics.SDF;
import static graphics.SDF.cone;
import static graphics.SDF.cylinder;
import static graphics.SDF.halfSpace;
import static graphics.SDF.intersectionSmooth;
import graphics.models.AssimpModel;
import graphics.models.SurfaceNet;
import graphics.passes.RenderPipeline;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_T;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import physics.AABB;
import util.Mutable;
import static util.math.MathUtils.clamp;
import util.math.Quaternion;
import util.math.Transformation;
import util.math.Vec3d;

public class Main {

    public static void main(String[] args) {
        Settings.SHOW_CURSOR = false;
        Core.init();

        new FPSBehavior().create();
        new QuitOnEscapeBehavior().create();

        World world = new World();

//        Player p = new Player();
//        p.position.position = new Vec3d(10, 10, 10);
//        p.physics.world = world;
//        p.create();
//
//        UPDATE.onStep(() -> {
//            moveCamera(p);
//        });
        AssimpModel gunModel = AssimpModel.load("Cerberus.fbx");
        PBRTexture gunTexture = PBRTexture.loadFromFolder("Cerberus", "tga");
        Supplier<Transformation> gunPos = () -> Transformation.create(camera3d.position.add(camera3d.facing().mul(2)),
                Quaternion.fromEulerAngles(camera3d.horAngle, camera3d.vertAngle, 0), .01);
        Renderable gun = new RenderablePBR(gunModel, gunTexture, gunPos);

        SurfaceNet iceModel = new SurfaceNet(.5);
        PBRTexture iceTexture = PBRTexture.loadFromFolder("ice2");
        Renderable ice = new RenderablePBR(iceModel, iceTexture, () -> Transformation.IDENTITY);
//        icePathControls(iceModel);

        Frozone f = new Frozone();
        f.player.position.position = new Vec3d(10, 10, 10);
        f.player.physics.world = world;
        f.iceModel = iceModel;
        f.create();

        Frozone f2 = new Frozone();
        f2.player.position.position = new Vec3d(-10, -10, 10);
        f2.player.physics.world = world;
        f2.iceModel = iceModel;
        f2.isPlayer = false;
        f2.create();

        List<Renderable> renderTask = Arrays.asList(world, gun, ice);

        RenderPipeline rp = new RenderPipeline();
        rp.renderTask = renderTask;
        rp.create();

        Core.run();
    }

    public static void icePathControls(SurfaceNet iceModel) {
        Mutable<Boolean> fly = new Mutable(false);
        UPDATE.onStep(() -> {
            if (Input.keyJustPressed(GLFW_KEY_R)) {
                fly.o = !fly.o;
            }
            if (fly.o) {
                camera3d.position = camera3d.position.add(camera3d.facing().setLength(dt() * 50));

                Vec3d side = camera3d.facing().cross(camera3d.up);
                Vec3d normal = camera3d.facing().cross(side).normalize();
                Vec3d pos1 = camera3d.position.add(normal.mul(2));
                Vec3d pos2 = pos1.add(camera3d.facing().mul(3));

                // SDF shape = intersectionSmooth(3, cylinder(pos1, camera3d.facing(), 3), halfSpace(pos1, normal), halfSpace(pos1, camera3d.facing()), halfSpace(pos2, camera3d.facing().mul(-1)));
                SDF shape = intersectionSmooth(3, cylinder(pos1, camera3d.facing(), 3), halfSpace(pos1, normal), halfSpace(pos1.add(normal.mul(1.5)), normal.mul(-1)), halfSpace(pos1, camera3d.facing()), halfSpace(pos2, camera3d.facing().mul(-1)));
                AABB bounds = AABB.boundingBox(Arrays.asList(pos1.sub(3), pos1.add(3), pos2.sub(3), pos2.add(3)));
                iceModel.unionSDF(shape, bounds);

//                if (Math.random() < dt()) {
//                    SDF shape2 = intersectionSmooth(3, cylinder(pos1, new Vec3d(0, 0, 1), 1), halfSpace(pos1, new Vec3d(0, 0, -1)), halfSpace(pos1.add(new Vec3d(0, 0, -20)), new Vec3d(0, 0, 1)));
//                    AABB bounds2 = AABB.boundingBox(Arrays.asList(pos1.sub(3), pos1.add(3), pos1.add(new Vec3d(0, 0, -20)).sub(3), pos1.add(new Vec3d(0, 0, -20)).add(3)));
//                    iceModel.unionSDF(shape2, bounds2);
//                }
            }

            if (Input.keyJustPressed(GLFW_KEY_T)) {
                Vec3d pos1 = camera3d.position.add(camera3d.facing().mul(3));
                Vec3d pos2 = pos1.add(camera3d.facing().mul(30));
                SDF shape = intersectionSmooth(3, cone(pos1, camera3d.facing(), .1), halfSpace(pos1, camera3d.facing()), halfSpace(pos2, camera3d.facing().mul(-1)));
                AABB bounds = AABB.boundingBox(Arrays.asList(pos1.sub(3), pos1.add(3), pos2.sub(3), pos2.add(3)));
                iceModel.unionSDF(shape, bounds);
            }
        });
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
