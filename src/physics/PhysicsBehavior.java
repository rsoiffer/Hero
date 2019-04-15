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
//        List<Vec3d> l1 = world.collisionShape.intersect(new SphereShape(position.position, radius));
//        if (!l1.isEmpty()) {
//            Vec3d pos = prevPos.prevPos.sub(position.position);
//            Vec3d vel = pos.mul(-1);
//            while (true) {
//                TreeMap<Double, Vec3d> m = new TreeMap();
//                for (Vec3d v : l1) {
//                    double d = vel.dot(v);
//                    if (Math.abs(d) > 1e-12) {
//                        double t = v.sub(pos).dot(v) / d;
//                        m.put(t, v);
//                    }
//                }
//                Entry<Double, Vec3d> e = m.ceilingEntry(-1e-12);
//                if (e == null || e.getKey() >= 1) {
//                    pos = pos.add(vel);
//                    break;
//                }
//                double t = e.getKey();
//                Vec3d v = e.getValue();
//                pos = pos.add(vel.mul(t - 1e-6));
//                vel = vel.projectAgainst(v).mul(1 - t);
//                velocity.velocity = velocity.velocity.projectAgainst(v);
//            }
//            position.position = position.position.add(pos);
//        }

        List<Vec3d> l2 = world.collisionShape.intersect(new SphereShape(position.position, radius));
        if (!l2.isEmpty()) {
            Vec3d sum = new Vec3d(0, 0, 0);
            for (Vec3d v : l2) {
                sum = sum.add(v);
                if (velocity.velocity.dot(v) < 0) {
                    velocity.velocity = velocity.velocity.projectAgainst(v);
                }
                onGround |= v.z > 0;
            }
            if (sum.length() > 2) {
                sum = sum.div(sum.length() / 2);
            }

            double t = 1, step = t / 2;
            for (int i = 0; i < 20; i++) {
                Vec3d pos = sum.mul(t);
                if (l2.stream().allMatch(v -> pos.dot(v) >= v.lengthSquared())) {
                    t -= step;
                } else {
                    t += step;
                }
                step /= 2;
            }
            position.position = position.position.add(sum.mul(t));
        }
    }
}
