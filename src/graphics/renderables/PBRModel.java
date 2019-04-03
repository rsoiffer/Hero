package graphics.renderables;

import graphics.PBRTexture;
import graphics.models.Model;
import static graphics.opengl.GLObject.bindAll;
import static graphics.passes.GeometryPass.SHADER_PBR;
import util.math.Transformation;

public class PBRModel extends Renderable {

    public Model model;
    public PBRTexture tex;
    public Transformation t = Transformation.IDENTITY;

    public PBRModel(Model model, PBRTexture tex) {
        this.model = model;
        this.tex = tex;
    }

    @Override
    public void renderGeom() {
        bindAll(SHADER_PBR, tex);
        setTransform(t);
        model.render();
    }

    @Override
    public void renderShadow() {
        setTransform(t);
        model.render();
    }
}
