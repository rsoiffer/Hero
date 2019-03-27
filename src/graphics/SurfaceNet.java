package graphics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import physics.AABB;
import static util.math.MathUtils.ceil;
import static util.math.MathUtils.clamp;
import static util.math.MathUtils.floor;
import static util.math.MathUtils.mod;
import util.math.Vec2d;
import util.math.Vec3d;

public class SurfaceNet implements Model {

    private static final double MIN = -1, BOUNDARY = 0, MAX = 1;
    private static final int SUBNET_SIZE = 32;
    private static final int SUBNET_OVERLAP = 2;

    private final double scale;
    private final HashMap<Vec3d, Subnet> subnets = new HashMap();

    public SurfaceNet(double scale) {
        this.scale = scale;
    }

    private double get(int x, int y, int z) {
        Subnet s = subnets.get(new Vec3d(x, y, z).div(SUBNET_SIZE).floor());
        return s == null ? MIN : s.data[mod(x, SUBNET_SIZE)][mod(y, SUBNET_SIZE)][mod(z, SUBNET_SIZE)];
    }

    private Subnet getSubnet(Vec3d v) {
        v = v.div(SUBNET_SIZE).floor();
        if (!subnets.containsKey(v)) {
            subnets.put(v, new Subnet((int) v.x, (int) v.y, (int) v.z));
        }
        return subnets.get(v);
    }

    public void intersectionSDF(SDF sdf, AABB bounds) {
        sdf = sdf.scale(scale);
        int xMin = floor(bounds.lower.x / scale), yMin = floor(bounds.lower.y / scale), zMin = floor(bounds.lower.z / scale);
        int xMax = ceil(bounds.upper.x / scale), yMax = ceil(bounds.upper.y / scale), zMax = ceil(bounds.upper.z / scale);
        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int z = zMin; z <= zMax; z++) {
                    double d = sdf.value(new Vec3d(x, y, z));
                    if (d < MAX) {
                        set(x, y, z, Math.max(get(x, y, z), d));
                    }
                }
            }
        }
    }

    @Override
    public void render() {
        for (Subnet s : subnets.values()) {
            if (s.changed) {
                s.updateModel();
                s.changed = false;
            }
            s.model.render();
        }
    }

    private void set(int x, int y, int z, double d) {
        d = clamp(d, MIN, MAX);
        if (d != get(x, y, z)) {
            int xm = mod(x, SUBNET_SIZE), ym = mod(y, SUBNET_SIZE), zm = mod(z, SUBNET_SIZE);
            for (int x2 = (xm < SUBNET_OVERLAP ? -1 : 0); x2 <= 0; x2++) {
                for (int y2 = (ym < SUBNET_OVERLAP ? -1 : 0); y2 <= 0; y2++) {
                    for (int z2 = (zm < SUBNET_OVERLAP ? -1 : 0); z2 <= 0; z2++) {
                        Vec3d v = new Vec3d(x + SUBNET_OVERLAP * x2, y + SUBNET_OVERLAP * y2, z + SUBNET_OVERLAP * z2);
                        Subnet s = getSubnet(v);
                        s.changed = true;
                        s.data[xm - SUBNET_SIZE * x2][ym - SUBNET_SIZE * y2][zm - SUBNET_SIZE * z2] = d;
                    }
                }
            }
        }
    }

    public void unionSDF(SDF sdf, AABB bounds) {
        sdf = sdf.scale(scale);
        int xMin = floor(bounds.lower.x / scale), yMin = floor(bounds.lower.y / scale), zMin = floor(bounds.lower.z / scale);
        int xMax = ceil(bounds.upper.x / scale), yMax = ceil(bounds.upper.y / scale), zMax = ceil(bounds.upper.z / scale);
        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int z = zMin; z <= zMax; z++) {
                    double d = sdf.value(new Vec3d(x, y, z));
                    if (d > MIN) {
                        set(x, y, z, Math.max(get(x, y, z), d));
                    }
                }
            }
        }
    }

    private class Edge {

        private final int x0, y0, z0, x1, y1, z1;
        private final double d0, d1;

        public Edge(int x0, int y0, int z0, int x1, int y1, int z1, double d0, double d1) {
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.d0 = d0;
            this.d1 = d1;
        }

        private Vec3d crossing() {
            double c = (BOUNDARY - d0) / (d1 - d0);
            return new Vec3d(x0, y0, z0).lerp(new Vec3d(x1, y1, z1), c);
        }

        private List<Vec3d> neighbors() {
            List<Vec3d> r = new LinkedList();
            for (int x = x1 - 1; x <= x0; x++) {
                for (int y = y1 - 1; y <= y0; y++) {
                    for (int z = z1 - 1; z <= z0; z++) {
                        r.add(new Vec3d(x, y, z));
                    }
                }
            }
            return r;
        }

        private boolean surface() {
            return d0 > BOUNDARY != d1 > BOUNDARY;
        }

        @Override
        public String toString() {
            return "Edge{" + "x0=" + x0 + ", y0=" + y0 + ", z0=" + z0 + ", x1=" + x1 + ", y1=" + y1 + ", z1=" + z1 + ", d0=" + d0 + ", d1=" + d1 + '}';
        }
    }

    private class Subnet {

        private final Vec3d subnetPos;
        private final double[][][] data = new double[SUBNET_SIZE + SUBNET_OVERLAP][SUBNET_SIZE + SUBNET_OVERLAP][SUBNET_SIZE + SUBNET_OVERLAP];
        private final CustomModel model = new CustomModel();
        private boolean changed;

        private Subnet(int subnetX, int subnetY, int subnetZ) {
            subnetPos = new Vec3d(subnetX, subnetY, subnetZ).mul(SUBNET_SIZE);
            for (int x = 0; x < SUBNET_SIZE + SUBNET_OVERLAP; x++) {
                for (int y = 0; y < SUBNET_SIZE + SUBNET_OVERLAP; y++) {
                    for (int z = 0; z < SUBNET_SIZE + SUBNET_OVERLAP; z++) {
                        data[x][y][z] = MIN;
                    }
                }
            }
            model.createVAO();
        }

        private void add(List<Edge> edges, Edge e) {
            if (e.surface()) {
                edges.add(e);
            }
        }

        private List<Edge> computeEdges() {
            List<Edge> edges = new LinkedList();
            for (int x = 0; x < SUBNET_SIZE + SUBNET_OVERLAP; x++) {
                for (int y = 0; y < SUBNET_SIZE + SUBNET_OVERLAP; y++) {
                    for (int z = 0; z < SUBNET_SIZE + SUBNET_OVERLAP; z++) {
                        double d = data[x][y][z];
                        if (x + 1 < SUBNET_SIZE + SUBNET_OVERLAP) {
                            add(edges, new Edge(x, y, z, x + 1, y, z, d, data[x + 1][y][z]));
                        }
                        if (y + 1 < SUBNET_SIZE + SUBNET_OVERLAP) {
                            add(edges, new Edge(x, y, z, x, y + 1, z, d, data[x][y + 1][z]));
                        }
                        if (z + 1 < SUBNET_SIZE + SUBNET_OVERLAP) {
                            add(edges, new Edge(x, y, z, x, y, z + 1, d, data[x][y][z + 1]));
                        }
                    }
                }
            }
            return edges;
        }

        private Vec3d[][][] computePoints(List<Edge> edges) {
            Vec3d[][][] points = new Vec3d[SUBNET_SIZE + 1][SUBNET_SIZE + 1][SUBNET_SIZE + 1];
            int[][][] pointCounts = new int[SUBNET_SIZE + 1][SUBNET_SIZE + 1][SUBNET_SIZE + 1];
            for (Edge e : edges) {
                for (Vec3d v : e.neighbors()) {
                    int x = (int) v.x, y = (int) v.y, z = (int) v.z;
                    if (x >= 0 && x <= SUBNET_SIZE && y >= 0 && y <= SUBNET_SIZE && z >= 0 && z <= SUBNET_SIZE) {
                        Vec3d p = points[x][y][z];
                        points[x][y][z] = (p == null ? e.crossing() : p.add(e.crossing()));
                        pointCounts[x][y][z] += 1;
                    }
                }
            }
            for (int x = 0; x <= SUBNET_SIZE; x++) {
                for (int y = 0; y <= SUBNET_SIZE; y++) {
                    for (int z = 0; z <= SUBNET_SIZE; z++) {
                        Vec3d p = points[x][y][z];
                        if (p != null) {
                            points[x][y][z] = p.div(pointCounts[x][y][z]).add(subnetPos).mul(scale);
                        }
                    }
                }
            }
            return points;
        }

        private void updateModel() {
            List<Edge> edges = computeEdges();
            Vec3d[][][] points = computePoints(edges);

            model.clear();
            for (Edge e : edges) {
                if (e.x0 > 0 && e.x0 <= SUBNET_SIZE && e.y0 > 0 && e.y0 <= SUBNET_SIZE && e.z0 > 0 && e.z0 <= SUBNET_SIZE) {
                    List<Vec3d> p = new ArrayList(4);
                    for (int x = e.x1 - 1; x <= e.x0; x++) {
                        for (int y = e.y1 - 1; y <= e.y0; y++) {
                            for (int z = e.z1 - 1; z <= e.z0; z++) {
                                p.add(points[x][y][z]);
                            }
                        }
                    }
                    // List<Vec3d> p = e.neighbors().stream().map(v -> points[(int) v.x][(int) v.y][(int) v.z]).collect(Collectors.toList());
                    if (e.y0 == e.y1 != e.d0 > BOUNDARY) {
                        if (e.x0 == e.x1) {
                            model.addTriangle(p.get(0), new Vec2d(0, 0), p.get(1), new Vec2d(1, 0), p.get(2), new Vec2d(0, 1));
                            model.addTriangle(p.get(3), new Vec2d(1, 1), p.get(2), new Vec2d(0, 1), p.get(1), new Vec2d(1, 0));
                        } else {
                            model.addTriangle(p.get(0), new Vec2d(0, 0), p.get(1), new Vec2d(0, 1), p.get(2), new Vec2d(1, 0));
                            model.addTriangle(p.get(3), new Vec2d(1, 1), p.get(2), new Vec2d(1, 0), p.get(1), new Vec2d(0, 1));
                        }
                    } else {
                        if (e.x0 == e.x1) {
                            model.addTriangle(p.get(0), new Vec2d(0, 0), p.get(2), new Vec2d(0, 1), p.get(1), new Vec2d(1, 0));
                            model.addTriangle(p.get(3), new Vec2d(1, 1), p.get(1), new Vec2d(1, 0), p.get(2), new Vec2d(0, 1));
                        } else {
                            model.addTriangle(p.get(0), new Vec2d(0, 0), p.get(2), new Vec2d(1, 0), p.get(1), new Vec2d(0, 1));
                            model.addTriangle(p.get(3), new Vec2d(1, 1), p.get(1), new Vec2d(0, 1), p.get(2), new Vec2d(1, 0));
                        }
                    }
                }
            }
            model.smoothVertexNormals();
            model.updateVBO();
        }
    }
}
