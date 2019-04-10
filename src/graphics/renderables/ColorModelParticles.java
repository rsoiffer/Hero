package graphics.renderables;

import graphics.models.Model;
import static graphics.passes.GeometryPass.SHADER_COLOR;
import java.util.LinkedList;
import java.util.List;
import util.math.Transformation;
import util.math.Vec3d;

public class ColorModelParticles extends Renderable {

    public Model model;
    public List<Transformation> transforms = new LinkedList();
    public Vec3d color = new Vec3d(1, 1, 1);
    public double metallic = 0;
    public double roughness = .8;
    public boolean renderShadow = false;

    public ColorModelParticles(Model model) {
        this.model = model;
    }

    @Override
    public void renderGeom() {
        SHADER_COLOR.bind();
        SHADER_COLOR.setUniform("color", color);
        SHADER_COLOR.setUniform("metallic", (float) metallic);
        SHADER_COLOR.setUniform("roughness", (float) roughness);
        for (Transformation t : transforms) {
            setTransform(t);
            model.render();
        }
    }

    @Override
    public void renderShadow() {
        if (renderShadow) {
            for (Transformation t : transforms) {
                setTransform(t);
                model.render();
            }
        }
    }
}
