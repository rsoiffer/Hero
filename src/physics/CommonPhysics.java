package physics;

import java.util.OptionalDouble;
import static util.math.MathUtils.clamp;
import static util.math.MathUtils.max;
import util.math.Vec3d;

public abstract class CommonPhysics {

    public static double linePointDistance(Vec3d p1, Vec3d p2, Vec3d v) {
        p2 = p2.sub(p1);
        v = v.sub(p1);
        double y = p2.dot(v) / p2.lengthSquared();
        return v.sub(p2.mul(y)).length();
    }

    public static double segmentAABBDistance(Vec3d p1, Vec3d p2, AABB aabb) {
        OptionalDouble t = aabb.raycast(p1, p2.sub(p1));
        if (t.isPresent() && t.getAsDouble() < 1) {
            return 0;
        }
        double minDist = Double.MAX_VALUE;
        for (int x0 = 0; x0 <= 1; x0++) {
            for (int y0 = 0; y0 <= 1; y0++) {
                for (int z0 = 0; z0 <= 1; z0++) {
                    for (int x1 = x0; x1 <= 1; x1++) {
                        for (int y1 = y0; y1 <= 1; y1++) {
                            for (int z1 = z0; z1 <= 1; z1++) {
                                if (x1 + y1 + z1 == x0 + y0 + z0 + 1) {
                                    Vec3d p3 = aabb.lower.lerp(aabb.upper, new Vec3d(x0, y0, z0));
                                    Vec3d p4 = aabb.lower.lerp(aabb.upper, new Vec3d(x1, y1, z1));
                                    minDist = Math.min(minDist, segmentSegmentDistance(p1, p2, p3, p4));
                                }
                            }
                        }
                    }
                }
            }
        }
        return minDist;
    }

    public static Vec3d segmentPointClosest(Vec3d p1, Vec3d p2, Vec3d v) {
        Vec3d dir = p2.sub(p1);
        double t = dir.dot(v.sub(p1)) / dir.lengthSquared();
        return p1.lerp(p2, clamp(t, 0, 1));
    }

    public static double segmentPointDistance(Vec3d p1, Vec3d p2, Vec3d v) {
        return segmentPointClosest(p1, p2, v).sub(v).length();
//        p2 = p2.sub(p1);
//        v = v.sub(p1);
//        double y = p2.dot(v) / p2.lengthSquared();
//        double x = v.sub(p2.mul(y)).length();
//        y = clamp(0, y - 1, y);
//        return Math.sqrt(x * x + y * y * p2.lengthSquared());
    }

    public static double segmentSegmentDistance(Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4) {
        Vec3d dir1 = p2.sub(p1), dir2 = p4.sub(p3), diff = p1.sub(p3);
        double a1 = -dir1.dot(diff), a2 = dir2.dot(diff);
        double b = dir1.lengthSquared(), c = -dir1.dot(dir2), d = dir2.lengthSquared();
        if (Math.abs(b * d - c * c) < 1e-6) {
            double x1 = p1.dot(dir1), x2 = p2.dot(dir1), x3 = p3.dot(dir1), x4 = p4.dot(dir1);
            double x = max(Math.min(x1, x2) - Math.max(x3, x4), Math.min(x3, x4) - Math.max(x1, x2), 0);
            double y = diff.sub(dir1.mul((x3 - x1) / dir1.lengthSquared())).lengthSquared();
            return Math.sqrt(x * x / dir1.lengthSquared() + y);
        }
        double t1 = (a1 * d - a2 * c) / (b * d - c * c);
        double t2 = (a2 * b - a1 * c) / (b * d - c * c);
        return p1.lerp(p2, clamp(t1, 0, 1)).sub(p3.lerp(p4, clamp(t2, 0, 1))).length();
    }
}
