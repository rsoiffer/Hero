package physics.shapes;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import util.math.Vec3d;

public abstract class CollisionShape {

    public abstract AABB boundingBox();

    public abstract boolean contains(Vec3d point);

    public List<Vec3d> intersect(SphereShape sphere) {
        Vec3d pos = surfaceClosest(sphere.pos);
        if (pos == null) {
            return Arrays.asList();
        }
        boolean inside = contains(sphere.pos);
        double d = sphere.pos.sub(pos).length();

        if (!inside && d >= sphere.radius) {
            return Arrays.asList();
        }
        Vec3d v = pos.sub(sphere.pos).setLength(d + sphere.radius * (inside ? 1 : -1));
        return Arrays.asList(v);
    }

    public abstract OptionalDouble raycast(Vec3d start, Vec3d dir);

    public abstract Vec3d surfaceClosest(Vec3d point);
}
