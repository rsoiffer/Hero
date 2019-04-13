package physics;

import behaviors._3d.PositionBehavior3d;
import behaviors._3d.PreviousPositionBehavior3d;
import behaviors._3d.VelocityBehavior3d;
import engine.Behavior;
import engine.Layer;
import game.World;
import java.util.List;
import util.math.Vec3d;

public class PhysicsBehavior extends Behavior {

    private static final Layer PHYSICS = new Layer(5);

    public final PositionBehavior3d position = require(PositionBehavior3d.class);
    public final PreviousPositionBehavior3d prevPos = require(PreviousPositionBehavior3d.class);
    public final VelocityBehavior3d velocity = require(VelocityBehavior3d.class);

    public double radius = 1;
    public World world;

    public boolean onGround;

    @Override
    public Layer layer() {
        return PHYSICS;
    }

    @Override
    public void step() {
        onGround = false;

        List<Vec3d> l = world.collisionShape.intersect(new SphereShape(position.position, radius));
        for (Vec3d v : l) {
            double vel = velocity.velocity.dot(v);
            if (vel < 0) {
                velocity.velocity = velocity.velocity.sub(v.mul(vel / v.lengthSquared()));
            }
            position.position = position.position.add(v);
            onGround |= v.z > 0;
        }
    }
}
