package graphics.renderables;

import graphics.models.Model;
import static graphics.passes.GeometryPass.SHADER_COLOR;
import static graphics.passes.ShadowPass.SHADER_SHADOW;
import util.math.Transformation;
import util.math.Vec3d;

public class ColorModel extends Renderable {

    public Model model;
    public Transformation t = Transformation.IDENTITY;
    public Vec3d color = new Vec3d(1, 1, 1);
    public double metallic = 0;
    public double roughness = .8;

    public ColorModel(Model model) {
        this.model = model;
    }

    @Override
    public void renderGeom() {
        SHADER_COLOR.bind();
        SHADER_COLOR.setUniform("color", color);
        SHADER_COLOR.setUniform("metallic", (float) metallic);
        SHADER_COLOR.setUniform("roughness", (float) roughness);
        setTransform(t);
        model.render();
    }

    @Override
    public void renderShadow() {
        SHADER_SHADOW.bind();
        setTransform(t);
        model.render();
    }
}
