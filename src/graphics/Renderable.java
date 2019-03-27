package graphics;

import graphics.opengl.GLState;
import java.util.function.Supplier;
import util.math.Transformation;

public interface Renderable extends Model {

    public void bindGeomShader();

    public Transformation getTransform();

    public default void renderTransformed() {
        GLState.getShaderProgram().setMVP(getTransform());
        render();
    }

    public static Renderable create(Runnable bindGeomShader, Supplier<Transformation> getTransform, Runnable render) {
        return new Renderable() {
            @Override
            public void bindGeomShader() {
                bindGeomShader.run();
            }

            @Override
            public Transformation getTransform() {
                return getTransform.get();
            }

            @Override
            public void render() {
                render.run();
            }
        };
    }
}
