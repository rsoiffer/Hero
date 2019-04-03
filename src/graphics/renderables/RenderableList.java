package graphics.renderables;

import java.util.Arrays;

public class RenderableList extends Renderable {

    private final Iterable<Renderable> renderables;

    public RenderableList(Iterable<Renderable> renderables) {
        this.renderables = renderables;
    }

    public RenderableList(Renderable... renderables) {
        this(Arrays.asList(renderables));
    }

    @Override
    public void renderGeom() {
        renderables.forEach(Renderable::renderGeom);
    }

    @Override
    public void renderShadow() {
        renderables.forEach(Renderable::renderShadow);
    }
}
