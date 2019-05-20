package physics.shapes;

import graphics.models.SurfaceNet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.stream.Collectors;
import static util.math.MathUtils.ceil;
import static util.math.MathUtils.floor;
import util.math.Vec3d;

public class SurfaceNetShape extends CollisionShape {

    private static final int RADIUS = 4;

    public final SurfaceNet surfaceNet;

    public SurfaceNetShape(SurfaceNet surfaceNet) {
        this.surfaceNet = surfaceNet;
    }

    @Override
    public AABB boundingBox() {
        return null;
    }

    @Override
    public boolean contains(Vec3d point) {
        return surfaceNet.getInterp(point) > 0;
    }

//    @Override
//    public List<Vec3d> intersect(SphereShape sphere) {
//        List<Vec3d> r = new LinkedList();
//        boolean inside = contains(sphere.pos);
//        for (Vec3d pos : surface(sphere.pos)) {
//            double d = sphere.pos.sub(pos).length();
//            if (inside || d < sphere.radius) {
//                Vec3d v = pos.sub(sphere.pos).setLength(d + sphere.radius * (inside ? 1 : -1));
//                r.add(v);
//            }
//        }
//        return r;
//    }
    @Override
    public OptionalDouble raycast(Vec3d start, Vec3d dir) {
        double t = 0;
        for (int i = 0; i < 50; i++) {
            if (contains(start.add(dir.mul(t)))) {
                return OptionalDouble.of(t);
            }
            Vec3d v = surfaceClosest(start.add(dir.mul(t)));
            if (v == null) {
                t += RADIUS / dir.length();
            } else {
                t += v.sub(start.add(dir.mul(t))).length() / dir.length();
            }
        }
        Vec3d pos = start.add(dir.mul(t));
        Vec3d closest = surfaceClosest(pos);
        if (closest != null && closest.sub(pos).lengthSquared() < .5) {
            return OptionalDouble.of(t);
        }
        return OptionalDouble.empty();
    }

    private List<Vec3d> surface(Vec3d point) {
        return surfaceNet.crossingsNear(point, 4).collect(Collectors.toList());
    }

    @Override
    public Vec3d surfaceClosest(Vec3d point) {
//        return surfaceNet.crossingsNear(point, 4)
//                .min(Comparator.comparingDouble(v -> v.sub(point).lengthSquared())).orElse(null);
        point = point.div(surfaceNet.scale);
        int xMin = floor(point.x) - RADIUS, yMin = floor(point.y) - RADIUS, zMin = floor(point.z) - RADIUS;
        int xMax = ceil(point.x) + RADIUS, yMax = ceil(point.y) + RADIUS, zMax = ceil(point.z) + RADIUS;
        Vec3d closest = null;
        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int z = zMin; z <= zMax; z++) {
                    double d = surfaceNet.get(x, y, z);
                    if (d > 0) {
                        Vec3d v = new Vec3d(x, y, z);
                        v = v.add(point.sub(v).setLength(d));
                        if (closest == null || closest.sub(point).lengthSquared() > v.sub(point).lengthSquared()) {
                            closest = v;
                        }
                    }
                }
            }
        }
        if (closest != null) {
//            double d = surfaceNet.get((int) closest.x, (int) closest.y, (int) closest.z);
//            closest = closest.add(point.sub(closest).setLength(d));
            return closest.mul(surfaceNet.scale);
        }
        return null;
//        TreeMap<Double, Vec3d> closest = new TreeMap<>();
//        for (int x = xMin; x <= xMax; x++) {
//            for (int y = yMin; y <= yMax; y++) {
//                for (int z = zMin; z <= zMax; z++) {
//                    double d = surfaceNet.get(x, y, z);
//                    if (d > 0) {
//                        Vec3d v = new Vec3d(x, y, z);
//                        v = v.add(point.sub(v).setLength(d));
//                        double sqlen = v.sub(point).lengthSquared();
//                        closest.put(sqlen, v);
//                    }
//                }
//            }
//        }
//        if (!closest.isEmpty()) {
//            List<Vec3d> samples = new ArrayList<>();
//            
//            Vec3d avg
//            return avg.mul(surfaceNet.scale);
//        }
//        return null;
    }
}
