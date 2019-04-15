package physics;

import java.util.OptionalDouble;
import util.math.Vec3d;

public class SphereShape extends CollisionShape {

    public final Vec3d pos;
    public final double radius;

    public SphereShape(Vec3d pos, double radius) {
        this.pos = pos;
        this.radius = radius;
    }

    @Override
    public AABB boundingBox() {
        return new AABB(pos.sub(radius), pos.add(radius));
    }

    @Override
    public boolean contains(Vec3d point) {
        return point.sub(pos).lengthSquared() < radius * radius;
    }

    @Override
    public OptionalDouble raycast(Vec3d start, Vec3d dir) {
        Vec3d diff = start.sub(pos);
        double a = dir.lengthSquared();
        double b = 2 * diff.dot(dir);
        double c = diff.lengthSquared() - radius * radius;
        if (b * b - 4 * a * c < 0) {
            return OptionalDouble.empty();
        }
        double t1 = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
        double t2 = (-b - Math.sqrt(b * b - 4 * a * c)) / (2 * a);
        return OptionalDouble.of(Math.max(0, Math.min(t1, t2)));
    }

    @Override
    public Vec3d surfaceClosest(Vec3d point) {
        return pos.add(point.sub(pos).setLength(radius));
    }
}
