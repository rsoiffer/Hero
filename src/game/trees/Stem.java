package game.trees;

import graphics.PBRTexture;
import graphics.models.CustomModel;
import graphics.renderables.LODPBRModel;
import graphics.renderables.Renderable;
import graphics.renderables.RenderableList;
import static java.lang.Double.NaN;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import physics.CapsuleShape;
import physics.CollisionShape;
import static util.math.MathUtils.ceil;
import static util.math.MathUtils.floor;
import static util.math.MathUtils.lerp;
import util.math.Quaternion;
import util.math.Transformation;
import util.math.Vec2d;
import util.math.Vec3d;

public class Stem {

    public static final double QUALITY = 1;

    private static final PBRTexture bark = PBRTexture.loadFromFolder("bark");
    private static final PBRTexture leaf = PBRTexture.loadFromFolder("leaf_maple");

    private final int Shape = 4;
    private final double BaseSize = 0.2;
    private final int Scale = 23, ScaleV = 5, ZScale = 1, ZScaleV = 0;
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
    public int level;
    public double myLength;
    public double myRadius;
    public double myStems;

    public int numSegments;
    public List<Vec3d> tube;
    public List<Quaternion> tubeDirs;
    public List<Stem> children;

    public List<Vec3d> leaves;
    public List<Quaternion> leafDirs;

    public LODPBRModel renderable;
    public LODPBRModel renderableLeaves;

    // Trunk-only data
    public double lengthBase;

    private Stem(Stem parent, double offsetChild, Vec3d basePos, Quaternion baseDir) {
        this.parent = parent;
        level = parent == null ? 0 : parent.level + 1;

        if (level == 0) {
            double scaleTree = Scale + pm() * ScaleV;
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
            double myDownAngle;
            if (DownAngleV[level] > 0) {
                myDownAngle = DownAngle[level] + pm() * DownAngleV[level];
            } else {
                double ratio = (parent.myLength - offsetChild) / (parent.myLength - parent.lengthBase);
                myDownAngle = DownAngle[level] + 1 * (DownAngleV[level] * (1 - 2 * shapeRatio(0, ratio)));
            }
            baseDir = rotateX(baseDir, myDownAngle);
        }

        if (level == 0) {
            myRadius = myLength * Ratio * Scale0;
        } else {
            myRadius = parent.myRadius * Math.pow(myLength / parent.myLength, RatioPower);
        }

        createTube(basePos, baseDir);

        if (level < Levels - 1) {
            children = new ArrayList();
            int numChildren = floor(myStems + Math.random());
            double angle = Math.random() * 360;
            for (int i = 0; i < numChildren; i++) {
                double z = lerp(level == 0 ? BaseSize : 0, 1, (i + .5) / numChildren);
                angle += Rotate[level + 1] + pm() * RotateV[level + 1];

                Quaternion baseDir2 = rotateZ(getDirZ(z), angle);
                Vec3d basePos2 = getPosZ(z).add(baseDir2.applyTo(new Vec3d(0, -0.5 * radiusZ(z, angle), 0)));
                children.add(new Stem(this, myLength * z, basePos2, baseDir2));
            }
        } else {
            leaves = new ArrayList();
            leafDirs = new ArrayList();
            double leavesPerBranch = Leaves * shapeRatio(4, offsetChild / parent.myLength) * QUALITY;
            int numLeaves = floor(leavesPerBranch + Math.random());
            double angle = Math.random() * 360;
            for (int i = 0; i < numLeaves; i++) {
                double z = (i + .5) / numLeaves;
                angle += Rotate[Math.min(3, level + 1)] + pm() * RotateV[Math.min(3, level + 1)];

                Vec3d basePos2 = getPosZ(z);
                leaves.add(basePos2);
                Quaternion baseDir2 = rotateZ(getDirZ(z), angle);
                baseDir2 = rotateX(baseDir2, DownAngle[Math.min(3, level + 1)] + pm() * DownAngleV[Math.min(3, level + 1)]);
                leafDirs.add(baseDir2);
            }
        }
    }

    private void addToModel(CustomModel model) {
        int detail = 12 / (level + 1);

        List<Vec3d> dirs1 = new ArrayList(), dirs2 = new ArrayList();
        for (int i = 0; i < tube.size(); i++) {
            dirs1.add(tubeDirs.get(Math.min(i, tubeDirs.size() - 1)).applyTo(new Vec3d(1, 0, 0)));
            dirs2.add(tubeDirs.get(Math.min(i, tubeDirs.size() - 1)).applyTo(new Vec3d(0, 1, 0)));
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

    private void addToModelLeaves(CustomModel model) {
        if (leaves != null) {
            for (int i = 0; i < leaves.size(); i++) {
                Vec3d pos = leaves.get(i);
                Quaternion dir = leafDirs.get(i);

                double bend = 1;
                Vec3d normal = dir.applyTo(new Vec3d(1, 0, 0));
                double thetaPosition = Math.atan2(pos.y, pos.x);
                double thetaBend = thetaPosition - Math.atan2(normal.y, normal.x);
                dir = rotateZ(dir, bend * thetaBend);
                double orientation = Math.acos(dir.applyTo(new Vec3d(0, 1, 0)).z);
                normal = dir.applyTo(new Vec3d(1, 0, 0));
                double phiBend = Math.atan2(Math.sqrt(normal.x * normal.x + normal.y + normal.y), normal.z);
                dir = rotateZ(dir, -orientation);
                dir = rotateX(dir, bend * phiBend);
                dir = rotateZ(dir, orientation);

                createLeaf(model, pos, dir);
            }
        }

        if (children != null) {
            children.forEach(c -> c.addToModelLeaves(model));
        }
    }

    private void createLeaf(CustomModel model, Vec3d pos, Quaternion dir) {
        double length = LeafScale / Math.sqrt(QUALITY);
        double width = LeafScale * LeafScaleX / Math.sqrt(QUALITY);

        Vec3d p = pos.add(dir.applyTo(new Vec3d(0, -0.5 * width, 0)));
        Vec3d edge1 = dir.applyTo(new Vec3d(0, width, 0));
        Vec3d edge2 = dir.applyTo(new Vec3d(0, 0, length));
        model.addRectangle(p, edge1, edge2, new Vec2d(150 / 512., (512 - 200) / 512.),
                new Vec2d(120 / 512., 0), new Vec2d(0, 200 / 512.));

//        Vec3d tip = pos.add(dir.applyTo(new Vec3d(0, 0, length)));
//        Vec3d p = pos.add(dir.applyTo(new Vec3d(0, -0.5 * width, length * .25)));
//        Vec3d edge1 = dir.applyTo(new Vec3d(0, width, 0));
//        Vec3d edge2 = dir.applyTo(new Vec3d(0, 0, length * .5));
//        model.addRectangle(p, edge1, edge2, new Vec2d(0, 0), new Vec2d(1, 0), new Vec2d(0, 1));
//        model.addTriangle(pos, new Vec2d(.5, -.5), p, new Vec2d(0, 0), p.add(edge1), new Vec2d(1, 0));
//        model.addTriangle(tip, new Vec2d(.5, 1.5), p.add(edge2), new Vec2d(0, 1), p.add(edge1).add(edge2), new Vec2d(1, 1));
    }

    private void createTube(Vec3d basePos, Quaternion baseDir) {
        numSegments = CurveRes[level];
        tube = new ArrayList();
        tubeDirs = new ArrayList();
        tube.add(basePos);
        Vec3d pos = basePos;
        Quaternion dir = baseDir;
        for (int i = 0; i < numSegments; i++) {
            tubeDirs.add(dir);
            double angle = (Curve[level] + pm() * CurveV[level]) / numSegments;
            dir = rotateX(dir, angle);

            if (level > 1) {
                double declination = Math.acos(dir.applyTo(new Vec3d(0, 0, 1)).z);
                double orientation = Math.acos(dir.applyTo(new Vec3d(0, 1, 0)).z);
                double curveUpSegment = AttractionUp * declination * Math.cos(orientation) / numSegments;
                dir = rotateX(dir, curveUpSegment);
            }

            pos = pos.add(dir.applyTo(new Vec3d(0, 0, myLength / numSegments)));
            tube.add(pos);
        }
    }

    public static Stem generateTree() {
        Quaternion q = Quaternion.fromAngleAxis(Math.random() * 2 * Math.PI, new Vec3d(0, 0, 1));
        return new Stem(null, 0, new Vec3d(0, 0, 0), q);
    }

    private Quaternion getDirZ(double z) {
        z *= numSegments;
        return tubeDirs.get(floor(z));
    }

    private Vec3d getPosZ(double z) {
        z *= numSegments;
        return tube.get(floor(z)).lerp(tube.get(ceil(z)), z - floor(z));
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

    private static Quaternion rotateX(Quaternion q, double angle) {
        return q.mul(Quaternion.fromAngleAxis(Math.toRadians(angle), new Vec3d(1, 0, 0)));
    }

    private static Quaternion rotateY(Quaternion q, double angle) {
        return q.mul(Quaternion.fromAngleAxis(Math.toRadians(angle), new Vec3d(0, 1, 0)));
    }

    private static Quaternion rotateZ(Quaternion q, double angle) {
        return q.mul(Quaternion.fromAngleAxis(Math.toRadians(angle), new Vec3d(0, 0, 1)));
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

    public Stream<CollisionShape> getCollisionShapes(Vec3d pos) {
        if (level >= 2) {
            return Stream.of();
        }
        return Stream.concat(
                IntStream.range(0, numSegments).mapToObj(i -> new CapsuleShape(pos.add(tube.get(i)),
                tube.get(i + 1).sub(tube.get(i)), radiusZ((i + .5) / numSegments, 0))),
                children.stream().flatMap(c -> c.getCollisionShapes(pos))
        );
    }

    public Renderable getRenderable(Vec3d pos) {
        if (renderable == null) {
            CustomModel model = new CustomModel();
            addToModel(model);
//            model.smoothVertexNormals();
            model.createVAO();
            renderable = new LODPBRModel(model, bark, 2);

            CustomModel modelLeaves = new CustomModel();
            addToModelLeaves(modelLeaves);
            modelLeaves.createVAO();
            renderableLeaves = new LODPBRModel(modelLeaves, leaf, 2);
        }

        LODPBRModel m = new LODPBRModel(renderable);
        m.t = Transformation.create(pos, Quaternion.IDENTITY, 1);
        LODPBRModel ml = new LODPBRModel(renderableLeaves);
        ml.t = Transformation.create(pos, Quaternion.IDENTITY, 1);
        return new RenderableList(m, ml);
    }
}
