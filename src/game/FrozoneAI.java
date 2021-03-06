package game;

import engine.Behavior;
import static engine.Core.dt;
import engine.Input;
import engine.Layer;
import static engine.Layer.PREUPDATE;
import game.pc.Main;
import static game.vr.IceCaster.iceModel;
import static graphics.Camera.camera3d;
import graphics.SDF;
import static graphics.SDF.cylinder;
import static graphics.SDF.halfSpace;
import static graphics.SDF.intersectionSmooth;
import java.util.Arrays;
import java.util.Random;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import physics.shapes.AABB;
import util.math.MathUtils;
import static util.math.MathUtils.clamp;
import util.math.Vec3d;

public class FrozoneAI extends Behavior {

    public final Player player = require(Player.class);

    public boolean fly = true;
    public double timer;
    public double timer2;

    public boolean isPlayer = true;

    private void createIce(Vec3d pos1, Vec3d pos2, Vec3d up) {
        double radius = 2, thickness = 1.5, negRadius = 1.5;
        Vec3d dir = pos2.sub(pos1);

        SDF shape2 = intersectionSmooth(3,
                cylinder(pos1, dir, negRadius),
                halfSpace(pos1, dir),
                halfSpace(pos2, dir.mul(-1))).invert();
        AABB bounds2 = AABB.boundingBox(Arrays.asList(pos1.sub(negRadius), pos1.add(negRadius), pos2.sub(negRadius), pos2.add(negRadius)));
        iceModel.intersectionSDF(shape2, bounds2);

        Vec3d side = dir.cross(up).normalize();
        // side = dir.cross(up.add(side.mul(.1 * Math.sin(5 * timer)))).normalize();
        Vec3d normal = dir.cross(side).normalize();
        pos1 = pos1.add(normal);
        pos2 = pos2.add(normal);

        SDF shape = intersectionSmooth(3,
                cylinder(pos1, dir, radius),
                halfSpace(pos1, normal),
                halfSpace(pos1.add(normal.mul(thickness)), normal.mul(-1)),
                halfSpace(pos1, dir),
                halfSpace(pos2, dir.mul(-1)));
        AABB bounds = AABB.boundingBox(Arrays.asList(pos1.sub(radius), pos1.add(radius), pos2.sub(radius), pos2.add(radius)));
        iceModel.unionSDF(shape, bounds);
    }

    @Override
    public Layer layer() {
        return PREUPDATE;
    }

    private void moveTowards(Vec3d vel) {
        timer += dt();
        timer2 += dt();

        double height = player.physics.world.raycastDown(player.pose.position);
        double speedMod = 8 + 50 * Math.pow(.7, height);

        Vec3d side = player.physics.velocity.add(MathUtils.randomInSphere(new Random()).mul(1e-12)).cross(new Vec3d(0, 0, 1));

        Vec3d accel = vel.add(side.mul(.1 * Math.sin(5 * timer))).add(new Vec3d(0, 0, speedMod));
        player.physics.velocity = player.physics.velocity.add(accel.mul(dt()));
        if (player.physics.velocity.length() > 20) {
            player.physics.velocity = player.physics.velocity.setLength(20);
        }

        if (timer2 > 0) {
            timer2 -= 1 / 30.;
            createIce(player.pose.position, player.pose.position.add(player.physics.velocity.mul(.3)), accel);
        }
    }

    @Override
    public void step() {
        if (isPlayer) {
            if (Input.keyJustPressed(GLFW_KEY_R)) {
                fly = !fly;
            }
            if (fly) {
                Main.moveCamera(player);
            } else {
                camera3d.horAngle -= Input.mouseDelta().x * 16. / 3;
                camera3d.vertAngle -= Input.mouseDelta().y * 3;
                camera3d.vertAngle = clamp(camera3d.vertAngle, -1.55, 1.55);

                double speed = 0;
                if (Input.keyDown(GLFW_KEY_W)) {
                    speed += 20;
                }
                if (Input.keyDown(GLFW_KEY_S)) {
                    speed -= 10;
                }
                moveTowards(camera3d.facing().mul(speed));
            }
        } else {
            moveTowards(camera3d.position.sub(player.pose.position).setLength(15));
        }
    }
}
