package physics;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import util.math.Vec3d;

public class MultigridShape extends CollisionShape {

    private static final int[] SIZES = {4, 16, 64, 256, 1024};

    private final List<CollisionShape> globals = new LinkedList();
    private final Map<Vec3d, List<CollisionShape>>[] layers;

    public MultigridShape(Iterable<CollisionShape> shapes) {
        layers = new Map[SIZES.length];
        for (int i = 0; i < SIZES.length; i++) {
            layers[i] = new HashMap();
        }
        for (CollisionShape s : shapes) {
            AABB bounds = s.boundingBox();
            if (bounds == null) {
                globals.add(s);
            } else {
                int layer = 0;
                while (layer < SIZES.length - 1) {
                    Vec3d min = bounds.lower.div(SIZES[layer]).floor();
                    Vec3d max = bounds.upper.div(SIZES[layer]).floor().add(1);
                    int num = (int) ((max.x - min.x) * (max.y - min.y) * (max.z - min.z));
                    if (num <= 8) {
                        break;
                    }
                    layer++;
                }
                Vec3d min = bounds.lower.div(SIZES[layer]).floor();
                Vec3d max = bounds.upper.div(SIZES[layer]).floor().add(1);
                for (int x = (int) min.x; x < max.x; x++) {
                    for (int y = (int) min.y; y < max.y; y++) {
                        for (int z = (int) min.z; z < max.z; z++) {
                            Vec3d v = new Vec3d(x, y, z);
                            layers[layer].putIfAbsent(v, new LinkedList());
                            layers[layer].get(v).add(s);
                        }
                    }
                }
            }
        }
    }

    @Override
    public AABB boundingBox() {
        return null;
    }

    @Override
    public boolean contains(Vec3d point) {
        for (int i = 0; i < layers.length; i++) {
            Vec3d v = point.div(SIZES[i]).floor();
            List<CollisionShape> l = layers[i].get(v);
            if (l != null && l.stream().anyMatch(s -> s.contains(point))) {
                return true;
            }
        }
        return globals.stream().anyMatch(s -> s.contains(point));
    }

    @Override
    public List<Vec3d> intersect(SphereShape sphere) {
        List<Vec3d> r = new LinkedList();
        for (int i = 0; i < layers.length; i++) {
            Vec3d min = sphere.boundingBox().lower.div(SIZES[i]).floor();
            Vec3d max = sphere.boundingBox().upper.div(SIZES[i]).floor().add(1);
            for (int x = (int) min.x; x < max.x; x++) {
                for (int y = (int) min.y; y < max.y; y++) {
                    for (int z = (int) min.z; z < max.z; z++) {
                        Vec3d v = new Vec3d(x, y, z);
                        List<CollisionShape> l = layers[i].get(v);
                        if (l != null) {
                            for (CollisionShape s : l) {
                                r.addAll(s.intersect(sphere));
                            }
                        }
                    }
                }
            }
        }
        for (CollisionShape s : globals) {
            r.addAll(s.intersect(sphere));
        }
        return r;
    }

    @Override
    public OptionalDouble raycast(Vec3d start, Vec3d dir) {
        return OptionalDouble.empty();
    }

    @Override
    public Vec3d surfaceClosest(Vec3d point) {
        return null;
    }
}
