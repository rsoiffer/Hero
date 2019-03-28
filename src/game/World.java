package game;

import graphics.CustomModel;
import static graphics.passes.GeometryPass.SHADER_DIFFUSE;
import static graphics.passes.GeometryPass.SHADER_PBR;
import graphics.PBRTexture;
import graphics.Renderable;
import graphics.opengl.Texture;
import static graphics.voxels.VoxelRenderer.DIRS;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import physics.AABB;
import util.Noise;
import static util.math.MathUtils.floor;
import util.math.Transformation;
import util.math.Vec2d;
import util.math.Vec3d;

public class World implements Renderable {

    public static final double FLOOR_HEIGHT = 4;
    public static final double BUILDING_SIZE = 32;
    public static final double STREET_WIDTH = 20;

    private static final String[] WALL_FILES = {"tower.png", "glass_0.png", "glass_1.png",
        "highrise_0.png", "highrise_1.png", "highrise_2.png", "highrise_3.png", "highrise_4.png"};
    private static final double[] WALL_SCALES = {2, 9, 3, 8, 4, 3, 3, 4};
    private static final Texture[] WALL_TEXTURES = new Texture[WALL_FILES.length];

    static {
        for (int i = 0; i < WALL_FILES.length; i++) {
            WALL_TEXTURES[i] = Texture.load(WALL_FILES[i]);
        }
    }
    private static final PBRTexture brick = PBRTexture.loadFromFolder("brick");
    private static final PBRTexture concrete = PBRTexture.loadFromFolder("concrete");
    private static final PBRTexture concreteFloor = PBRTexture.loadFromFolder("concrete_floor");
    private static final PBRTexture obsidian = PBRTexture.loadFromFolder("obsidian");
    private static final PBRTexture road = PBRTexture.loadFromFolder("road");
    private static final PBRTexture sidewalk = PBRTexture.loadFromFolder("sidewalk");
    private static final PBRTexture snow = PBRTexture.loadFromFolder("snow");
    private static final PBRTexture whiteBrick = PBRTexture.loadFromFolder("white_brick");

    public List<AABB> buildings = new ArrayList();
    private Noise colorNoise = new Noise(new Random());
    private Noise heightNoise = new Noise(new Random());
    private CustomModel ground, roofs;
    private CustomModel[] walls = new CustomModel[WALL_FILES.length];

    public World() {
        for (int i = 0; i < 2000; i += 2 * BUILDING_SIZE + STREET_WIDTH) {
            for (int j = 0; j < 2000; j += 8 * BUILDING_SIZE + STREET_WIDTH) {
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

        ground = new CustomModel();
        for (AABB b : buildings) {
            if (b.upper.z == 0) {
                ground.addRectangle(b.lower.setZ(b.upper.z), b.size().setY(0).setZ(0), b.size().setX(0).setZ(0),
                        new Vec2d(0, 0), new Vec2d(b.size().x / 4, 0), new Vec2d(0, b.size().y / 4));
            }
        }
        ground.createVAO();

        roofs = new CustomModel();
        for (AABB b : buildings) {
            if (b.upper.z != 0) {
                roofs.addRectangle(b.lower.setZ(b.upper.z), b.size().setY(0).setZ(0), b.size().setX(0).setZ(0),
                        new Vec2d(0, 0), new Vec2d(b.size().x / 4, 0), new Vec2d(0, b.size().y / 4));
            }
        }
        roofs.createVAO();

        for (int i = 0; i < walls.length; i++) {
            walls[i] = new CustomModel();
        }
        for (AABB b : buildings) {
            int i = floor(Math.random() * walls.length);
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
        for (int i = 0; i < walls.length; i++) {
            walls[i].createVAO();
        }
    }

    @Override
    public void renderGeom() {
        SHADER_PBR.bind();
        setTransform(Transformation.IDENTITY);
        sidewalk.bind();
        ground.render();
        concreteFloor.bind();
        roofs.render();
        SHADER_DIFFUSE.bind();
        SHADER_DIFFUSE.setUniform("metallic", 0.0f);
        SHADER_DIFFUSE.setUniform("roughness", 0.8f);
        setTransform(Transformation.IDENTITY);
        for (int i = 0; i < walls.length; i++) {
            WALL_TEXTURES[i].bind();
            walls[i].render();
        }
    }

    @Override
    public void renderShadow() {
        setTransform(Transformation.IDENTITY);
        ground.render();
        roofs.render();
        for (int i = 0; i < walls.length; i++) {
            walls[i].render();
        }
    }
}
