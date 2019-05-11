package game.vr;

import engine.Behavior;
import engine.Layer;
import static game.Player.POSTPHYSICS;
import game.RenderableBehavior;
import static game.RenderableBehavior.createRB;
import graphics.models.VoxelModel2;
import graphics.renderables.ColorModel;
import util.math.Quaternion;
import util.math.Transformation;
import util.math.Vec3d;
import static vr.ViveInput.TRIGGER;

public class Teleport extends Behavior {

    public final ControllerBehavior controller = require(ControllerBehavior.class);

    public ColorModel markerModel;
    public RenderableBehavior markerRB;

    @Override
    public void createInner() {
        controller.renderable.renderable = new ColorModel(VoxelModel2.load("controller_yellow.vox"));
        markerModel = new ColorModel(VoxelModel2.load("singlevoxel.vox"));
        markerModel.color = new Vec3d(.2, .6, 1);
        markerRB = createRB(markerModel);
        markerRB.beforeRender = () -> {
            Vec3d newPos = findPos();
            markerRB.visible = newPos != null;
            if (markerRB.visible) {
                markerModel.t = Transformation.create(newPos.sub(.5), Quaternion.IDENTITY, 1);
            }
        };
    }

    @Override
    public void destroyInner() {
        markerRB.destroy();
    }

    public Vec3d findPos() {
        Vec3d pos = controller.pos();
        if (controller.player.physics.wouldCollideAt(pos)) {
            return null;
        }
        Vec3d vel = controller.controller.forwards();
        for (int i = 0; i < 400; i++) {
            Vec3d pos2 = pos.add(vel.mul(.5));
            if (controller.player.physics.wouldCollideAt(pos2)) {
                return pos;
            }
            pos = pos2;
            vel = vel.add(new Vec3d(0, 0, -.01));
        }
        return null;
    }

    @Override
    public Layer layer() {
        return POSTPHYSICS;
    }

    @Override
    public void step() {
        if (controller.controller.buttonJustPressed(TRIGGER)) {
            Vec3d newPos = findPos();
            if (newPos != null) {
                controller.player.position.position = newPos;
                controller.player.velocity.velocity = new Vec3d(0, 0, 0);
            }
        }
    }
}
