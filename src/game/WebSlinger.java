package game;

import engine.Behavior;
import engine.Layer;
import static engine.Layer.RENDER3D;
import static game.Player.POSTPHYSICS;
import graphics.Color;
import graphics.voxels.VoxelModel;
import util.math.Transformation;
import util.math.Vec3d;
import static vr.ViveInput.TRIGGER;

public class WebSlinger extends Behavior {

    public final ControllerBehavior controller = require(ControllerBehavior.class);

    public Vec3d web;
    public double prefLength;

    private Behavior renderer;

    @Override
    public void createInner() {
        controller.model = VoxelModel.load("controller.vox");
        renderer = RENDER3D.onStep(() -> {
            if (web != null) {
                Vec3d pos = controller.pos();
                Vec3d forwards = web.sub(pos);
                Vec3d side = forwards.cross(new Vec3d(0, 0, 1)).setLength(.05);
                Vec3d up = forwards.cross(side).setLength(.05);;
                Vec3d pos2 = pos.sub(side.div(2)).sub(up.div(2));
                VoxelModel.load("singlevoxel.vox").render(Transformation.create(pos2, forwards, side, up), Color.WHITE);
            }
        });
    }

    @Override
    public void destroyInner() {
        renderer.destroy();
    }

    @Override
    public Layer layer() {
        return POSTPHYSICS;
    }

    @Override
    public void step() {
        if (controller.controller.buttonJustPressed(TRIGGER)) {
            Vec3d start = controller.pos();
            Vec3d dir = controller.controller.forwards();
            double t = controller.player.physics.world.buildings.stream().mapToDouble(a -> a.raycast(start, dir))
                    .filter(d -> d >= 0).min().orElse(-1);
            if (t == -1) {
                web = null;
            } else {
                web = start.add(dir.mul(t));
                prefLength = t - 4;
            }
        }
        if (controller.controller.buttonJustReleased(TRIGGER)) {
            web = null;
        }
        if (web != null) {
            double exag = 10;
            prefLength = Math.min(prefLength, web.sub(controller.pos(exag)).length() - controller.controller.trigger());
            Vec3d pullDir = web.sub(controller.pos(exag)).normalize();
            double strength = 10 * Math.max(controller.pos(exag).sub(web).length() - prefLength, 0);
            controller.player.applyForce(pullDir.mul(strength), .02);
            controller.player.applyForce(controller.controller.forwards().mul(2), 0);

//            Vec3d pullDir = web.sub(controller.pos()).normalize();
//            pullDir = pullDir.lerp(controller.controller.forwards(), .2);
//            controller.player.applyForce(pullDir.mul(20), .05);
//
//            double pullStrength = Math.exp(-.02 * pullDir.dot(controller.player.velocity.velocity));
//            controller.player.velocity.velocity = controller.player.velocity.velocity.add(pullDir.mul(pullStrength * dt() * 20));
        }
    }
}
