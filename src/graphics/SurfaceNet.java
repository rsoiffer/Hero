package graphics;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import static util.math.MathUtils.ceil;
import static util.math.MathUtils.clamp;
import static util.math.MathUtils.floor;
import util.math.Vec2d;
import util.math.Vec3d;

public class SurfaceNet implements Model {

    private static final double MIN = -2, BOUNDARY = 0, MAX = 2;
    private static final int SUBNET_SIZE = 32;

    private final double scale;
    private final HashMap<Vec3d, Subnet> subnets = new HashMap();

    public SurfaceNet(double scale) {
        this.scale = scale;
    }

    public void addToAll(double amt) {
        for (Subnet s : subnets.values()) {
            for (Vec3d v : new LinkedList<>(s.data.keySet())) {
                int x = (int) v.x, y = (int) v.y, z = (int) v.z;
                set(x, y, z, get(x, y, z) + amt);
            }
        }
    }

    private double get(int x, int y, int z) {
        Vec3d v = new Vec3d(x, y, z);
        Subnet s = subnets.get(v.div(SUBNET_SIZE).floor());
        return s == null ? MIN : s.data.getOrDefault(v, MIN);
    }

    private Subnet getSubnet(Vec3d v) {
        v = v.div(SUBNET_SIZE).floor();
        if (!subnets.containsKey(v)) {
            subnets.put(v, new Subnet());
        }
        return subnets.get(v);
    }

    public void intersectionSDF(SDF sdf, Vec3d pos, double size) {
        intersectionSDF(sdf, pos.sub(size), pos.add(size));
    }

    public void intersectionSDF(SDF sdf, Vec3d lower, Vec3d upper) {
        sdf = sdf.scale(scale);
        lower = lower.div(scale);
        upper = upper.div(scale);
        for (int x = floor(lower.x); x <= ceil(upper.x); x++) {
            for (int y = floor(lower.y); y <= ceil(upper.y); y++) {
                for (int z = floor(lower.z); z <= ceil(upper.z); z++) {
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
            Vec3d v = new Vec3d(x, y, z);
            for (int i = -1; i <= 1; i += 2) {
                for (int j = -1; j <= 1; j += 2) {
                    for (int k = -1; k <= 1; k += 2) {
                        Subnet s = getSubnet(v.add(new Vec3d(i, j, k)));
                        s.changed = true;
                    }
                }
            }
            Subnet s = getSubnet(v);
            if (d == MIN) {
                s.data.remove(v);
            } else {
                s.data.put(v, d);
            }
        }
    }

    public void unionSDF(SDF sdf, Vec3d pos, double size) {
        unionSDF(sdf, pos.sub(size), pos.add(size));
    }

    public void unionSDF(SDF sdf, Vec3d lower, Vec3d upper) {
        sdf = sdf.scale(scale);
        lower = lower.div(scale);
        upper = upper.div(scale);
        for (int x = floor(lower.x); x <= ceil(upper.x); x++) {
            for (int y = floor(lower.y); y <= ceil(upper.y); y++) {
                for (int z = floor(lower.z); z <= ceil(upper.z); z++) {
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

        private Edge(int x0, int y0, int z0, int x1, int y1, int z1) {
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            d0 = get(x0, y0, z0);
            d1 = get(x1, y1, z1);
        }

        private Edge(int x0, int y0, int z0, int x1, int y1, int z1, double d0) {
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.d0 = d0;
            d1 = get(x1, y1, z1);
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

        private final HashMap<Vec3d, Double> data = new HashMap();
        private final CustomModel model = new CustomModel();
        private boolean changed;

        private Subnet() {
            model.createVAO();
        }

        private List<Edge> computeEdges() {
            List<Edge> edges = new LinkedList();
            for (Entry<Vec3d, Double> e : data.entrySet()) {
                if (Math.abs(e.getValue()) > 1) {
                    continue;
                }
                int x = (int) e.getKey().x, y = (int) e.getKey().y, z = (int) e.getKey().z;
                edges.add(new Edge(x, y, z, x + 1, y, z, e.getValue()));
                edges.add(new Edge(x, y, z, x, y + 1, z, e.getValue()));
                edges.add(new Edge(x, y, z, x, y, z + 1, e.getValue()));
//                if (get(x - 1, y, z) == MIN) {
//                    edges.add(new Edge(x - 1, y, z, x, y, z));
//                }
//                if (get(x, y - 1, z) == MIN) {
//                    edges.add(new Edge(x, y - 1, z, x, y, z));
//                }
//                if (get(x, y, z - 1) == MIN) {
//                    edges.add(new Edge(x, y, z - 1, x, y, z));
//                }
            }
            edges.removeIf(e -> !e.surface());
            return edges;
        }

        private Vec3d computePoint(Vec3d v) {
            int x = (int) v.x, y = (int) v.y, z = (int) v.z;
            List<Edge> edges = new LinkedList();
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    edges.add(new Edge(x, y + i, z + j, x + 1, y + i, z + j));
                    edges.add(new Edge(x + i, y, z + j, x + i, y + 1, z + j));
                    edges.add(new Edge(x + i, y + j, z, x + i, y + j, z + 1));
                }
            }
            edges.removeIf(e -> !e.surface());
            Vec3d p = edges.stream().map(Edge::crossing).reduce(new Vec3d(0, 0, 0), Vec3d::add);
            return p.div(edges.size());
//            return v.add(.5);
        }

        private void updateModel() {
            List<Edge> edges = computeEdges();
            Set<Vec3d> allPoints = edges.stream().flatMap(e -> e.neighbors().stream()).collect(Collectors.toSet());
            HashMap<Vec3d, Vec3d> points = new HashMap();
            for (Vec3d v : allPoints) {
                points.put(v, computePoint(v));
            }

            model.clear();
            for (Edge e : edges) {
                List<Vec3d> p = e.neighbors().stream().map(v -> points.get(v))
                        .map(v -> v.mul(scale)).collect(Collectors.toList());
                if (e.y0 == e.y1 != e.d0 > BOUNDARY) {
                    model.addTriangle(p.get(0), new Vec2d(0, 0), p.get(1), new Vec2d(1, 0), p.get(2), new Vec2d(0, 1));
                    model.addTriangle(p.get(3), new Vec2d(1, 1), p.get(2), new Vec2d(0, 1), p.get(1), new Vec2d(1, 0));
                } else {
                    model.addTriangle(p.get(0), new Vec2d(0, 0), p.get(2), new Vec2d(0, 1), p.get(1), new Vec2d(1, 0));
                    model.addTriangle(p.get(3), new Vec2d(1, 1), p.get(1), new Vec2d(1, 0), p.get(2), new Vec2d(0, 1));
                }
            }
            model.smoothVertexNormals();
            model.updateVBO();
        }
    }
}
