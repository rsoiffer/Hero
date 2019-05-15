package graphics.renderables;

import graphics.PBRTexture;
import graphics.models.Model;
import static graphics.opengl.GLObject.bindAll;
import static graphics.passes.GeometryPass.SHADER_PBR;
import static graphics.passes.ShadowPass.SHADER_SHADOW;
import static graphics.passes.ShadowPass.SHADER_SHADOW_ALPHA;
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
        SHADER_PBR.setUniform("lod", 0f);
        model.render();
    }

    @Override
    public void renderShadow() {
        if (tex.hasAlpha()) {
            bindAll(SHADER_SHADOW_ALPHA, tex);
            SHADER_SHADOW_ALPHA.setUniform("lod", 0f);
        } else {
            bindAll(SHADER_SHADOW);
        }
        setTransform(t);
        model.render();
    }
}
