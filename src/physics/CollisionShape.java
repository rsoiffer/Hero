package physics;

import java.util.Collection;
import util.math.Vec3d;

public interface CollisionShape {

    public boolean contains(Vec3d point);

    public boolean intersects(AABB aabb);

    public boolean intersects(SphereShape sphere);

    public double raycast(Vec3d start, Vec3d dir);

    public static class UnionShape implements CollisionShape {

        private final Collection<CollisionShape> shapes;

        public UnionShape(Collection<CollisionShape> shapes) {
            this.shapes = shapes;
        }

        @Override
        public boolean contains(Vec3d point) {
            return shapes.stream().anyMatch(s -> s.contains(point));
        }

        @Override
        public boolean intersects(AABB aabb) {
            return shapes.stream().anyMatch(s -> s.intersects(aabb));
        }

        @Override
        public boolean intersects(SphereShape sphere) {
            return shapes.stream().anyMatch(s -> s.intersects(sphere));
        }

        @Override
        public double raycast(Vec3d start, Vec3d dir) {
            return shapes.stream().mapToDouble(a -> a.raycast(start, dir))
                    .filter(d -> d >= 0).min().orElse(-1);
        }
    }
}
