package game.vr;

import engine.Behavior;
import engine.Layer;
import static engine.Layer.POSTUPDATE;
import game.Player;
import game.RenderableBehavior;
import graphics.Camera;
import graphics.models.VoxelModel2;
import graphics.renderables.ColorModel;
import org.joml.Matrix4d;
import util.math.Transformation;
import util.math.Vec3d;
import vr.ViveInput.ViveController;

public class ControllerBehavior extends Behavior {

    private static final Vec3d OFFSET = new Vec3d(0, 0, 1.2);

    public final RenderableBehavior renderable = require(RenderableBehavior.class);

    public ViveController controller;
    public Player player;
    public VoxelModel2 model = VoxelModel2.load("controller.vox");

    @Override
    public void createInner() {
        renderable.renderable = new ColorModel(model);
    }

    public Transformation getTransform() {
        return new Transformation(new Matrix4d()
                .translate(Camera.camera3d.position.toJOML())
                .mul(controller.pose())
                .translate(-.125, -.125, -.125)
                .scale(1 / 32.));
    }

    @Override
    public Layer layer() {
        return POSTUPDATE;
    }

    public Vec3d pos() {
        return Camera.camera3d.position.add(controller.position());
    }

    public Vec3d pos(double exaggeration) {
        return Camera.camera3d.position.add(controller.position()
                .sub(OFFSET).mul(exaggeration).add(OFFSET));
    }

    @Override
    public void step() {
        ((ColorModel) renderable.renderable).t = getTransform();
    }
}
