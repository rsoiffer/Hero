package graphics;

import static graphics.GeometryPass.SHADER_PBR;
import static graphics.opengl.GLObject.bindAll;
import graphics.opengl.GLState;
import java.util.function.Supplier;
import org.joml.Matrix4d;
import util.math.Transformation;

public interface Renderable {

    public void renderGeom();

    public void renderShadow();

    public default void setTransform(Matrix4d m) {
        GLState.getShaderProgram().setUniform("model", m);
    }

    public default void setTransform(Transformation t) {
        setTransform(t.modelMatrix());
    }

    public static class RenderablePBR implements Renderable {

        private final Model m;
        private final PBRTexture tex;
        private final Supplier<Transformation> t;

        public RenderablePBR(Model m, PBRTexture tex, Supplier<Transformation> t) {
            this.m = m;
            this.tex = tex;
            this.t = t;
        }

        @Override
        public void renderGeom() {
            bindAll(SHADER_PBR, tex);
            setTransform(t.get());
            m.render();
        }

        @Override
        public void renderShadow() {
            setTransform(t.get());
            m.render();
        }
    }
}
