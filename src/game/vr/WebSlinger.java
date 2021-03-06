package game.vr;

import engine.Behavior;
import engine.Layer;
import static game.Player.POSTPHYSICS;
import game.RenderableBehavior;
import static game.RenderableBehavior.createRB;
import graphics.models.VoxelModel2;
import graphics.renderables.ColorModel;
import java.util.OptionalDouble;
import util.math.Transformation;
import util.math.Vec3d;
import static vr.Vive.TRIGGER;

public class WebSlinger extends Behavior {

    public final ControllerBehavior controller = require(ControllerBehavior.class);

    public Vec3d web;
    public double prefLength;

    public ColorModel webModel;
    public RenderableBehavior webRB;

    @Override
    public void createInner() {
        controller.renderable.renderable = new ColorModel(VoxelModel2.load("controller.vox"));
        webModel = new ColorModel(VoxelModel2.load("singlevoxel.vox"));
        webRB = createRB(webModel);
        webRB.beforeRender = () -> {
            webRB.visible = web != null;
            if (webRB.visible) {
                Vec3d pos = controller.pos();
                Vec3d forwards = web.sub(pos);
                Vec3d side = forwards.cross(new Vec3d(0, 0, 1)).setLength(.05);
                Vec3d up = forwards.cross(side).setLength(.05);;
                Vec3d pos2 = pos.sub(side.div(2)).sub(up.div(2));
                webModel.t = Transformation.create(pos2, forwards, side, up);
            }
        };
    }

    @Override
    public void destroyInner() {
        webRB.destroy();
    }

    @Override
    public Layer layer() {
        return POSTPHYSICS;
    }

    @Override
    public void step() {
        if (controller.controller.buttonJustPressed(TRIGGER)) {
            Vec3d start = controller.pos();
            Vec3d dir = controller.forwards();
            OptionalDouble t = controller.player.physics.world.collisionShape.raycast(start, dir);
            if (t.isPresent()) {
                web = start.add(dir.mul(t.getAsDouble()));
                prefLength = t.getAsDouble() - 4;
            } else {
                web = null;
            }
        }
        if (controller.controller.buttonJustReleased(TRIGGER)) {
            web = null;
        }
        if (web != null) {
            double exag = 10;
            prefLength = Math.min(prefLength, web.sub(controller.pos(exag)).length() - controller.controller.trigger());
            Vec3d pullDir = web.sub(controller.pos(exag)).normalize();
            double strength = 1000 * Math.max(controller.pos(exag).sub(web).length() - prefLength, 0);
            controller.player.physics.applyForce(pullDir.mul(strength), controller.pos());

            double thrustStrength = 200;
            controller.player.physics.applyForce(controller.forwards().mul(thrustStrength), controller.pos());

//            Vec3d pullDir = web.sub(controller.pos()).normalize();
//            pullDir = pullDir.lerp(controller.controller.forwards(), .2);
//            controller.player.applyForce(pullDir.mul(20), .05);
//
//            double pullStrength = Math.exp(-.02 * pullDir.dot(controller.player.velocity.velocity));
//            controller.player.velocity.velocity = controller.player.velocity.velocity.add(pullDir.mul(pullStrength * dt() * 20));
        }
    }
}
