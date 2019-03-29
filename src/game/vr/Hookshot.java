package game.vr;

import engine.Behavior;
import static engine.Core.dt;
import engine.Layer;
import static engine.Layer.RENDER3D;
import static game.Player.POSTPHYSICS;
import graphics.voxels.VoxelModel;
import util.math.Transformation;
import util.math.Vec3d;
import util.math.Vec4d;
import static vr.ViveInput.TRIGGER;

public class Hookshot extends Behavior {

    public final ControllerBehavior controller = require(ControllerBehavior.class);

    public Vec3d hookPos, hookVel;
    public boolean grabbing;

    private Behavior renderer;

    @Override
    public void createInner() {
        controller.model = VoxelModel.load("controller_gray.vox");
        renderer = RENDER3D.onStep(() -> {
            if (hookPos != null) {
                Vec3d pos = controller.pos();
                Vec3d forwards = hookPos.sub(pos);
                Vec3d side = forwards.cross(new Vec3d(0, 0, 1)).setLength(.05);
                Vec3d up = forwards.cross(side).setLength(.05);;
                Vec3d pos2 = pos.sub(side.div(2)).sub(up.div(2));
                VoxelModel.load("singlevoxel.vox").render(Transformation.create(pos2, forwards, side, up), new Vec4d(.5, .5, .5, 1));
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
            hookPos = controller.pos();
            hookVel = controller.controller.forwards().mul(100);
        }
        if (controller.controller.buttonJustReleased(TRIGGER)) {
            hookPos = null;
            grabbing = false;
        }
        if (hookPos != null) {
            if (!grabbing) {
                hookPos = hookPos.add(hookVel.mul(dt()));
                grabbing = controller.player.physics.world.buildings.stream().anyMatch(b -> b.contains(hookPos));
            } else {
                Vec3d pullDir = hookPos.sub(controller.pos()).normalize();
                pullDir = pullDir.lerp(controller.controller.forwards(), .2);
                controller.player.velocity.velocity = controller.player.velocity.velocity.lerp(
                        pullDir.mul(40), 1 - Math.exp(-1 * dt()));
            }
        }
    }
}
