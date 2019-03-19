package game;

import engine.Behavior;
import engine.Layer;
import static engine.Layer.RENDER3D;
import graphics.Camera;
import graphics.Color;
import graphics.voxels.VoxelModel;
import org.joml.Matrix4d;
import util.math.Transformation;
import util.math.Vec3d;
import vr.ViveInput.ViveController;

public class ControllerBehavior extends Behavior {

    private static Vec3d OFFSET = new Vec3d(0, 0, 1.2);

    public ViveController controller;
    public Player player;
    public VoxelModel model = VoxelModel.load("controller.vox");

    @Override
    public Layer layer() {
        return RENDER3D;
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
        model.render(new Transformation(new Matrix4d()
                .translate(Camera.camera3d.position.toJOML())
                .mul(controller.pose())
                .translate(-.125, -.125, -.125)
                .scale(1 / 32.)
        ), Color.WHITE);
    }
}
