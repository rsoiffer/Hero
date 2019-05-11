package game.trees;

import graphics.renderables.Renderable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import physics.CollisionShape;
import static util.math.MathUtils.floor;
import util.math.Vec3d;

public class StemGenerator {

    private final List<Stem> treeInstances = new ArrayList();
    private final List<List<Vec3d>> treePlacements = new ArrayList();

    public List<CollisionShape> collisionShapes() {
        List<CollisionShape> r = new LinkedList();
//        for (int i = 0; i < treeInstances.size(); i++) {
//            Stem tb = treeInstances.get(i);
//            for (Vec3d v : treePlacements.get(i)) {
//                tb.getCollisionShapes(v).forEach(r::add);
//            }
//        }
        return r;
    }

    public void generateInstances(int num) {
        num = 4;
        for (int i = 0; i < num; i++) {
            Stem tb = Stem.generateTree();
            treeInstances.add(tb);
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
            Stem tb = treeInstances.get(i);
            for (Vec3d v : treePlacements.get(i)) {
                r.add(tb.getRenderable(v));
            }
        }
//        for (int i = 0; i < treeInstances.size(); i++) {
//            TreeBranch tb = treeInstances.get(i);
//            for (Vec3d v : treePlacements.get(i)) {
//                r.add(tb.getLeafRenderable(v));
//            }
//        }
        return r;
    }
}
