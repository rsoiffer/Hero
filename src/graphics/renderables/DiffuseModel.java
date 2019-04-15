package graphics.renderables;

import graphics.models.Model;
import static graphics.opengl.GLObject.bindAll;
import graphics.opengl.Texture;
import static graphics.passes.GeometryPass.SHADER_DIFFUSE;
import static graphics.passes.ShadowPass.SHADER_SHADOW;
import util.math.Transformation;

public class DiffuseModel extends Renderable {

    public Model model;
    public Texture tex;
    public Transformation t = Transformation.IDENTITY;
    public double metallic = 0;
    public double roughness = .8;

    public DiffuseModel(Model model, Texture tex) {
        this.model = model;
        this.tex = tex;
    }

    @Override
    public void renderGeom() {
        bindAll(SHADER_DIFFUSE, tex);
        SHADER_DIFFUSE.setUniform("metallic", (float) metallic);
        SHADER_DIFFUSE.setUniform("roughness", (float) roughness);
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
