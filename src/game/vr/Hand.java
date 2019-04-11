package game.vr;

import engine.Behavior;
import engine.Layer;
import static game.Player.POSTPHYSICS;
import game.RenderableBehavior;
import static game.RenderableBehavior.createRB;
import graphics.models.VoxelModel2;
import graphics.renderables.ColorModel;
import util.math.Transformation;
import util.math.Vec3d;
import vr.EyeCamera;
import static vr.ViveInput.TRIGGER;

public class Hand extends Behavior {

    public final ControllerBehavior controller = require(ControllerBehavior.class);

    public Vec3d handPos;

    public ColorModel armModel;
    public RenderableBehavior armRB;

    @Override
    public void createInner() {
        controller.renderable.renderable = new ColorModel(VoxelModel2.load("controller_green.vox"));
        armModel = new ColorModel(VoxelModel2.load("singlevoxel.vox"));
        armModel.color = new Vec3d(.5, 1, .4);
        armRB = createRB(armModel);
        armRB.beforeRender = () -> {
            Vec3d v = handPos;
            if (v == null) {
                Vec3d start = controller.pos();
                Vec3d dir = controller.controller.forwards();
                double t = controller.player.physics.world.buildings.stream().mapToDouble(a -> a.raycast(start, dir))
                        .filter(d -> d >= 0).min().orElse(-1);
                if (t != -1 && t <= 8) {
                    v = start.add(dir.mul(t));
                }
            }

            armRB.visible = v != null;
            if (armRB.visible) {
                Vec3d pos = controller.pos();
                Vec3d forwards = v.sub(pos);
                Vec3d side = forwards.cross(new Vec3d(0, 0, 1)).setLength(.05);
                Vec3d up = forwards.cross(side).setLength(.05);;
                Vec3d pos2 = pos.sub(side.div(2)).sub(up.div(2));
                armModel.t = Transformation.create(pos2, forwards, side, up);
            }
        };
    }

    @Override
    public void destroyInner() {
        armRB.destroy();
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
            if (t == -1 || t > 8) {
                handPos = null;
            } else {
                handPos = start.add(dir.mul(t));
            }
        }
        if (controller.controller.buttonJustReleased(TRIGGER) && handPos != null) {
            handPos = null;
            controller.player.velocity.velocity = EyeCamera.headTransform(new Vec3d(1, 0, .5)).mul(25);
        }
        if (handPos != null) {
            Vec3d dir = handPos.sub(controller.player.position.position).normalize();
            controller.player.velocity.velocity = dir.mul(30);
        } else if (!controller.player.physics.onGround) {
            controller.player.applyForce(EyeCamera.headTransform(new Vec3d(1, 0, 0)).mul(3), .05);
        }
    }
}
