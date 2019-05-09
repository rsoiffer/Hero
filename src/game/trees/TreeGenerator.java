package game.trees;

import graphics.renderables.Renderable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import physics.CollisionShape;
import static util.math.MathUtils.floor;
import util.math.Vec3d;

public class TreeGenerator {

    private final List<TreeBranch> treeInstances = new ArrayList();
    private final Map<Vec3d, TreeBranch> treePlacements = new HashMap();

    public List<CollisionShape> collisionShapes() {
        return treePlacements.entrySet().stream()
                .flatMap(e -> e.getValue().getCollisionShapes(e.getKey()))
                .collect(Collectors.toList());
    }

    public void generateInstances(int num) {
        for (int i = 0; i < num; i++) {
            TreeBranch tb = TreeBranch.generateTree();
            treeInstances.add(tb);
        }
    }

    public void placeTree(Vec3d pos) {
        TreeBranch chosen = treeInstances.get(floor(Math.random() * treeInstances.size()));
        treePlacements.put(pos, chosen);
    }

    public List<Renderable> renderables() {
        return treePlacements.entrySet().stream().flatMap(e -> Stream.of(
                //e.getValue().getRenderable(e.getKey()),e.getValue().getLeafRenderable(e.getKey())))
                e.getValue().getRenderable(e.getKey())))
                .collect(Collectors.toList());
    }
}
