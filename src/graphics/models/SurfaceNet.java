package graphics.models;

import graphics.SDF;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
                    double d = quantize(sdf.value(new Vec3d(x, y, z)));
                    if (d < MAX && d < get(x, y, z)) {
                        set(x, y, z, d);
                    }
                }
            }
        }
    }

    private static double quantize(double d) {
        return (floor(d * 32) + .5) / 32;
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
                        getSubnet(v).set(xm - SUBNET_SIZE * x2, ym - SUBNET_SIZE * y2, zm - SUBNET_SIZE * z2, d);
                    }
                }
            }
        }
    }

    public void unionSDF(SDF sdf, AABB bounds) {
        sdf = sdf.scale(scale);
        int xMin = floor(bounds.lower.x / scale), yMin = floor(bounds.lower.y / scale), zMin = floor(bounds.lower.z / scale);
        int xMax = ceil(bounds.upper.x / scale), yMax = ceil(bounds.upper.y / scale), zMax = ceil(bounds.upper.z / scale);
        int c1 = 0, c2 = 0;
        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int z = zMin; z <= zMax; z++) {
                    double d = quantize(sdf.value(new Vec3d(x, y, z)));
                    if (d > MIN && d > get(x, y, z)) {
                        set(x, y, z, d);
                        c2++;
                    }
                    c1++;
                }
            }
        }
        System.out.println(c1 + " " + c2);
    }

    private class Subnet {

        private final Vec3d ZERO = new Vec3d(0, 0, 0);

        private final Vec3d subnetPos;
        private final double[][][] data = new double[SUBNET_SIZE + 2][SUBNET_SIZE + 2][SUBNET_SIZE + 2];
        private final Set<Edge> surfaceEdges = new HashSet();
        private final CustomModel model = new CustomModel();
        private boolean changed;

        private final Vec3d[][][] points = new Vec3d[SUBNET_SIZE + 1][SUBNET_SIZE + 1][SUBNET_SIZE + 1];
        private final int[][][] pointCounts = new int[SUBNET_SIZE + 1][SUBNET_SIZE + 1][SUBNET_SIZE + 1];

        private Subnet(int subnetX, int subnetY, int subnetZ) {
            subnetPos = new Vec3d(subnetX, subnetY, subnetZ).mul(SUBNET_SIZE);
            for (int x = 0; x < SUBNET_SIZE + 2; x++) {
                for (int y = 0; y < SUBNET_SIZE + 2; y++) {
                    for (int z = 0; z < SUBNET_SIZE + 2; z++) {
                        data[x][y][z] = MIN;
                        if (x < SUBNET_SIZE + 1 && y < SUBNET_SIZE + 1 && z < SUBNET_SIZE + 1) {
                            points[x][y][z] = ZERO;
                        }
                    }
                }
            }
            model.createVAO();
        }

        private void set(int x, int y, int z, double d) {
            if (x > 0) {
                updateEdge(x, y, z, x - 1, y, z, d);
            }
            if (x < SUBNET_SIZE + 1) {
                updateEdge(x, y, z, x + 1, y, z, d);
            }
            if (y > 0) {
                updateEdge(x, y, z, x, y - 1, z, d);
            }
            if (y < SUBNET_SIZE + 1) {
                updateEdge(x, y, z, x, y + 1, z, d);
            }
            if (z > 0) {
                updateEdge(x, y, z, x, y, z - 1, d);
            }
            if (z < SUBNET_SIZE + 1) {
                updateEdge(x, y, z, x, y, z + 1, d);
            }
            data[x][y][z] = d;
            changed = true;
        }

        private void updateEdge(int x0, int y0, int z0, int x1, int y1, int z1, double newD0) {
            double oldD0 = data[x0][y0][z0];
            double d1 = data[x1][y1][z1];
            boolean oldSurface = oldD0 > BOUNDARY != d1 > BOUNDARY;
            boolean newSurface = newD0 > BOUNDARY != d1 > BOUNDARY;

            if (oldSurface || newSurface) {
                Vec3d oldCrossing = oldSurface ? new Vec3d(x0, y0, z0).lerp(new Vec3d(x1, y1, z1), (BOUNDARY - oldD0) / (d1 - oldD0)) : ZERO;
                Vec3d newCrossing = newSurface ? new Vec3d(x0, y0, z0).lerp(new Vec3d(x1, y1, z1), (BOUNDARY - newD0) / (d1 - newD0)) : ZERO;
                Vec3d modCrossing = newCrossing.sub(oldCrossing);
                int modCount = (oldSurface ? -1 : 0) + (newSurface ? 1 : 0);

                for (int x = Math.max(x0, x1) - 1; x <= Math.min(x0, x1); x++) {
                    for (int y = Math.max(y0, y1) - 1; y <= Math.min(y0, y1); y++) {
                        for (int z = Math.max(z0, z1) - 1; z <= Math.min(z0, z1); z++) {
                            if (x >= 0 && x < SUBNET_SIZE + 1 && y >= 0 && y < SUBNET_SIZE + 1 && z >= 0 && z < SUBNET_SIZE + 1) {
                                points[x][y][z] = points[x][y][z].add(modCrossing);
                                pointCounts[x][y][z] += modCount;
                            }
                        }
                    }
                }
                Edge e;
                if (x0 > x1 || y0 > y1 || z0 > z1) {
                    e = new Edge(x1, y1, z1, x0, y0, z0, d1, newD0);
                } else {
                    e = new Edge(x0, y0, z0, x1, y1, z1, newD0, d1);
                }
                if (!newSurface) {
                    surfaceEdges.remove(e);
                }
                if (!oldSurface) {
                    surfaceEdges.add(e);
                }
            }
        }

        private void updateModel() {
            model.clear();
            for (Edge e : surfaceEdges) {
                if (e.x0 > 0 && e.x0 <= SUBNET_SIZE && e.y0 > 0 && e.y0 <= SUBNET_SIZE && e.z0 > 0 && e.z0 <= SUBNET_SIZE) {
                    e.addToModel();
                }
            }
            model.smoothVertexNormals();
            model.updateVBO();
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

            private void addToModel() {
                List<Vec3d> p = new ArrayList(4);
                for (int x = x1 - 1; x <= x0; x++) {
                    for (int y = y1 - 1; y <= y0; y++) {
                        for (int z = z1 - 1; z <= z0; z++) {
                            p.add(points[x][y][z].div(pointCounts[x][y][z]).add(subnetPos).mul(scale));
                        }
                    }
                }
                if (y0 == y1 != d0 > BOUNDARY) {
                    if (x0 == x1) {
                        model.addTriangle(p.get(0), new Vec2d(0, 0), p.get(1), new Vec2d(1, 0), p.get(2), new Vec2d(0, 1));
                        model.addTriangle(p.get(3), new Vec2d(1, 1), p.get(2), new Vec2d(0, 1), p.get(1), new Vec2d(1, 0));
                    } else {
                        model.addTriangle(p.get(0), new Vec2d(0, 0), p.get(1), new Vec2d(0, 1), p.get(2), new Vec2d(1, 0));
                        model.addTriangle(p.get(3), new Vec2d(1, 1), p.get(2), new Vec2d(1, 0), p.get(1), new Vec2d(0, 1));
                    }
                } else {
                    if (x0 == x1) {
                        model.addTriangle(p.get(0), new Vec2d(0, 0), p.get(2), new Vec2d(0, 1), p.get(1), new Vec2d(1, 0));
                        model.addTriangle(p.get(3), new Vec2d(1, 1), p.get(1), new Vec2d(1, 0), p.get(2), new Vec2d(0, 1));
                    } else {
                        model.addTriangle(p.get(0), new Vec2d(0, 0), p.get(2), new Vec2d(1, 0), p.get(1), new Vec2d(0, 1));
                        model.addTriangle(p.get(3), new Vec2d(1, 1), p.get(1), new Vec2d(0, 1), p.get(2), new Vec2d(1, 0));
                    }
                }
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                final Edge other = (Edge) obj;
                if (this.x0 != other.x0) {
                    return false;
                }
                if (this.y0 != other.y0) {
                    return false;
                }
                if (this.z0 != other.z0) {
                    return false;
                }
                if (this.x1 != other.x1) {
                    return false;
                }
                if (this.y1 != other.y1) {
                    return false;
                }
                if (this.z1 != other.z1) {
                    return false;
                }
                return true;
            }

            @Override
            public int hashCode() {
                int hash = 7;
                hash = 97 * hash + this.x0;
                hash = 97 * hash + this.y0;
                hash = 97 * hash + this.z0;
                hash = 97 * hash + this.x1;
                hash = 97 * hash + this.y1;
                hash = 97 * hash + this.z1;
                return hash;
            }
        }
    }
}
