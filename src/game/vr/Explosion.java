package game.vr;

import engine.Behavior;
import static engine.Core.dt;
import engine.Layer;
import static game.Player.POSTPHYSICS;
import game.RenderableBehavior;
import static game.RenderableBehavior.createRB;
import game.vr.Thruster.Particle;
import graphics.models.VoxelModel2;
import graphics.renderables.ColorModel;
import graphics.renderables.ColorModelParticles;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import util.math.MathUtils;
import util.math.Vec3d;
import vr.Vive;
import static vr.Vive.TRIGGER;

public class Explosion extends Behavior {

    public final ControllerBehavior controller = require(ControllerBehavior.class);

    public double charge;
    public List<Particle> particles = new LinkedList();
    public ColorModelParticles particlesModel;
    public RenderableBehavior particlesRB;

    @Override
    public void createInner() {
        controller.renderable.renderable = new ColorModel(VoxelModel2.load("controller_orange.vox"));
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
        charge += dt();
        charge = Math.min(charge, 1.5);
        if (controller.controller.buttonJustPressed(TRIGGER)) {
            Vec3d pullDir = controller.sideways();
            if (controller.controller == Vive.LEFT) {
                pullDir = pullDir.mul(-1);
            }

//            double exag = controller.player.velocity.velocity.dot(pullDir);
//            exag = Math.log(1 + Math.exp(.01 * exag));
//            Vec3d impulse = pullDir.mul(charge * -15 * exag);
//            controller.player.velocity.velocity = controller.player.velocity.velocity.add(impulse);
            controller.player.physics.applyImpulse(pullDir.mul(charge * -1000), controller.pos());

            for (int i = 0; i < 1000 * charge; i++) {
                particles.add(new Particle(controller.pos(), controller.player.physics.velocity.add(
                        pullDir.mul(10).add(MathUtils.randomInSphere(new Random())).mul(5))));
            }
            charge = 0;
        }

        for (Particle p : particles) {
            p.time += dt();
        }
        particles.removeIf(p -> Math.random() < 5 * dt());
    }
}
