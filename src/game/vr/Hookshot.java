package game.vr;

import engine.Behavior;
import static engine.Core.dt;
import engine.Layer;
import static game.Player.POSTPHYSICS;
import game.RenderableBehavior;
import static game.RenderableBehavior.createRB;
import graphics.models.VoxelModel2;
import graphics.renderables.ColorModel;
import util.math.Transformation;
import util.math.Vec3d;
import static vr.ViveInput.TRIGGER;

public class Hookshot extends Behavior {

    public final ControllerBehavior controller = require(ControllerBehavior.class);

    public Vec3d hookPos, hookVel;
    public boolean grabbing;
    public ColorModel lineModel;
    public RenderableBehavior lineRB;

    @Override
    public void createInner() {
        controller.renderable.renderable = new ColorModel(VoxelModel2.load("controller_gray.vox"));
        lineModel = new ColorModel(VoxelModel2.load("singlevoxel.vox"));
        lineModel.color = new Vec3d(.5, .5, .5);
        lineRB = createRB(lineModel);
        lineRB.beforeRender = () -> {
            lineRB.visible = hookPos != null;
            if (lineRB.visible) {
                Vec3d pos = controller.pos();
                Vec3d forwards = hookPos.sub(pos);
                Vec3d side = forwards.cross(new Vec3d(0, 0, 1)).setLength(.05);
                Vec3d up = forwards.cross(side).setLength(.05);;
                Vec3d pos2 = pos.sub(side.div(2)).sub(up.div(2));
                lineModel.t = Transformation.create(pos2, forwards, side, up);
            }
        };
    }

    @Override
    public void destroyInner() {
        lineRB.destroy();
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
