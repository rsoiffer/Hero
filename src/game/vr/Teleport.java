package game.vr;

import engine.Behavior;
import engine.Layer;
import static game.Player.POSTPHYSICS;
import game.RenderableBehavior;
import static game.RenderableBehavior.createRB;
import graphics.Camera;
import graphics.models.VoxelModel2;
import graphics.renderables.ColorModel;
import graphics.renderables.ColorModelParticles;
import graphics.renderables.RenderableList;
import java.util.LinkedList;
import util.math.Quaternion;
import util.math.Transformation;
import util.math.Vec3d;
import static vr.ViveInput.TRIGGER;

public class Teleport extends Behavior {

    public final ControllerBehavior controller = require(ControllerBehavior.class);

    public RenderableBehavior markerRB;

    @Override
    public void createInner() {
        controller.renderable.renderable = new ColorModel(VoxelModel2.load("controller_yellow.vox"));
        ColorModel markerModel = new ColorModel(VoxelModel2.load("singlevoxel.vox"));
        markerModel.color = new Vec3d(.6, .2, .8);
        
        ColorModelParticles arcModel = new ColorModelParticles(VoxelModel2.load("singlevoxel.vox"));
        arcModel.color = new Vec3d(.6, .2, .8);
        
        markerRB = createRB(new RenderableList(markerModel, arcModel));
        markerRB.beforeRender = () -> {
            Vec3d newPos = findPos();
            markerRB.visible = newPos != null;
            if (markerRB.visible) {
                double scale = Math.min(1, newPos.sub(Camera.camera3d.position).length() / 20);
                markerModel.t = Transformation.create(newPos.sub(scale/2), Quaternion.IDENTITY, scale);
                
                arcModel.transforms = new LinkedList();
                Vec3d pos = controller.pos();
                Vec3d vel = controller.controller.forwards();
                for (int i = 0; i < 100; i++) {
                    Vec3d pos2 = pos.add(vel.mul(.5));
                    if (controller.player.physics.wouldCollideAt(pos2)) {
                        break;
                    }
                    Vec3d dir = pos2.sub(pos);                
                    double scale2 = Math.min(1, pos.sub(Camera.camera3d.position).length() / 20) / 4;
                    Vec3d dir1 = dir.cross(new Vec3d(0, 0, 1)).setLength(scale2);
                    Vec3d dir2 = dir1.cross(dir).setLength(scale2);
                    arcModel.transforms.add(Transformation.create(pos.sub(dir1.div(2)).sub(dir2.div(2)), dir, dir1, dir2));
                    pos = pos2;
                    vel = vel.add(new Vec3d(0, 0, -.005));
                }
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
        for (int i = 0; i < 100; i++) {
            Vec3d pos2 = pos.add(vel.mul(.5));
            if (controller.player.physics.wouldCollideAt(pos2)) {
                return pos;
            }
            pos = pos2;
            vel = vel.add(new Vec3d(0, 0, -.005));
        }
        return pos;
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
                // controller.player.velocity.velocity = new Vec3d(0, 0, 0);
            }
        }
    }
}
