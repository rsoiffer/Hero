package game;

import behaviors._3d.AccelerationBehavior3d;
import behaviors._3d.PositionBehavior3d;
import behaviors._3d.VelocityBehavior3d;
import engine.Behavior;
import static engine.Core.dt;
import engine.Layer;
import graphics.Camera;
import physics.AABB;
import physics.PhysicsBehavior;
import util.math.Vec3d;

public class Player extends Behavior {

    public static final Layer POSTPHYSICS = new Layer(6);

    public final PositionBehavior3d position = require(PositionBehavior3d.class);
    public final VelocityBehavior3d velocity = require(VelocityBehavior3d.class);
    public final AccelerationBehavior3d acceleration = require(AccelerationBehavior3d.class);
    public final PhysicsBehavior physics = require(PhysicsBehavior.class);

    public Vec3d cameraOffset = new Vec3d(0, 0, .8);

    public void applyForce(Vec3d force, double dampening) {
        if (velocity.velocity.lengthSquared() >= 1e-6 && force.lengthSquared() >= 1e-6) {
            Vec3d v = velocity.velocity.normalize();
            Vec3d forceAlongVelocity = v.mul(v.dot(force));
            double multiplier = -1 + Math.exp(-dampening * velocity.velocity.dot(force.normalize()));
            force = force.add(forceAlongVelocity.mul(multiplier));
        }
        velocity.velocity = velocity.velocity.add(force.mul(dt()));
    }

    public void applyForce2(Vec3d force, double dampening) {
        if (velocity.velocity.lengthSquared() >= 1e-6 && force.lengthSquared() >= 1e-6) {
            Vec3d v = velocity.velocity.normalize();
            Vec3d forceAlongVelocity = v.mul(v.dot(force));
            double multiplier = -1 + Math.log(1 + Math.exp(-dampening * velocity.velocity.dot(force.normalize())));
            force = force.add(forceAlongVelocity.mul(multiplier));
        }
        velocity.velocity = velocity.velocity.add(force.mul(dt()));
    }

    @Override
    public void createInner() {
        acceleration.acceleration = new Vec3d(0, 0, -10);
        physics.hitbox = new AABB(new Vec3d(-1, -1, -1), new Vec3d(1, 1, 1));
    }

    @Override
    public Layer layer() {
        return POSTPHYSICS;
    }

    @Override
    public void step() {
        if (cameraOffset != null) {
            Camera.camera3d.position = position.position.add(cameraOffset);
        }
        double friction = physics.onGround ? 2 : .01;
        velocity.velocity = velocity.velocity.mul(Math.exp(-dt() * friction));
    }
}
