package game;

import engine.Behavior;
import game.trees.TreeGenerator;
import static game.vr.IceCaster.iceModel;
import graphics.PBRTexture;
import graphics.models.CustomModel;
import graphics.opengl.Texture;
import graphics.renderables.DiffuseModel;
import graphics.renderables.PBRModel;
import graphics.renderables.Renderable;
import graphics.renderables.RenderableList;
import static graphics.voxels.VoxelRenderer.DIRS;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import physics.AABB;
import physics.CollisionShape;
import physics.MultigridShape;
import physics.SurfaceNetShape;
import util.Noise;
import static util.math.MathUtils.floor;
import util.math.Vec2d;
import util.math.Vec3d;

public class World extends Behavior {

    public static final double FLOOR_HEIGHT = 4;
    public static final double BUILDING_SIZE = 32;
    public static final double STREET_WIDTH = 20;
    public static final double BLOCK_WIDTH = 2 * BUILDING_SIZE + STREET_WIDTH;
    public static final double BLOCK_HEIGHT = 8 * BUILDING_SIZE + STREET_WIDTH;

    private static final int NUM_WALL_TYPES = 10;
    private static final double[] WALL_SCALES = {2, 9, 3, 8, 4, 3, 3, 4, 6, 10};
    private static final String[] WALL_TEXTURES = {"tower.png", "glass_0.png", "glass_1.png",
        "highrise_0.png", "highrise_1.png", "highrise_2.png", "highrise_3.png", "highrise_4.png"};
    private static final String[] WALL_PBR_TEXTURES = {"highrise_facade_1", "highrise_facade_3"};

    public final RenderableBehavior renderable = require(RenderableBehavior.class);

    public CollisionShape collisionShape;
    private final List<AABB> buildings = new ArrayList();
    private final TreeGenerator trees = new TreeGenerator();
    private final Random random = new Random();

    @Override
    public void createInner() {
        Noise heightNoise = new Noise(random);
        for (int i = 0; i < 2000; i += BLOCK_WIDTH) {
            for (int j = 0; j < 2000; j += BLOCK_HEIGHT) {
                for (int k = 0; k < 200; k++) {
                    double x = i + floor(Math.random() * 2) * BUILDING_SIZE;
                    double y = j + floor(Math.random() * 8) * BUILDING_SIZE;
                    if (x != 0 || y != 0) {
                        if (!buildings.stream().anyMatch(b -> b.lower.x == x && b.lower.y == y)) {
                            double height = floor(Math.random() * 40 * heightNoise.noise2d(x, y, .005) + 10) * FLOOR_HEIGHT;
                            buildings.add(new AABB(new Vec3d(x, y, 0), new Vec3d(x + BUILDING_SIZE, y + BUILDING_SIZE, height)));
                        }
                    }
                }
                buildings.add(new AABB(new Vec3d(i, j, -500), new Vec3d(i + 2 * BUILDING_SIZE + STREET_WIDTH, j + 8 * BUILDING_SIZE + STREET_WIDTH, 0)));
            }
        }

        trees.generateInstances(32);
        for (int k = 0; k < 1000; k++) {
            double x = Math.random() * 2000;
            double y = Math.random() * 2000;
            trees.placeTree(new Vec3d(x, y, 0));
        }

        List<CollisionShape> l = new LinkedList();
        l.addAll(buildings);
        l.addAll(trees.collisionShapes());
        l.add(new SurfaceNetShape(iceModel));
        collisionShape = new MultigridShape(l);

        renderable.renderable = createRenderable();
    }

    public Renderable createRenderable() {
        CustomModel ground = new CustomModel();
        for (AABB b : buildings) {
            if (b.upper.z == 0) {
                ground.addRectangle(b.lower.setZ(b.upper.z), b.size().setY(0).setZ(0), b.size().setX(0).setZ(0),
                        new Vec2d(0, 0), new Vec2d(b.size().x / 2, 0), new Vec2d(0, b.size().y / 2));
            }
        }
        ground.createVAO();

        CustomModel roofs = new CustomModel();
        for (AABB b : buildings) {
            if (b.upper.z != 0) {
                roofs.addRectangle(b.lower.setZ(b.upper.z), b.size().setY(0).setZ(0), b.size().setX(0).setZ(0),
                        new Vec2d(0, 0), new Vec2d(b.size().x / 4, 0), new Vec2d(0, b.size().y / 4));
            }
        }
        roofs.createVAO();

        CustomModel[] walls = new CustomModel[NUM_WALL_TYPES];
        for (int i = 0; i < NUM_WALL_TYPES; i++) {
            walls[i] = new CustomModel();
        }
        for (AABB b : buildings) {
            int i = floor(Math.random() * NUM_WALL_TYPES);
            for (int j = 0; j < 4; j++) {
                Vec3d dir = DIRS.get(j).mul(b.size());
                Vec3d dir2 = DIRS.get(j < 2 ? j + 2 : 3 - j).mul(b.size());
                Vec3d dir3 = DIRS.get(5).mul(b.size());
                Vec3d v = b.lower.add(b.size().div(2)).add(dir.div(2)).sub(dir2.div(2)).sub(dir3.div(2));
                float texW = (float) Math.abs(dir2.x + dir2.y + dir2.z) / (float) (FLOOR_HEIGHT * WALL_SCALES[i]);
                float texH = (float) Math.abs(dir3.x + dir3.y + dir3.z) / (float) (FLOOR_HEIGHT * WALL_SCALES[i]);
                walls[i].addRectangle(v, dir2, dir3, new Vec2d(0, 0), new Vec2d(texW, 0), new Vec2d(0, texH));
            }
        }
        for (int i = 0; i < NUM_WALL_TYPES; i++) {
            walls[i].createVAO();
        }

        List<Renderable> parts = new LinkedList();
        parts.add(new PBRModel(ground, PBRTexture.loadFromFolder("sidewalk")));
        parts.add(new PBRModel(roofs, PBRTexture.loadFromFolder("concrete_floor")));
        for (int i = 0; i < NUM_WALL_TYPES; i++) {
            if (i < WALL_TEXTURES.length) {
                parts.add(new DiffuseModel(walls[i], Texture.load(WALL_TEXTURES[i])));
            } else {
                parts.add(new PBRModel(walls[i], PBRTexture.loadFromFolder(WALL_PBR_TEXTURES[i - WALL_TEXTURES.length])));
            }
        }
        parts.addAll(trees.renderables());
        return new RenderableList(parts);
    }

    public double raycastDown(Vec3d pos) {
        double d = Double.MAX_VALUE;
        for (AABB b : buildings) {
            if (b.contains(pos.setZ(b.center().z))) {
                d = Math.min(d, pos.z - b.upper.z);
            }
        }
        return d;
    }
}
