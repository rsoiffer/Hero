package physics;

import java.util.Arrays;
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
    public boolean intersects(AABB aabb) {
        AABB bounds = AABB.boundingBox(Arrays.asList(pos, pos.add(dir))).expand(radius);
        if (!bounds.intersects(aabb)) {
            return false;
        }
        return CommonPhysics.segmentAABBDistance(pos, pos.add(dir), aabb) < radius;
    }

    @Override
    public boolean intersects(SphereShape sphere) {
        return CommonPhysics.segmentPointDistance(pos, pos.add(dir), sphere.pos) < radius + sphere.radius;
    }

    @Override
    public double raycast(Vec3d start, Vec3d dir2) {
        if (CommonPhysics.segmentSegmentDistance(pos, pos.add(dir), start, start.add(dir2.mul(1000))) > radius) {
            return -1;
        }
        return 0;
    }
}
