package physics;

import engine.Behavior;
import static engine.Core.dt;
import engine.Layer;
import game.World;
import java.util.List;
import java.util.function.Supplier;
import physics.shapes.SphereShape;
import util.math.Quaternion;
import util.math.Vec3d;

public class PhysicsBehavior extends Behavior {

    private static final Layer PHYSICS = new Layer(5);

    public final PoseBehavior pose = require(PoseBehavior.class);

    public Vec3d velocity = new Vec3d(0, 0, 0);
    public Vec3d acceleration = new Vec3d(0, 0, 0);
    public Vec3d rotationalVelocity = new Vec3d(0, 0, 0);

    public boolean allowRotation;
    public Supplier<Vec3d> centerOfMass = () -> pose.position;

    public double radius = 1;
    public double mass = 100;
    public double rotationalInertia = 500;
    public double drag = .1; // Terminal velocity of 100 m/s
    public double rotationalDrag = 2000;
    public double reorient = 1000;

    public World world;
    public boolean onGround;

    public void applyForce(Vec3d force, Vec3d pos) {
        applyImpulse(force.mul(dt()), pos);
    }

    public void applyImpulse(Vec3d impulse, Vec3d pos) {
        velocity = velocity.add(impulse.div(mass));
        applyTorqueImpulse(pos.sub(centerOfMass.get()).cross(impulse));
    }

    public void applyTorque(Vec3d torque) {
        applyTorqueImpulse(torque.mul(dt()));
    }

    public void applyTorqueImpulse(Vec3d torqueImpulse) {
        if (allowRotation) {
            rotationalVelocity = rotationalVelocity.add(torqueImpulse.div(rotationalInertia));
        }
    }

    private List<Vec3d> collide(Vec3d pos) {
        return world.collisionShape.intersect(new SphereShape(pos, radius));
    }

    private double findFirstCollision(Vec3d delta) {
        double t = 0;
        double step = .5;
        for (int i = 0; i < 10; i++) {
            if (!wouldCollideAt(pose.position.add(delta.mul(t + step)))) {
                t += step;
            }
            step *= .5;
        }
        return t;
    }

    @Override
    public Layer layer() {
        return PHYSICS;
    }

    private void projectVelocityAgainst(List<Vec3d> l) {
        Vec3d sum = l.stream().reduce(new Vec3d(0, 0, 0), Vec3d::add);
        if (velocity.dot(sum) < 0) {
            velocity = velocity.projectAgainst(sum);
        }
        if (sum.z > 0) {
            onGround = true;
        }
    }

    @Override
    public void step() {
        onGround = false;
        velocity = velocity.add(acceleration.mul(dt()));
        if (allowRotation) {
            pose.rotate(Quaternion.fromAngleAxis(rotationalVelocity.mul(dt())));
        }

        double airResistanceForce = drag * velocity.lengthSquared();
        if (airResistanceForce > 1e-12) {
            applyForce(velocity.setLength(-airResistanceForce), pose.position);
        }
        double airResistanceTorque = rotationalDrag * (rotationalVelocity.length() + rotationalVelocity.lengthSquared());
        if (airResistanceTorque > 1e-12) {
            applyTorque(rotationalVelocity.setLength(-airResistanceTorque));
        }
        double reorientTorque = reorient * pose.rotation.applyTo(new Vec3d(0, 0, 1)).setZ(0).length();
        if (reorientTorque > 1e-12) {
            applyTorque(pose.rotation.applyTo(new Vec3d(0, 0, 1)).cross(new Vec3d(0, 0, 1)).setLength(reorientTorque));
        }

        Vec3d newPos = pose.position.add(velocity.mul(dt()));
        if (!wouldCollideAt(newPos)) {
            pose.position = newPos;
        } else {
            double t2 = 1;
            for (int i = 0; i < 4; i++) {
                double t = findFirstCollision(velocity.mul(t2 * dt()));
                pose.translate(velocity.mul(t * t2 * dt()));
                projectVelocityAgainst(collide(pose.position.add(velocity.mul(1e-3 * t2 * dt()))));
                t2 *= 1 - t;
            }
        }
    }

    public boolean wouldCollideAt(Vec3d pos) {
        return !collide(pos).isEmpty();
    }
}
