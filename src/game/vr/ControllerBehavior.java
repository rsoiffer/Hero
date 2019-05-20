package game.vr;

import engine.Behavior;
import engine.Layer;
import static engine.Layer.POSTUPDATE;
import game.Player;
import game.RenderableBehavior;
import graphics.renderables.ColorModel;
import util.math.Transformation;
import util.math.Vec3d;
import vr.ViveController;

public class ControllerBehavior extends Behavior {

    private static final Vec3d OFFSET = new Vec3d(0, 0, 1.2);

    public final RenderableBehavior renderable = require(RenderableBehavior.class);

    public ViveController controller;
    public Player player;
    public Vec3d modelOffset = new Vec3d(4, 4, 4);

    public Transformation controllerPose() {
        return controller.pose();
    }

    public Transformation controllerPose(double exaggeration) {
        return vrCoords()
                .translate(OFFSET.mul(-1))
                .scale(exaggeration)
                .translate(OFFSET)
                .mul(controller.poseRaw());
    }

    public Vec3d forwards() {
        return controllerPose().applyRotation(new Vec3d(1, 0, 0));
    }

    public Transformation getTransform() {
        return controllerPose().scale(1 / 32.).translate(modelOffset.mul(-1));
    }

    @Override
    public Layer layer() {
        return POSTUPDATE;
    }

    public Vec3d pos() {
        return controllerPose().position();
    }

    public Vec3d pos(double exaggeration) {
        return controllerPose(exaggeration).position();
    }

    public Vec3d sideways() {
        return controllerPose().applyRotation(new Vec3d(0, 1, 0));
    }

    @Override
    public void step() {
        ((ColorModel) renderable.renderable).t = getTransform();
    }

    public Vec3d upwards() {
        return controllerPose().applyRotation(new Vec3d(0, 0, 1));
    }

    public Transformation vrCoords() {
        return player.pose.getTransform().translate(new Vec3d(0, 0, -1));
    }
}
