package physics;

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
    public double raycast(Vec3d start, Vec3d dir2) {
        if (CommonPhysics.segmentSegmentDistance(pos, pos.add(dir), start, start.add(dir2.mul(1000))) > radius) {
            return -1;
        }
        return 0;
    }

    @Override
    public Vec3d surfaceClosest(Vec3d point) {
        Vec3d v = CommonPhysics.segmentPointClosest(pos, pos.add(dir), point);
        return v.add(point.sub(v).setLength(radius));
    }
}
