package graphics.renderables;

import graphics.Camera;
import graphics.PBRTexture;
import graphics.models.CustomModel;
import graphics.models.ModelSimplifier3;
import static graphics.opengl.GLObject.bindAll;
import static graphics.passes.GeometryPass.SHADER_PBR;
import static graphics.passes.ShadowPass.SHADER_SHADOW;
import static graphics.passes.ShadowPass.SHADER_SHADOW_ALPHA;
import java.util.ArrayList;
import java.util.List;
import static util.math.MathUtils.clamp;
import static util.math.MathUtils.round;
import util.math.Transformation;
import util.math.Vec3d;

public class LODPBRModel extends Renderable {

    public int numLOD;
    public List<CustomModel> modelLODS = new ArrayList();
    public PBRTexture tex;
    public Transformation t = Transformation.IDENTITY;
    public boolean castShadow = true;

    public LODPBRModel(CustomModel model, PBRTexture tex, int numLOD) {
        this.numLOD = numLOD;
        for (int i = 0; i < numLOD; i++) {
            if (i > 0) {
                model = ModelSimplifier3.simplify(model, .1 * Math.pow(3, i));
                model.createVAO();
            }
            modelLODS.add(model);
        }
        this.tex = tex;
    }

    public LODPBRModel(LODPBRModel other) {
        numLOD = other.numLOD;
        modelLODS = other.modelLODS;
        tex = other.tex;
        t = other.t;
    }

    @Override
    public void renderGeom() {
        double estimatedDist = t.apply(new Vec3d(0, 0, 0)).sub(Camera.camera3d.position).setZ(0).length();
        int lod = clamp(round(Math.log(estimatedDist / 50) / Math.log(2)), 0, numLOD);
        // int lod = clamp(round(estimatedDist / 20), 0, numLOD);
        if (lod < numLOD) {
            bindAll(SHADER_PBR, tex);
            setTransform(t);
            modelLODS.get(lod).render();
        }
    }

    @Override
    public void renderShadow() {
        if (castShadow) {
            double estimatedDist = t.apply(new Vec3d(0, 0, 0)).sub(Camera.camera3d.position).setZ(0).length();
            int lod = clamp(round(Math.log(estimatedDist / 50) / Math.log(2)), 0, numLOD);
//            int lod = clamp(round(estimatedDist / 20), 0, numLOD);
            if (lod < numLOD) {
                if (tex.hasAlpha()) {
                    bindAll(SHADER_SHADOW_ALPHA, tex);
                } else {
                    bindAll(SHADER_SHADOW);
                }
                setTransform(t);
                modelLODS.get(lod).render();
            }
        }
    }
}
