package physics.shapes;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import util.math.Vec3d;

public class AABB extends CollisionShape {

    public final Vec3d lower, upper;

    public AABB(Vec3d lower, Vec3d upper) {
        this.lower = lower;
        this.upper = upper;
    }

    public static AABB boundingBox(List<Vec3d> points) {
        Vec3d lower = points.get(0);
        Vec3d upper = points.get(0);
        for (Vec3d p : points) {
            lower = new Vec3d(Math.min(p.x, lower.x), Math.min(p.y, lower.y), Math.min(p.z, lower.z));
            upper = new Vec3d(Math.max(p.x, upper.x), Math.max(p.y, upper.y), Math.max(p.z, upper.z));
        }
        return new AABB(lower, upper);
    }

    @Override
    public AABB boundingBox() {
        return this;
    }

    public Vec3d center() {
        return lower.lerp(upper, .5);
    }

    @Override
    public boolean contains(Vec3d point) {
        return lower.x < point.x && point.x < upper.x
                && lower.y < point.y && point.y < upper.y
                && lower.z < point.z && point.z < upper.z;
    }

    public AABB expand(double amt) {
        return new AABB(lower.sub(amt), upper.add(amt));
    }

    @Override
    public OptionalDouble raycast(Vec3d start, Vec3d dir) {
        Vec3d timeToLower = lower.sub(start).div(dir);
        Vec3d timeToUpper = upper.sub(start).div(dir);
        DoubleStream times = DoubleStream.of(timeToLower.x, timeToLower.y, timeToLower.z, timeToUpper.x, timeToUpper.y, timeToUpper.z);
        return times.filter(t -> t >= 0
                && (contains(start.add(dir.mul(t + 1e-6))) || contains(start.add(dir.mul(t - 1e-6))))
        ).min();
    }

    public Vec3d size() {
        return upper.sub(lower);
    }

    @Override
    public Vec3d surfaceClosest(Vec3d point) {
        Vec3d pos2 = point.clamp(lower, upper);
        if (!point.equals(pos2)) {
            return pos2;
        }
        return Stream.of(
                new Vec3d(lower.x - pos2.x, 0, 0), new Vec3d(upper.x - pos2.x, 0, 0),
                new Vec3d(0, lower.y - pos2.y, 0), new Vec3d(0, upper.y - pos2.y, 0),
                new Vec3d(0, 0, lower.z - pos2.z), new Vec3d(0, 0, upper.z - pos2.z)
        ).min(Comparator.comparingDouble(v -> v.lengthSquared())).get().add(pos2);
    }

    @Override
    public String toString() {
        return "AABB{" + "lower=" + lower + ", upper=" + upper + '}';
    }

    public AABB translate(Vec3d pos) {
        return new AABB(lower.add(pos), upper.add(pos));
    }
}
