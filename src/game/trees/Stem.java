package game.trees;

import graphics.PBRTexture;
import graphics.models.CustomModel;
import graphics.renderables.LODPBRModel;
import graphics.renderables.Renderable;
import static java.lang.Double.NaN;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import util.math.MathUtils;
import static util.math.MathUtils.ceil;
import static util.math.MathUtils.clamp;
import static util.math.MathUtils.floor;
import static util.math.MathUtils.lerp;
import util.math.Quaternion;
import util.math.Transformation;
import util.math.Vec2d;
import util.math.Vec3d;

public class Stem {

    public static final double QUALITY = 1;

    private final int Shape = 4;
    private final double BaseSize = 0.2;
    private final int Scale = 23 * 2, ScaleV = 5, ZScale = 1, ZScaleV = 0;
    private final int Levels = 4;
    private final double Ratio = .015, RatioPower = 1.3;
    private final int Lobes = 3;
    private final double LobeDepth = 0.1;
    private final double Flare = 1;

    private final double Scale0 = 1, Scale0V = 0;
    private final double[] DownAngle = {NaN, 60, 30, 45};
    private final double[] DownAngleV = {NaN, -40, 10, 10};
    private final double[] Rotate = {NaN, 140, 140, 140};
    private final double[] RotateV = {NaN, 0, 0, 0};
    private final double[] Branches = {NaN, 50, 25, 12};
    private final double[] Length = {1, 0.3, 0.6, 0.4};
    private final double[] LengthV = {0, 0.05, 0.1, 0};
    private final double[] Taper = {1.1, 1, 1, 1};
    private final int[] CurveRes = {50, 10, 10, 1};
    private final double[] Curve = {0, 0, -10, 0};
    private final double[] CurveBack = {0, 0, 0, 0};
    private final double[] CurveV = {40, 90, 150, 0};

    private final int Leaves = 6, LeafShape = 0;
    private final double LeafScale = 0.3, LeafScaleX = 0.5;
    private final double AttractionUp = 0.5;

    public Stem parent;
    public int level, n;
    public double myLength;
    public double myRadius;
    public Vec3d xAxis;

    public List<Vec3d> tube;
    public List<Stem> children;
    public LODPBRModel renderable;

    // Trunk-only data
    public double scaleTree;
    public double lengthBase;

    // Branch-only data
    public double myStems;
    public double myDownAngle;

    private Stem(Stem parent, double offsetChild, Vec3d basePos, Vec3d zDir, Vec3d xAxis) {
        this.parent = parent;
        level = parent == null ? 0 : parent.level + 1;
        n = Math.min(level, 3);
        this.xAxis = xAxis;

        if (level == 0) {
            scaleTree = Scale + pm() * ScaleV;
            lengthBase = BaseSize * scaleTree;
            myLength = (Length[level] + pm() * LengthV[level]) * scaleTree;

            myStems = Branches[level + 1];
        } else if (level == 1) {
            double lengthChildMax = Length[level] + pm() * LengthV[level];
            double ratio = (parent.myLength - offsetChild) / (parent.myLength - parent.lengthBase);
            myLength = parent.myLength * lengthChildMax * shapeRatio(Shape, ratio);

            myStems = Branches[level + 1] * (0.2 + 0.8 * (myLength / parent.myLength) / lengthChildMax);
        } else {
            double lengthChildMax = Length[level] + pm() * LengthV[level];
            myLength = lengthChildMax * (parent.myLength - 0.6 * offsetChild);

            if (level < Levels - 1) {
                myStems = Branches[level + 1] * (1.0 - 0.5 * offsetChild / parent.myLength);
            }
        }

        if (level != 0) {
            if (DownAngleV[level] > 0) {
                myDownAngle = DownAngle[level] + pm() * DownAngleV[level];
            } else {
                double ratio = (parent.myLength - offsetChild) / (parent.myLength - parent.lengthBase);
                myDownAngle = DownAngle[level] + 1 * (DownAngleV[level] * (1 - 2 * shapeRatio(0, ratio)));
            }
            zDir = Quaternion.fromAngleAxis(Math.toRadians(myDownAngle), xAxis).applyTo(zDir);
        }

        if (level == 0) {
            myRadius = myLength * Ratio * Scale0;
        } else {
            myRadius = parent.myRadius * Math.pow(myLength / parent.myLength, RatioPower);
        }

        createTube(basePos, zDir.setLength(myLength));

        if (level < Levels - 1) {
            children = new ArrayList();
            int numChildren = floor(myStems + Math.random());
            double angle = Math.random() * 360;
            for (int i = 0; i < numChildren; i++) {
                double offset = lerp(level == 0 ? BaseSize : 0, 1, (i + .5) / numChildren);
                angle += Rotate[level + 1] + pm() * RotateV[level + 1];

                Vec3d xAxis2 = Quaternion.fromAngleAxis(Math.toDegrees(angle), zDir).applyTo(xAxis);
                xAxis2 = getDirZ(offset).cross(xAxis2).cross(getDirZ(offset)).setLength(-1);
                Vec3d basePos2 = getPosZ(offset).add(xAxis2.cross(getDirZ(offset)).setLength(.8 * radiusZ(offset, angle)));
                children.add(new Stem(this, myLength * offset, basePos2, getDirZ(offset), xAxis2));
            }
        } else {
            double leavesPerBranch = Leaves * shapeRatio(4, offsetChild / parent.myLength) * QUALITY;
        }
    }

    private void addToModel(CustomModel model) {
        int detail = 12 / (level + 1);

        Vec3d randomDir = MathUtils.randomInSphere(new Random());
        randomDir = randomDir.projectAgainst(tube.get(tube.size() - 1).sub(tube.get(0)));
        List<Vec3d> dirs1 = new ArrayList(), dirs2 = new ArrayList();
        for (int i = 0; i < tube.size(); i++) {
            Vec3d dir0 = tube.get(Math.min(i + 1, tube.size() - 1)).sub(tube.get(Math.max(i - 1, 0))).normalize();
            Vec3d dir1 = dir0.cross(randomDir).normalize();
            Vec3d dir2 = dir0.cross(dir1).normalize();
            dirs1.add(dir1);
            dirs2.add(dir2);
        }
        double texW = Math.max(1, floor(2 * Math.PI * radiusZ(0, 0) / 2));
        double texH = 0;
        for (int i = 0; i < tube.size() - 1; i++) {
            double z0 = (double) i / (tube.size() - 1);
            double z1 = (double) (i + 1) / (tube.size() - 1);
            double dTexH = tube.get(i + 1).sub(tube.get(i)).length() / 4;
            for (int j = 0; j < detail; j++) {
                double angle0 = j * 2 * Math.PI / detail, angle1 = (j + 1) * 2 * Math.PI / detail;
                Vec3d v00 = tube.get(i).add(dirs1.get(i).mul(Math.cos(angle0) * radiusZ(z0, angle0)))
                        .add(dirs2.get(i).mul(Math.sin(angle0) * radiusZ(z0, angle0)));
                Vec3d v01 = tube.get(i).add(dirs1.get(i).mul(Math.cos(angle1) * radiusZ(z0, angle1))
                        .add(dirs2.get(i).mul(Math.sin(angle1) * radiusZ(z0, angle1))));
                Vec3d v10 = tube.get(i + 1).add(dirs1.get(i + 1).mul(Math.cos(angle0) * radiusZ(z1, angle0)))
                        .add(dirs2.get(i + 1).mul(Math.sin(angle0) * radiusZ(z1, angle0)));
                Vec3d v11 = tube.get(i + 1).add(dirs1.get(i + 1).mul(Math.cos(angle1) * radiusZ(z1, angle1)))
                        .add(dirs2.get(i + 1).mul(Math.sin(angle1) * radiusZ(z1, angle1)));

                Vec2d uv = new Vec2d(texW * j / detail, texH), uvd1 = new Vec2d(texW / detail, 0), uvd2 = new Vec2d(0, dTexH);
                model.addTriangle(v00, uv, v01, uv.add(uvd1), v11, uv.add(uvd1).add(uvd2));
                model.addTriangle(v00, uv, v11, uv.add(uvd1).add(uvd2), v10, uv.add(uvd2));
            }
            texH += dTexH;
        }

        if (children != null) {
            children.forEach(c -> c.addToModel(model));
        }
    }

    private void createTube(Vec3d basePos, Vec3d baseDir) {
        tube = new ArrayList();
        tube.add(basePos);
        Vec3d pos = basePos, dir = baseDir;
        for (int i = 0; i < CurveRes[level]; i++) {
            double angle = Math.toRadians((Curve[level] + pm() * CurveV[level]) / CurveRes[level]);
            dir = Quaternion.fromAngleAxis(angle, xAxis).applyTo(dir);
            pos = pos.add(dir.mul(1. / CurveRes[level]));
            tube.add(pos);
        }
    }

    public static Stem generateTree() {
        Vec3d xAxis = Quaternion.fromAngleAxis(Math.random() * 2 * Math.PI, new Vec3d(0, 0, 1)).applyToForwards();
        return new Stem(null, 0, new Vec3d(0, 0, 0), new Vec3d(0, 0, 1), xAxis);
    }

    private Vec3d getDirZ(double z) {
        z *= tube.size() - 1;
        z = clamp(z, 0, tube.size() - 1);
        return tube.get(ceil(z)).sub(tube.get(ceil(z - 1))).normalize();
    }

    private Vec3d getPosZ(double z) {
        z *= tube.size() - 1;
        z = clamp(z, 0, tube.size() - 1);
        return tube.get(ceil(z - 1)).lerp(tube.get(ceil(z)), ceil(z) - z);
    }

    private static double pm() {
        return 2 * Math.random() - 1;
    }

    private double radiusZ(double z, double angle) {
        double unitTaper;
        if (Taper[level] < 1) {
            unitTaper = Taper[level];
        } else if (Taper[level] < 2) {
            unitTaper = 2 - Taper[level];
        } else {
            unitTaper = 0;
        }
        double taperZ = myRadius * (1 - unitTaper * z);
        double radiusZ;
        if (Taper[level] < 1) {
            radiusZ = taperZ;
        } else {
            double z2 = (1 - z) * myLength;
            double depth = (Taper[level] < 2) || (z2 < taperZ) ? 1 : Taper[level] - 2;
            double z3 = Taper[level] < 2 ? z2 : Math.abs(z2 - 2 * taperZ * floor(z2 / (2 * taperZ) + 0.5));
            radiusZ = (Taper[level] < 2) && (z3 >= taperZ) ? taperZ : (1 - depth) * taperZ
                    + depth * Math.sqrt(taperZ * taperZ - (z3 - taperZ) * (z3 - taperZ));
        }

        if (level == 0) {
            double y = 1 - 8 * z;
            double flareZ = Flare * (Math.pow(100, y) - 1) / 100 + 1;
            radiusZ *= flareZ;

            double lobeZ = 1.0 + LobeDepth * Math.sin(Lobes * angle);
            radiusZ *= lobeZ;

            radiusZ *= Scale0 + pm() * Scale0V;
        }

        return radiusZ;
    }

    private static double shapeRatio(int shape, double ratio) {
        switch (shape) {
            case 0:
                return 0.2 + 0.8 * ratio;
            case 4:
                return 0.5 + 0.5 * ratio;
            case 5:
                return ratio <= 0.7 ? (ratio / 0.7) : ((1.0 - ratio) / 0.3);
            default:
                throw new RuntimeException("Unknown shape " + shape);
        }
    }

    public Renderable getRenderable(Vec3d pos) {
        if (renderable == null) {
            CustomModel model = new CustomModel();
            addToModel(model);
//            model.smoothVertexNormals();
            model.createVAO();
            renderable = new LODPBRModel(model, PBRTexture.loadFromFolder("bark"), 2);
            for (CustomModel m2 : renderable.modelLODS) {
                System.out.println(m2.numTriangles());
            }
        }

        LODPBRModel m = new LODPBRModel(renderable);
        m.t = Transformation.create(pos, Quaternion.IDENTITY, 1);
        return m;
    }
}
