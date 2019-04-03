package game;

import engine.Behavior;
import graphics.renderables.Renderable;
import java.util.Collection;
import java.util.stream.Stream;

public class RenderableBehavior extends Behavior {

    public static final Collection<RenderableBehavior> ALL = track(RenderableBehavior.class);

    public Renderable renderable;
    public boolean visible = true;

    public static Stream<Renderable> allRenderables() {
        return ALL.stream().filter(r -> r.visible).map(r -> r.renderable);
    }

    public static RenderableBehavior createRB(Renderable renderable) {
        RenderableBehavior rb = new RenderableBehavior();
        rb.renderable = renderable;
        rb.create();
        return rb;
    }
}
