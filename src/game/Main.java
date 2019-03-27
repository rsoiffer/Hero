package game;

import behaviors.FPSBehavior;
import engine.Core;
import static engine.Core.dt;
import engine.Input;
import static engine.Layer.UPDATE;
import graphics.AssimpModel;
import graphics.Camera;
import static graphics.Camera.camera3d;
import graphics.Color;
import graphics.GeometryPass;
import graphics.LightingPass;
import graphics.PBRTexture;
import graphics.Renderable;
import graphics.Renderable.RenderablePBR;
import graphics.SDF;
import static graphics.SDF.cylinder;
import static graphics.SDF.halfSpace;
import static graphics.SDF.intersectionSmooth;
import graphics.ShadowPass;
import graphics.SurfaceNet;
import graphics.Window;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
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
        Core.init();

        new FPSBehavior().create();
        Window.window.setCursorEnabled(false);

        Camera.current = camera3d;
        camera3d.position = new Vec3d(10, 10, 10);

        UPDATE.onStep(() -> {
            if (Input.keyJustPressed(GLFW_KEY_ESCAPE)) {
                Core.stopGame();
            }
            moveCamera();
        });

        World world = new World();

        AssimpModel gunModel = AssimpModel.load("Cerberus.fbx");
        PBRTexture gunTexture = PBRTexture.loadFromFolder("Cerberus", "tga");
        Supplier<Transformation> gunPos = () -> Transformation.create(camera3d.position.add(camera3d.facing().mul(2)),
                Quaternion.fromEulerAngles(camera3d.horAngle, camera3d.vertAngle, 0), .01);
        Renderable gun = new RenderablePBR(gunModel, gunTexture, gunPos);

        SurfaceNet iceModel = new SurfaceNet(1);
        PBRTexture iceTexture = PBRTexture.loadFromFolder("ice");
        Renderable ice = new RenderablePBR(iceModel, iceTexture, () -> Transformation.IDENTITY);
        icePathControls(iceModel);

        List<Renderable> renderTask = Arrays.asList(world, gun, ice);
        renderPipeline(renderTask);

        Core.run();
    }

    public static void icePathControls(SurfaceNet sn) {
        Mutable<Boolean> fly = new Mutable(false);
        UPDATE.onStep(() -> {
            if (Input.keyJustPressed(GLFW_KEY_R)) {
                fly.o = !fly.o;
            }
            if (fly.o) {
                camera3d.position = camera3d.position.add(camera3d.facing().setLength(dt() * 20));

                Vec3d side = camera3d.facing().cross(camera3d.up);
                Vec3d normal = camera3d.facing().cross(side).normalize();
                Vec3d pos1 = camera3d.position.add(normal.mul(2));
                Vec3d pos2 = pos1.add(camera3d.facing().mul(3));

                SDF shape = intersectionSmooth(3, cylinder(pos1, camera3d.facing(), 3), halfSpace(pos1, normal), halfSpace(pos1, camera3d.facing()), halfSpace(pos2, camera3d.facing().mul(-1)));
                AABB bounds = AABB.boundingBox(Arrays.asList(pos1.sub(3), pos1.add(3), pos2.sub(3), pos2.add(3)));
                sn.unionSDF(shape, bounds);
            }

            if (Input.keyJustPressed(GLFW_KEY_T)) {
                Vec3d pos1 = camera3d.position.add(camera3d.facing().mul(3));
                Vec3d pos2 = pos1.add(camera3d.facing().mul(30));
                SDF shape = intersectionSmooth(3, cylinder(pos1, camera3d.facing(), 3), halfSpace(pos1, camera3d.facing()), halfSpace(pos2, camera3d.facing().mul(-1)));
                AABB bounds = AABB.boundingBox(Arrays.asList(pos1.sub(3), pos1.add(3), pos2.sub(3), pos2.add(3)));
                sn.unionSDF(shape, bounds);
            }
        });
    }

    public static void moveCamera() {
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
    }

    public static void renderPipeline(List<Renderable> renderTask) {
        Color clearColor = new Color(.4, .7, 1, 1);
        Vec3d sunColor = new Vec3d(10, 9, 8).mul(.3);
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
    }
}
