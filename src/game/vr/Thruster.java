package game.vr;

import engine.Behavior;
import static engine.Core.dt;
import engine.Layer;
import static game.Player.POSTPHYSICS;
import game.RenderableBehavior;
import static game.RenderableBehavior.createRB;
import graphics.models.VoxelModel2;
import graphics.renderables.ColorModel;
import graphics.renderables.ColorModelParticles;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import util.math.MathUtils;
import util.math.Quaternion;
import util.math.Transformation;
import util.math.Vec3d;
import vr.Vive;

public class Thruster extends Behavior {

    public final ControllerBehavior controller = require(ControllerBehavior.class);

    public List<Particle> particles = new LinkedList();
    public ColorModelParticles particlesModel;
    public RenderableBehavior particlesRB;

    @Override
    public void createInner() {
        controller.renderable.renderable = new ColorModel(VoxelModel2.load("controller_red.vox"));
        particlesModel = new ColorModelParticles(VoxelModel2.load("fireball.vox"));
        particlesRB = createRB(particlesModel);
        particlesRB.beforeRender = () -> {
            particlesModel.transforms = particles.stream().map(p -> p.transform()).collect(Collectors.toList());
        };
    }

    @Override
    public void destroyInner() {
        particlesRB.destroy();
    }

    @Override
    public Layer layer() {
        return POSTPHYSICS;
    }

    @Override
    public void step() {
        double t = controller.controller.trigger();
        if (t > .1) {
            Vec3d pullDir = controller.sideways();
            if (controller.controller == Vive.LEFT) {
                pullDir = pullDir.mul(-1);
            }
//            controller.player.applyForce(pullDir.mul(t * -10), .03);
            controller.player.physics.applyForce(pullDir.mul(t * -1000), controller.pos());
//            double pullStrength = Math.exp(.02 * pullDir.dot(controller.player.velocity.velocity));
//            controller.player.velocity.velocity = controller.player.velocity.velocity.add(pullDir.mul(dt() * t * pullStrength * -10));
            for (int i = 0; i < 1000 * t * dt(); i++) {
                particles.add(new Particle(controller.pos(), controller.player.physics.velocity.add(
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

        public Transformation transform() {
            return Transformation.create(position.add(velocity.mul(time)).sub(1 / 8.), Quaternion.IDENTITY, 1 / 32.);
        }
    }
}
