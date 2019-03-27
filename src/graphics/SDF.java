package graphics;

import java.util.stream.Stream;
import util.math.Vec3d;

public interface SDF {

    public double value(Vec3d v);

    public static SDF cylinder(Vec3d pos, Vec3d dir, double radius) {
        Vec3d dir2 = dir.normalize();
        return v -> radius - v.sub(pos).sub(dir2.mul(v.sub(pos).dot(dir2))).length();
    }

    public static SDF cylinder(Vec3d pos, Vec3d dir, double radius, double height) {
        return intersection(cylinder(pos, dir, radius), halfSpace(pos.add(dir.mul(-height)), dir), halfSpace(pos.add(dir.mul(height)), dir.mul(-1)));
    }

    public static SDF halfSpace(Vec3d pos, Vec3d dir) {
        Vec3d dir2 = dir.normalize();
        return v -> v.sub(pos).dot(dir2);
    }

    public static SDF intersection(SDF... a) {
        return v -> Stream.of(a).mapToDouble(sdf -> sdf.value(v)).min().getAsDouble();
    }

    public default SDF invert() {
        return v -> -value(v);
    }

    public default SDF scale(double scale) {
        return v -> scale * value(v.mul(scale));
    }

    public static SDF sphere(Vec3d pos, double size) {
        return v -> size - v.sub(pos).length();
    }

    public default SDF translate(Vec3d t) {
        return v -> value(v.sub(t));
    }

    public static SDF union(SDF... a) {
        return v -> Stream.of(a).mapToDouble(sdf -> sdf.value(v)).max().getAsDouble();
    }
}
