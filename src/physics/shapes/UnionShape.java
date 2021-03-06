package physics.shapes;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import util.math.Vec3d;

public class UnionShape extends CollisionShape {

    private final Collection<CollisionShape> shapes;

    public UnionShape(Collection<CollisionShape> shapes) {
        this.shapes = shapes;
    }

    @Override
    public AABB boundingBox() {
        return null;
    }

    @Override
    public boolean contains(Vec3d point) {
        return shapes.stream().anyMatch(s -> s.contains(point));
    }

    @Override
    public List<Vec3d> intersect(SphereShape sphere) {
        return shapes.stream().flatMap(s -> s.intersect(sphere).stream()).collect(Collectors.toList());
    }

    @Override
    public OptionalDouble raycast(Vec3d start, Vec3d dir) {
        return shapes.stream().map(s -> s.raycast(start, dir))
                .filter(t -> t.isPresent()).mapToDouble(t -> t.getAsDouble()).min();
    }

    @Override
    public Vec3d surfaceClosest(Vec3d point) {
        return shapes.stream().map(s -> s.surfaceClosest(point)).filter(p -> p != null)
                .min(Comparator.comparingDouble(v -> v.sub(point).lengthSquared())).get();
    }
}
