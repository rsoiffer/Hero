package physics;

import java.util.OptionalDouble;
import util.math.Vec3d;

public class CapsuleShape implements CollisionShape {

    public final Vec3d pos, dir;
    public final double radius;

    public CapsuleShape(Vec3d pos, Vec3d dir, double radius) {
        this.pos = pos;
        this.dir = dir;
        this.radius = radius;
    }

    @Override
    public boolean contains(Vec3d point) {
        return CommonPhysics.segmentPointDistance(pos, pos.add(dir), point) < radius;
    }

    @Override
    public OptionalDouble raycast(Vec3d start, Vec3d dir2) {
        if (CommonPhysics.segmentSegmentDistance(pos, pos.add(dir), start, start.add(dir2.mul(1000))) > radius) {
            return OptionalDouble.empty();
        }
        double t = 0;
        for (int i = 0; i < 10; i++) {
            Vec3d v = surfaceClosest(start.add(dir2.mul(t)));
            t += v.sub(start.add(dir2.mul(t))).length() / dir2.length();
        }
        return OptionalDouble.of(t);
    }

    @Override
    public Vec3d surfaceClosest(Vec3d point) {
        Vec3d v = CommonPhysics.segmentPointClosest(pos, pos.add(dir), point);
        return v.add(point.sub(v).setLength(radius));
    }
}
