package game.vr;

import engine.Behavior;
import static engine.Core.dt;
import engine.Layer;
import static game.Player.POSTPHYSICS;
import graphics.models.VoxelModel2;
import graphics.renderables.ColorModel;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import util.math.MathUtils;
import util.math.Vec3d;
import vr.ViveInput;

public class Thruster extends Behavior {

    public final ControllerBehavior controller = require(ControllerBehavior.class);

    public List<Particle> particles = new LinkedList();

    @Override
    public void createInner() {
        // controller.model = VoxelModel2.load("controller_red.vox");
        controller.renderable.renderable = new ColorModel(VoxelModel2.load("controller_red.vox"));
    }

    @Override
    public Layer layer() {
        return POSTPHYSICS;
    }

    @Override
    public void step() {
        double t = controller.controller.trigger();
        if (t > .1) {
            Vec3d pullDir = controller.controller.sideways();
            if (controller.controller == ViveInput.LEFT) {
                pullDir = pullDir.mul(-1);
            }
            controller.player.applyForce(pullDir.mul(t * -10), .03);
//            double pullStrength = Math.exp(.02 * pullDir.dot(controller.player.velocity.velocity));
//            controller.player.velocity.velocity = controller.player.velocity.velocity.add(pullDir.mul(dt() * t * pullStrength * -10));
            for (int i = 0; i < 1000 * t * dt(); i++) {
                particles.add(new Particle(controller.pos(), controller.player.velocity.velocity.add(
                        pullDir.mul(10).add(MathUtils.randomInSphere(new Random())).mul(5))));
            }
        }

        for (Particle p : particles) {
            p.time += dt();
        }
        particles.removeIf(p -> p.time > .2);
    }

    public static class Particle {

        public final Vec3d position, velocity;
        public double time = 0;

        public Particle(Vec3d position, Vec3d velocity) {
            this.position = position;
            this.velocity = velocity;
        }
    }
}
