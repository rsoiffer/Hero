package game;

import engine.Behavior;
import game.trees.StemGenerator;
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
import physics.CapsuleShape;
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

    private static final int NUM_WALL_TYPES = 11;
    private static final double[] WALL_SCALES = {2, 9, 3, 8, 4, 3, 3, 4, 12, 12, 10};
    private static final double[] WALL_SCALES_X = {1, 1, 1, 1, 1, 1, 1, 1, 1, .5, 1};
    private static final String[] WALL_TEXTURES = {"tower.png", "glass_0.png", "glass_1.png",
        "highrise_0.png", "highrise_1.png", "highrise_2.png", "highrise_3.png", "highrise_4.png"};
    private static final String[] WALL_PBR_TEXTURES = {"highrise_facade_1", "highrise_facade_2", "highrise_facade_3"};

    public final RenderableBehavior renderable = require(RenderableBehavior.class);

    public CollisionShape collisionShape;

    private final List<AABB> buildings = new ArrayList();
    private final List<AABB> intersections = new ArrayList();
    private final List<AABB> roads = new ArrayList();
    private final List<AABB> sidewalks = new ArrayList();
    private final List<AABB> parks = new ArrayList();
    private final StemGenerator treeGenerator = new StemGenerator();
    private final List<AABB> billboards = new ArrayList();
    private final List<CapsuleShape> poles = new ArrayList();

    @Override
    public void createInner() {
        treeGenerator.generateInstances(32);
        Noise heightNoise = new Noise(new Random());

        for (int i = 0; i < 2000; i += BLOCK_WIDTH) {
            for (int j = 0; j < 2000; j += BLOCK_HEIGHT) {
                double buffer1 = 4;
                double buffer2 = 4.2;
                intersections.add(new AABB(new Vec3d(i - STREET_WIDTH + buffer1, j - STREET_WIDTH + buffer1, -500), new Vec3d(i - buffer1, j - buffer1, 0)));
                roads.add(new AABB(new Vec3d(i - buffer1, j - STREET_WIDTH + buffer1, -500), new Vec3d(i + 2 * BUILDING_SIZE + buffer1, j - buffer1, 0)));
                roads.add(new AABB(new Vec3d(i - STREET_WIDTH + buffer1, j - buffer1, -500), new Vec3d(i + buffer1, j + 8 * BUILDING_SIZE + buffer1, 0)));
                boolean parkBlock = Math.random() < .2;
                if (parkBlock) {
                    parks.add(new AABB(new Vec3d(i - buffer2, j - buffer2, -500), new Vec3d(i + 2 * BUILDING_SIZE + buffer2, j + 8 * BUILDING_SIZE + buffer2, .1)));
                    for (int k = 0; k < 15; k++) {
                        double x = i + Math.random() * 2 * BUILDING_SIZE;
                        double y = j + Math.random() * 8 * BUILDING_SIZE;
                        treeGenerator.placeTree(new Vec3d(x, y, 0));
                    }
                } else {
                    sidewalks.add(new AABB(new Vec3d(i - buffer2, j - buffer2, -500), new Vec3d(i + 2 * BUILDING_SIZE + buffer2, j + 8 * BUILDING_SIZE + buffer2, .1)));
                    for (int k = 0; k < 200; k++) {
                        double x = i + floor(Math.random() * 2) * BUILDING_SIZE;
                        double y = j + floor(Math.random() * 8) * BUILDING_SIZE;
                        if (x != 0 || y != 0) {
                            if (!buildings.stream().anyMatch(b -> b.lower.x == x && b.lower.y == y)) {
                                double dist2 = new Vec2d(x, y).sub(1000).lengthSquared();
                                double height = floor(Math.random() * (50 * Math.exp(-dist2 / 160000) + 50 * heightNoise.noise2d(x, y, .005)) + 4) / 2 * 2 * FLOOR_HEIGHT;
                                buildings.add(new AABB(new Vec3d(x, y, 0), new Vec3d(x + BUILDING_SIZE, y + BUILDING_SIZE, height)));

//                                for (int l = 0; l < 2; l++) {
//                                    double xScale = 10 * Math.random() + 5;
//                                    double yScale = 10 * Math.random() + 5;
//                                    double zScale = 6 * Math.random() + 3;
//                                    if (Math.random() < .5) {
//                                        xScale /= Math.abs(xScale) * 10;
//                                    } else {
//                                        yScale /= Math.abs(xScale) * 10;
//                                    }
//                                    double x2 = x + Math.random() * BUILDING_SIZE - xScale / 2 + xScale * (Math.random() - .5);
//                                    double y2 = y + Math.random() * BUILDING_SIZE - yScale / 2 + yScale * (Math.random() - .5);
//                                    double z = Math.random() * height;
//                                    AABB bb = new AABB(new Vec3d(x2, y2, z), new Vec3d(x2 + xScale, y2 + yScale, z + zScale));
//                                    if (!new AABB(new Vec3d(i, j, 0), new Vec3d(i + 2 * BUILDING_SIZE, j + 8 * BUILDING_SIZE, 500)).contains(bb.center())) {
//                                        billboards.add(bb);
//                                    } else {
//                                        l--;
//                                    }
//                                }
                            }
                        }
                    }
                }
                poles.add(new CapsuleShape(new Vec3d(i - STREET_WIDTH + 3, j - STREET_WIDTH + 3, 0), new Vec3d(0, 0, 5), .1));
                poles.add(new CapsuleShape(new Vec3d(i - 3, j - STREET_WIDTH + 3, 0), new Vec3d(0, 0, 5), .1));
                poles.add(new CapsuleShape(new Vec3d(i - STREET_WIDTH + 3, j - 3, 0), new Vec3d(0, 0, 5), .1));
                poles.add(new CapsuleShape(new Vec3d(i - 3, j - 3, 0), new Vec3d(0, 0, 5), .1));
            }
        }

        List<CollisionShape> l = new LinkedList();
        l.add(new AABB(new Vec3d(0, 0, -500), new Vec3d(2000, 2000, 0)));
        l.addAll(buildings);
        l.addAll(sidewalks);
        l.addAll(parks);
        l.addAll(treeGenerator.collisionShapes());
        l.addAll(billboards);
        l.addAll(poles);
        l.add(new SurfaceNetShape(iceModel));
        collisionShape = new MultigridShape(l);

        renderable.renderable = createRenderable();
    }

    public Renderable createRenderable() {
        CustomModel intersectionsModel = new CustomModel();
        for (AABB b : intersections) {
            intersectionsModel.addRectangle(b.lower.setZ(b.upper.z), b.size().setY(0).setZ(0), b.size().setX(0).setZ(0),
                    new Vec2d(.5, .5), new Vec2d(b.size().x / 6, 0), new Vec2d(0, b.size().y / 3));
        }
        intersectionsModel.createVAO();

        CustomModel roadsModel = new CustomModel();
        for (AABB b : roads) {
            if (b.size().x >= b.size().y) {
                roadsModel.addRectangle(b.lower.setZ(b.upper.z), b.size().setY(0).setZ(0), b.size().setX(0).setZ(0),
                        new Vec2d(.5, .5), new Vec2d(b.size().x / 6, 0), new Vec2d(0, b.size().y / 3));
            } else {
                roadsModel.addRectangle(b.lower.setZ(b.upper.z), b.size().setX(0).setZ(0), b.size().setY(0).setZ(0),
                        new Vec2d(.5, .5), new Vec2d(b.size().y / 6, 0), new Vec2d(0, b.size().x / 3));
            }
        }
        roadsModel.createVAO();

        CustomModel sidewalksModel = new CustomModel();
        for (AABB b : sidewalks) {
            sidewalksModel.addRectangle(b.lower.setZ(b.upper.z), b.size().setY(0).setZ(0), b.size().setX(0).setZ(0),
                    new Vec2d(0, 0), new Vec2d(b.size().x / 2, 0), new Vec2d(0, b.size().y / 2));
            createWalls(new AABB(b.lower.setZ(0), b.upper), 2, 1, sidewalksModel);
        }
        sidewalksModel.createVAO();

        CustomModel parksModel = new CustomModel();
        for (AABB b : parks) {
            parksModel.addRectangle(b.lower.setZ(b.upper.z), b.size().setY(0).setZ(0), b.size().setX(0).setZ(0),
                    new Vec2d(0, 0), new Vec2d(b.size().x / 2, 0), new Vec2d(0, b.size().y / 2));
            createWalls(new AABB(b.lower.setZ(0), b.upper), 2, 1, parksModel);
        }
        parksModel.createVAO();

        CustomModel roofs = new CustomModel();
        for (AABB b : buildings) {
            roofs.addRectangle(b.lower.setZ(b.upper.z), b.size().setY(0).setZ(0), b.size().setX(0).setZ(0),
                    new Vec2d(0, 0), new Vec2d(b.size().x / 4, 0), new Vec2d(0, b.size().y / 4));
        }
        roofs.createVAO();

        CustomModel[] walls = new CustomModel[NUM_WALL_TYPES];
        for (int i = 0; i < NUM_WALL_TYPES; i++) {
            walls[i] = new CustomModel();
        }
        for (AABB b : buildings) {
            int i = floor(Math.random() * NUM_WALL_TYPES);
            createWalls(b, FLOOR_HEIGHT * WALL_SCALES[i], WALL_SCALES_X[i], walls[i]);
        }
        for (int i = 0; i < NUM_WALL_TYPES; i++) {
            walls[i].createVAO();
        }

        CustomModel billboardsModel = new CustomModel();
        for (AABB b : billboards) {
            billboardsModel.addRectangle(b.lower, b.size().setY(0).setZ(0), b.size().setX(0).setZ(0),
                    new Vec2d(0, 0), new Vec2d(b.size().x / 4, 0), new Vec2d(0, b.size().y / 4));
            billboardsModel.addRectangle(b.lower.setZ(b.upper.z), b.size().setY(0).setZ(0), b.size().setX(0).setZ(0),
                    new Vec2d(0, 0), new Vec2d(b.size().x / 4, 0), new Vec2d(0, b.size().y / 4));
            createWalls(b, 4, 1, billboardsModel);
        }
        billboardsModel.createVAO();

        CustomModel polesModel = new CustomModel();
        for (CapsuleShape c : poles) {
            polesModel.addCylinder(c.pos, c.dir, c.radius, 16, STREET_WIDTH, 1, c.dir.length() / (2 * Math.PI * c.radius));
        }
        polesModel.smoothVertexNormals();
        polesModel.createVAO();

        List<Renderable> parts = new LinkedList();
        parts.add(new PBRModel(intersectionsModel, PBRTexture.loadFromFolder("road_empty")));
        parts.add(new PBRModel(roadsModel, PBRTexture.loadFromFolder("road")));
        parts.add(new PBRModel(sidewalksModel, PBRTexture.loadFromFolder("sidewalk")));
        parts.add(new PBRModel(parksModel, PBRTexture.loadFromFolder("grass")));
        parts.add(new PBRModel(roofs, PBRTexture.loadFromFolder("concrete_floor")));
        // parts.add(new PBRModel(billboardsModel, PBRTexture.loadFromFolder("concrete_pole")));
        parts.add(new PBRModel(polesModel, PBRTexture.loadFromFolder("concrete_pole")));
        for (int i = 0; i < NUM_WALL_TYPES; i++) {
            if (i < WALL_TEXTURES.length) {
                parts.add(new DiffuseModel(walls[i], Texture.load(WALL_TEXTURES[i])));
            } else {
                parts.add(new PBRModel(walls[i], PBRTexture.loadFromFolder(WALL_PBR_TEXTURES[i - WALL_TEXTURES.length])));
            }
        }
        parts.addAll(treeGenerator.renderables());
        return new RenderableList(parts);
    }

    private void createWalls(AABB b, double scale, double scaleX, CustomModel m) {
        for (int j = 0; j < 4; j++) {
            Vec3d dir = DIRS.get(j).mul(b.size());
            Vec3d dir2 = DIRS.get(j < 2 ? j + 2 : 3 - j).mul(b.size());
            Vec3d dir3 = DIRS.get(5).mul(b.size());
            Vec3d v = b.lower.add(b.size().div(2)).add(dir.div(2)).sub(dir2.div(2)).sub(dir3.div(2));
            float texW = (float) (Math.abs(dir2.x + dir2.y + dir2.z) / scale / scaleX);
            float texH = (float) (Math.abs(dir3.x + dir3.y + dir3.z) / scale);
            m.addRectangle(v, dir2, dir3, new Vec2d(0, 0), new Vec2d(texW, 0), new Vec2d(0, texH));
        }
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
