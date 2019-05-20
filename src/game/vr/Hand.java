package game.vr;

import engine.Behavior;
import static engine.Core.dt;
import engine.Layer;
import static game.Player.POSTPHYSICS;
import game.RenderableBehavior;
import static game.RenderableBehavior.createRB;
import graphics.models.VoxelModel2;
import graphics.renderables.ColorModel;
import java.util.OptionalDouble;
import util.math.Transformation;
import util.math.Vec3d;
import vr.EyeCamera;
import static vr.Vive.TRIGGER;

public class Hand extends Behavior {

    private static double jumpTimer = 0;

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
                Vec3d dir = controller.forwards();
//                double t = controller.player.physics.world.buildings.stream().mapToDouble(a -> a.raycast(start, dir))
//                        .filter(d -> d >= 0).min().orElse(-1);
                OptionalDouble t = controller.player.physics.world.collisionShape.raycast(start, dir);
                if (t.isPresent() && t.getAsDouble() <= 8) {
                    v = start.add(dir.mul(t.getAsDouble()));
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
            Vec3d dir = controller.forwards();
            OptionalDouble t = controller.player.physics.world.collisionShape.raycast(start, dir);
            if (t.isPresent() && t.getAsDouble() <= 8) {
                handPos = start.add(dir.mul(t.getAsDouble()));
            } else {
                handPos = null;
            }
        }
        if (controller.controller.buttonJustReleased(TRIGGER) && handPos != null) {
            handPos = null;
            if (jumpTimer > 0) {
                controller.player.physics.velocity = EyeCamera.headPose().applyRotation(new Vec3d(1, 0, .5)).mul(25);
                jumpTimer = 0;
            } else {
                jumpTimer = .2;
            }
        }
        if (handPos != null) {
            jumpTimer -= dt();
            Vec3d dir = handPos.sub(controller.player.pose.position).normalize();
            controller.player.physics.velocity = controller.player.physics.velocity
                    .lerp(dir.mul(20), 1 - Math.pow(1e-6, dt()));
        } else if (!controller.player.physics.onGround) {
            controller.player.physics.applyForce(EyeCamera.headPose().applyRotation(new Vec3d(1, 0, 0)).mul(300),
                    controller.player.physics.centerOfMass.get());
        }
    }
}
