package physics;

import behaviors._3d.PositionBehavior3d;
import behaviors._3d.PreviousPositionBehavior3d;
import behaviors._3d.VelocityBehavior3d;
import engine.Behavior;
import engine.Layer;
import game.World;
import util.math.Vec3d;

public class PhysicsBehavior extends Behavior {

    private static final Layer PHYSICS = new Layer(5);
    private static final int DETAIL = 10;

    public final PositionBehavior3d position = require(PositionBehavior3d.class);
    public final PreviousPositionBehavior3d prevPos = require(PreviousPositionBehavior3d.class);
    public final VelocityBehavior3d velocity = require(VelocityBehavior3d.class);

    public AABB hitbox;
    public World world;

    public boolean onGround;
    public boolean hitWall;

    @Override
    public Layer layer() {
        return PHYSICS;
    }

    private boolean moveToWall(Vec3d del) {
        if (!wouldCollideAt(position.position.add(del))) {
            position.position = position.position.add(del);
            return false;
        }
        double best = 0;
        double check = .5;
        double step = .25;
        for (int i = 0; i < DETAIL; i++) {
            if (wouldCollideAt(del.mul(check).add(position.position))) {
                check -= step;
            } else {
                best = check;
                check += step;
            }
            step /= 2;
        }
        position.position = position.position.add(del.mul(best));
        return true;
    }

    @Override
    public void step() {
        // Useful vars
        Vec3d del = position.position.sub(prevPos.prevPos);

        // Reset all vars
        onGround = false;
        hitWall = false;

        // Check collision
        if (wouldCollideAt(position.position)) {
            if (wouldCollideAt(prevPos.prevPos)) {
                // Give up
                velocity.velocity = new Vec3d(0, 0, 0);
            } else {
                position.position = prevPos.prevPos;

                if (moveToWall(new Vec3d(0, 0, del.z))) {
                    velocity.velocity = velocity.velocity.setZ(0);
                    if (del.z < 0) {
                        onGround = true;
                    }
                }
                if (moveToWall(new Vec3d(del.x, 0, 0))) {
                    velocity.velocity = velocity.velocity.setX(0);
                    hitWall = true;
                }
                if (moveToWall(new Vec3d(0, del.y, 0))) {
                    velocity.velocity = velocity.velocity.setY(0);
                    hitWall = true;
                }
            }
        }

        // Set onGround
        if (!onGround && wouldCollideAt(position.position.add(new Vec3d(0, 0, -.01)))) {
            onGround = true;
        }
    }

    public boolean wouldCollideAt(Vec3d pos) {
        return world.collisionShape.intersects(hitbox.translate(pos));
    }
}
