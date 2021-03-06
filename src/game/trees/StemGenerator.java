package game.trees;

import graphics.renderables.Renderable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import physics.shapes.CollisionShape;
import static util.math.MathUtils.floor;
import util.math.Vec3d;

public class StemGenerator {

    private final List<Stem> treeInstances = new ArrayList();
    private final List<List<Vec3d>> treePlacements = new ArrayList();

    public List<CollisionShape> collisionShapes() {
        List<CollisionShape> r = new LinkedList();
        for (int i = 0; i < treeInstances.size(); i++) {
            Stem s = treeInstances.get(i);
            for (Vec3d v : treePlacements.get(i)) {
                s.getCollisionShapes(v).forEach(r::add);
            }
        }
        return r;
    }

    public void generateInstances(int num) {
        num = 8;
        for (int i = 0; i < num; i++) {
            Stem s = Stem.generateTree();
            treeInstances.add(s);
            treePlacements.add(new LinkedList());
        }
        System.out.println("Done generating");
    }

    public void placeTree(Vec3d pos) {
        int chosen = floor(Math.random() * treeInstances.size());
        treePlacements.get(chosen).add(pos);
    }

    public List<Renderable> renderables() {
        List<Renderable> r = new LinkedList();
        for (int i = 0; i < treeInstances.size(); i++) {
            Stem s = treeInstances.get(i);
            for (Vec3d v : treePlacements.get(i)) {
                r.add(s.getRenderable(v));
            }
        }
        for (int i = 0; i < treeInstances.size(); i++) {
            Stem s = treeInstances.get(i);
            for (Vec3d v : treePlacements.get(i)) {
                r.add(s.getRenderableLeaves(v));
            }
        }
        return r;
    }
}
