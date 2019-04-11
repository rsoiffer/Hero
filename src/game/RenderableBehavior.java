package game;

import engine.Behavior;
import engine.Layer;
import static engine.Layer.POSTUPDATE;
import graphics.renderables.Renderable;
import java.util.Collection;
import java.util.stream.Stream;

public class RenderableBehavior extends Behavior {

    public static final Collection<RenderableBehavior> ALL = track(RenderableBehavior.class);

    public Renderable renderable;
    public boolean visible = true;
    public Runnable beforeRender = null;

    public static Stream<Renderable> allRenderables() {
        return ALL.stream().filter(r -> r.visible).map(r -> r.renderable);
    }

    public static RenderableBehavior createRB(Renderable renderable) {
        RenderableBehavior rb = new RenderableBehavior();
        rb.renderable = renderable;
        rb.create();
        return rb;
    }

    @Override
    public Layer layer() {
        return POSTUPDATE;
    }

    @Override
    public void step() {
        if (beforeRender != null) {
            beforeRender.run();
        }
    }
}
