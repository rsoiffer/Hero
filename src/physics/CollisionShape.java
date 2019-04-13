package physics;

import java.util.Arrays;
import java.util.List;
import util.math.Vec3d;

public interface CollisionShape {

    public boolean contains(Vec3d point);

    public default List<Vec3d> intersect(SphereShape sphere) {
        Vec3d pos = surfaceClosest(sphere.pos);
        boolean inside = contains(sphere.pos);
        double d = sphere.pos.sub(pos).length();

        if (!inside && d >= sphere.radius) {
            return Arrays.asList();
        }
        Vec3d v = pos.sub(sphere.pos).setLength(d + sphere.radius * (inside ? 1 : -1));
        return Arrays.asList(v);
    }

    public double raycast(Vec3d start, Vec3d dir);

    public Vec3d surfaceClosest(Vec3d point);
}
