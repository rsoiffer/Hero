package physics;

import java.util.stream.DoubleStream;
import util.math.Vec3d;

public class AABB {

    public final Vec3d lower, upper;

    public AABB(Vec3d lower, Vec3d upper) {
        this.lower = lower;
        this.upper = upper;
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

    public AABB translate(Vec3d pos) {
        return new AABB(lower.add(pos), upper.add(pos));
    }
}
