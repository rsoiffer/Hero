package physics;

import java.util.List;
import java.util.stream.DoubleStream;
import util.math.Vec3d;

public class AABB {

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

    public boolean contains(Vec3d point) {
        return lower.x < point.x && point.x < upper.x
                && lower.y < point.y && point.y < upper.y
                && lower.z < point.z && point.z < upper.z;
    }

    public boolean intersects(AABB other) {
        return lower.x < other.upper.x && other.lower.x < upper.x
                && lower.y < other.upper.y && other.lower.y < upper.y
                && lower.z < other.upper.z && other.lower.z < upper.z;
    }

    public double raycast(Vec3d start, Vec3d dir) {
        Vec3d timeToLower = lower.sub(start).div(dir);
        Vec3d timeToUpper = upper.sub(start).div(dir);
        DoubleStream times = DoubleStream.of(timeToLower.x, timeToLower.y, timeToLower.z, timeToUpper.x, timeToUpper.y, timeToUpper.z);
        return times.filter(d -> d >= 0).filter(d -> contains(start.add(dir.mul(d + .001)))).min().orElse(-1);
    }

    public Vec3d size() {
        return upper.sub(lower);
    }

    @Override
    public String toString() {
        return "AABB{" + "lower=" + lower + ", upper=" + upper + '}';
    }

    public AABB translate(Vec3d pos) {
        return new AABB(lower.add(pos), upper.add(pos));
    }
}
