package game.vr;

import engine.Behavior;
import static engine.Core.dt;
import engine.Layer;
import static game.Player.POSTPHYSICS;
import graphics.models.VoxelModel2;
import graphics.renderables.ColorModel;
import util.math.Vec3d;
import vr.ViveInput;

public class Wing extends Behavior {

    public final ControllerBehavior controller = require(ControllerBehavior.class);

    public Vec3d prevPos = null;

    @Override
    public void createInner() {
        controller.renderable.renderable = new ColorModel(VoxelModel2.load("controller_wing.vox"));
        if (controller.controller == ViveInput.LEFT) {
            controller.renderable.renderable = new ColorModel(VoxelModel2.load("controller_wing_left.vox"));
        }
        controller.modelOffset = new Vec3d(16, 40, 2);
    }

    @Override
    public Layer layer() {
        return POSTPHYSICS;
    }

    @Override
    public void step() {
        Vec3d sideways = controller.controller.sideways();
        if (controller.controller != ViveInput.LEFT) {
            sideways = sideways.mul(-1);
        }
        Vec3d pos = controller.pos(5).add(sideways.mul(1.5));

        if (prevPos != null) {
            Vec3d motion = pos.sub(prevPos);
            if (motion.lengthSquared() >= 1e-6) {
                Vec3d wingUp = controller.controller.transform(new Vec3d(0, 0, 1));
                double C = -motion.normalize().dot(wingUp);
                if (C < 0) {
                    C *= .5;
                }
                double strength = 10 * C * motion.lengthSquared() / dt();
                controller.player.applyForce(wingUp.mul(strength), 0);
            }
        }
        prevPos = pos;

        if (!controller.player.physics.onGround) {
            double thrustStrength = 2;
            controller.player.applyForce(controller.controller.forwards().mul(thrustStrength), 0.05);
        }
    }
}
