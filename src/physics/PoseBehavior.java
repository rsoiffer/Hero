package physics;

import engine.Behavior;
import org.joml.Matrix4d;
import util.math.Quaternion;
import util.math.Transformation;
import util.math.Vec3d;

public class PoseBehavior extends Behavior {

    public Vec3d position = new Vec3d(0, 0, 0);
    public Quaternion rotation = Quaternion.IDENTITY;

    public Matrix4d getMatrix() {
        return getTransform().matrix();
    }

    public Transformation getTransform() {
        return Transformation.create(position, rotation, 1);
    }

    public void rotate(Quaternion q) {
        rotation = q.mul(rotation);
    }

    public void translate(Vec3d v) {
        position = position.add(v);
    }
}
