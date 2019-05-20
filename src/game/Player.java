package game;

import engine.Behavior;
import static engine.Core.dt;
import engine.Layer;
import graphics.Camera;
import physics.PhysicsBehavior;
import physics.PoseBehavior;
import util.math.Vec3d;
import vr.EyeCamera;
import vr.Vive;

public class Player extends Behavior {

    public static final Layer POSTPHYSICS = new Layer(6);

    public final PoseBehavior pose = require(PoseBehavior.class);
    public final PhysicsBehavior physics = require(PhysicsBehavior.class);

    public Vec3d cameraOffset = new Vec3d(0, 0, .8);
    public Vec3d prevVelocity = new Vec3d(0, 0, 0);

//    public void applyForce(Vec3d force, double dampening) {
//        if (physics.velocity.lengthSquared() >= 1e-6 && force.lengthSquared() >= 1e-6) {
//            Vec3d v = physics.velocity.normalize();
//            Vec3d forceAlongVelocity = v.mul(v.dot(force));
//            double multiplier = -1 + Math.exp(-dampening * physics.velocity.dot(force.normalize()));
//            force = force.add(forceAlongVelocity.mul(multiplier));
//        }
//        physics.velocity = physics.velocity.add(force.mul(dt()));
//    }
//
//    public void applyForce2(Vec3d force, double dampening) {
//        if (physics.velocity.lengthSquared() >= 1e-6 && force.lengthSquared() >= 1e-6) {
//            Vec3d v = physics.velocity.normalize();
//            Vec3d forceAlongVelocity = v.mul(v.dot(force));
//            double multiplier = -1 + Math.log(1 + Math.exp(-dampening * physics.velocity.dot(force.normalize())));
//            force = force.add(forceAlongVelocity.mul(multiplier));
//        }
//        physics.velocity = physics.velocity.add(force.mul(dt()));
//    }
    @Override
    public void createInner() {
        physics.acceleration = new Vec3d(0, 0, -10);
        physics.allowRotation = true;
        physics.centerOfMass = () -> Vive.footTransform.get().position().lerp(EyeCamera.headPose().position(), .5);
        Vive.footTransform = () -> pose.getTransform().translate(new Vec3d(0, 0, -1));
    }

    @Override
    public Layer layer() {
        return POSTPHYSICS;
    }

    @Override
    public void step() {
        if (cameraOffset != null) {
            Camera.camera3d.position = pose.position.add(cameraOffset);
        }
        double friction = physics.onGround ? 2 : 0;
        physics.velocity = physics.velocity.mul(Math.exp(-dt() * friction));
        if (Vive.running) {
            if (physics.velocity.sub(prevVelocity).lengthSquared() > 50) {
                Vive.LEFT.hapticPulse(5);
                Vive.RIGHT.hapticPulse(5);
            }
        }
        prevVelocity = physics.velocity;
    }
}
