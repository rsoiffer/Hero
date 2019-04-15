package game;

import graphics.PBRTexture;
import graphics.models.CustomModel;
import graphics.renderables.PBRModel;
import graphics.renderables.Renderable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import physics.CapsuleShape;
import physics.CollisionShape;
import util.Mutable;
import util.math.MathUtils;
import static util.math.MathUtils.floor;
import util.math.Vec2d;
import util.math.Vec3d;

public class TreeBranch {

    // Generation
    private static TreeBranch genBranch(Vec3d pos, Vec3d dir, int level, int segment, boolean root) {
        dir = dir.lerp(new Vec3d(0, 0, length(level - 1)), .05);
        double radius = radius(level, segment);
        TreeBranch b = new TreeBranch(pos, dir, radius, level, root);
        if (segment < 4 - level) {
            Vec3d newDir = MathUtils.randomInSphere(new Random());
            double length = 1 * Math.pow(.8, level);
            TreeBranch next = genBranch(pos.add(dir), dir.add(newDir.mul(length)), level, segment + 1, false);
            b.next = next;
        }
        if (level < 1) {
            int numBranches = floor((4 + 2 * Math.random()) * 2 * Math.pow(.4, level));
            for (int i = 0; i < numBranches; i++) {
                Vec3d newDir = MathUtils.randomInSphere(new Random());
                if (level == 0) {
                    newDir = newDir.projectAgainst(dir);
                } else {
                    newDir = newDir.add(dir.mul(.1));
                }
                Vec3d newPos = pos.add(dir.mul(Math.random())).add(newDir.setLength(radius * .2));
                b.branches.add(genBranch(newPos, newDir.mul(length(level)), level + 1, Math.max(level, segment - 1), true));
            }
        }
        return b;
    }

    public static TreeBranch generateTree(Vec3d pos) {
        return genBranch(pos, new Vec3d(0, 0, 20), 0, 0, true);
    }

    private static double length(int level) {
        return 10.0 * Math.pow(.3, level);
    }

    private static double radius(int level, int segment) {
        return 5.0 * Math.pow(.2, level) * (1 - segment * .18);
    }

    // Model creation
    private static void addBranchToModel(List<Vec3d> branch, List<Double> radii, int detail, CustomModel model) {
        Vec3d randomDir = MathUtils.randomInSphere(new Random());
        randomDir = randomDir.projectAgainst(branch.get(branch.size() - 1).sub(branch.get(0)));
        List<Vec3d> dirs1 = new ArrayList(), dirs2 = new ArrayList();
        for (int i = 0; i < branch.size(); i++) {
            Vec3d dir0 = branch.get(Math.min(i + 1, branch.size() - 1)).sub(branch.get(Math.max(i - 1, 0))).normalize();
            Vec3d dir1 = dir0.cross(randomDir).normalize();
            Vec3d dir2 = dir0.cross(dir1).normalize();
            dirs1.add(dir1);
            dirs2.add(dir2);
        }
        double texW = Math.max(1, floor(2 * Math.PI * radii.get(0) / 2));
        double texH = 0;
        for (int i = 0; i < branch.size() - 1; i++) {
            double dTexH = branch.get(i + 1).sub(branch.get(i)).length() / 4;
            for (int j = 0; j < detail; j++) {
                double angle0 = j * 2 * Math.PI / detail, angle1 = (j + 1) * 2 * Math.PI / detail;
                Vec3d v00 = branch.get(i).add(dirs1.get(i).mul(Math.cos(angle0) * radii.get(i)))
                        .add(dirs2.get(i).mul(Math.sin(angle0) * radii.get(i)));
                Vec3d v01 = branch.get(i).add(dirs1.get(i).mul(Math.cos(angle1) * radii.get(i)))
                        .add(dirs2.get(i).mul(Math.sin(angle1) * radii.get(i)));
                Vec3d v10 = branch.get(i + 1).add(dirs1.get(i + 1).mul(Math.cos(angle0) * radii.get(i + 1)))
                        .add(dirs2.get(i + 1).mul(Math.sin(angle0) * radii.get(i + 1)));
                Vec3d v11 = branch.get(i + 1).add(dirs1.get(i + 1).mul(Math.cos(angle1) * radii.get(i + 1)))
                        .add(dirs2.get(i + 1).mul(Math.sin(angle1) * radii.get(i + 1)));

                Vec2d uv = new Vec2d(texW * j / detail, texH), uvd1 = new Vec2d(texW / detail, 0), uvd2 = new Vec2d(0, dTexH);
                model.addTriangle(v00, uv, v01, uv.add(uvd1), v11, uv.add(uvd1).add(uvd2));
                model.addTriangle(v00, uv, v11, uv.add(uvd1).add(uvd2), v10, uv.add(uvd2));
            }
            texH += dTexH;
        }
    }

    public static List<CollisionShape> collisionShapes(List<TreeBranch> trees) {
        return trees.stream().flatMap(t -> t.partsRecursive(2).map(p -> p.collisionShape())).collect(Collectors.toList());
    }

    public static Renderable createBranchRenderable(List<TreeBranch> trees) {
        CustomModel treesModel = new CustomModel();
        for (TreeBranch tree : trees) {
            tree.partsRecursive().filter(tb -> tb.root).forEach(tb -> {
                List<Vec3d> branch = new ArrayList();
                List<Double> radii = new ArrayList();
                int detail = 12 / (tb.level + 1);
                while (tb != null) {
                    branch.add(tb.pos);
                    radii.add(tb.radius);
                    if (tb.next == null) {
                        branch.add(tb.pos.add(tb.dir));
                        radii.add(tb.radius * .6);
                        branch.add(tb.pos.add(tb.dir.mul(1.01)));
                        radii.add(0.01);
                    }
                    tb = tb.next;
                }
                addBranchToModel(branch, radii, detail, treesModel);
            });
        }
        treesModel.smoothVertexNormals();
        treesModel.createVAO();
        System.out.println(treesModel.numTriangles());
        return new PBRModel(treesModel, PBRTexture.loadFromFolder("bark"));
    }

    public static Renderable createLeafRenderable(List<TreeBranch> trees) {
        CustomModel leavesModel = new CustomModel();
        Random random = new Random();
        Mutable<Integer> c = new Mutable(0);
        for (TreeBranch tree : trees) {
            tree.partsRecursive().filter(tb -> tb.level == 2).forEach(tb -> {
                for (int i = 0; i < 10; i++) {
                    double z = Math.random();
                    double scale = 1 + 3 * Math.random();
                    c.o += 1;
                    leavesModel.addRectangle(tb.pos.add(tb.dir.mul(z)),
                            MathUtils.randomInSphere(random).mul(scale), MathUtils.randomInSphere(random).mul(scale),
                            new Vec2d(0, 0), new Vec2d(1, 0), new Vec2d(0, 1));
                }
            });
        }
        leavesModel.createVAO();
        System.out.println(leavesModel.numTriangles());
        return new PBRModel(leavesModel, PBRTexture.loadFromFolder("leaf"));
    }

    // Instances
    private final Vec3d pos, dir;
    private final double radius;
    private final int level;
    private final boolean root;
    private TreeBranch next;
    private final List<TreeBranch> branches = new LinkedList();

    public TreeBranch(Vec3d pos, Vec3d dir, double radius, int level, boolean root) {
        this.pos = pos;
        this.dir = dir;
        this.radius = radius;
        this.level = level;
        this.root = root;
    }

    private CapsuleShape collisionShape() {
        return new CapsuleShape(pos, dir, radius);
    }

    private Stream<TreeBranch> partsRecursive() {
        if (next == null) {
            return Stream.concat(Stream.of(this), branches.stream().flatMap(b -> b.partsRecursive()));
        }
        return Stream.concat(Stream.concat(Stream.of(this), next.partsRecursive()),
                branches.stream().flatMap(b -> b.partsRecursive()));
    }

    private Stream<TreeBranch> partsRecursive(int maxLevel) {
        return partsRecursive().filter(p -> p.level <= maxLevel);
    }
}
