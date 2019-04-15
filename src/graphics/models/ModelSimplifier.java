package graphics.models;

import graphics.models.Vertex.VertexPBR;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.joml.Matrix4d;
import org.joml.Vector4d;
import util.math.Vec3d;

public abstract class ModelSimplifier {

    public static CustomModel simplify(CustomModel m) {
        Map<Vec3d, Vertex> vertices = new HashMap();
        for (VertexPBR v : m.vertices) {
            vertices.put(v.position, new Vertex(v));
        }
        Set<Face> faces = new HashSet();
        for (int i = 0; i < vertices.size(); i += 3) {
            Vertex v1 = vertices.get(m.vertices.get(i).position);
            Vertex v2 = vertices.get(m.vertices.get(i + 1).position);
            Vertex v3 = vertices.get(m.vertices.get(i + 2).position);
            Face f = new Face(v1, v2, v3);
            faces.add(f);
            Quadric q = f.fundamentalQuadric();
            v1.q = v2.q = v3.q = q;
        }
        TreeMap<Edge, Edge> edges = new TreeMap<>(Comparator.comparingDouble(e -> e.contractionError));
        for (Face f : faces) {
            for (Edge e : f.edges()) {
                edges.putIfAbsent(e, e);
                edges.get(e).faces.add(f);
            }
        }
//        for (Vertex v1 : vertices.values()) {
//            for (Vertex v2 : vertices.values()) {
//                if (v1.v.position.sub(v2.v.position).lengthSquared() < .01) {
//                    Edge e = new Edge(v1, v2);
//                    edges.put(e, e);
//                }
//            }
//        }

        while (faces.size() > .5 * m.numTriangles()) {
            Edge e = edges.pollFirstEntry().getKey();
            for (Edge e2 : e.v1.edges) {
                edges.remove(e2);
            }
            for (Edge e2 : e.v2.edges) {
                edges.remove(e2);
            }
            System.out.println(faces.size());
            faces.removeAll(e.faces);

            Set<Vertex> adjacent = new HashSet();
            e.v1.edges.forEach(e2 -> {
                adjacent.add(e2.v1);
                adjacent.add(e2.v2);
            });
            e.v2.edges.forEach(e2 -> {
                adjacent.add(e2.v1);
                adjacent.add(e2.v2);
            });
            adjacent.remove(e.v1);
            adjacent.remove(e.v2);

            Vec3d pos = e.bestPos;
            Vertex newV = new Vertex(new VertexPBR(pos,
                    e.v1.v.texCoord.lerp(e.v2.v.texCoord, .5),
                    e.v1.v.normal.lerp(e.v2.v.normal, .5),
                    e.v1.v.tangent.lerp(e.v2.v.tangent, .5),
                    e.v1.v.bitangent.lerp(e.v2.v.bitangent, .5)));
            newV.q = e.quadric;
            for (Vertex n : adjacent) {
                Edge e2 = new Edge(newV, n);
                edges.put(e2, e2);
            }
            faces.forEach(f -> f.replace(e.v1, newV));
            faces.forEach(f -> f.replace(e.v2, newV));
            for (Face f : faces) {
                for (Edge e2 : f.edges()) {
                    if (edges.get(e2) == null) {
                        System.out.println("bad");
                    }
                    edges.get(e2).faces.add(f);
                }
            }
        }

        CustomModel m2 = new CustomModel();
        faces.forEach(f -> {
            m2.vertices.add(f.v1.v);
            m2.vertices.add(f.v2.v);
            m2.vertices.add(f.v3.v);
        });
        return m2;
    }

    public static class Edge {

        public final Vertex v1, v2;
        public final Set<Face> faces = new HashSet();
        public final Quadric quadric;
        public final Vec3d bestPos;
        public final double contractionError;

        public Edge(Vertex v1, Vertex v2) {
            this.v1 = v1;
            this.v2 = v2;
            quadric = v1.q.add(v2.q);
            bestPos = quadric.minimum(v1.v.position, v2.v.position);
            contractionError = quadric.minVal(v1.v.position, v2.v.position);
            v1.edges.add(this);
            v2.edges.add(this);
        }

        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Edge other = (Edge) obj;
            if (!Objects.equals(this.v1, other.v1)) {
                if (!Objects.equals(this.v1, other.v2)) {
                    return false;
                }
                return Objects.equals(this.v2, other.v1);
            }
            return Objects.equals(this.v2, other.v2);
        }

        @Override
        public String toString() {
            return "Edge{" + "v1=" + v1 + ", v2=" + v2 + '}';
        }
    }

    public static class Face {

        public Vertex v1, v2, v3;

        public Face(Vertex v1, Vertex v2, Vertex v3) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            v1.faces.add(this);
            v2.faces.add(this);
            v3.faces.add(this);
        }

        public List<Edge> edges() {
            return Arrays.asList(new Edge(v1, v2), new Edge(v1, v3), new Edge(v2, v3));
        }

        public Quadric fundamentalQuadric() {
            Vec3d n = v3.v.position.sub(v1.v.position).cross(v2.v.position.sub(v1.v.position)).normalize();
            double d = -v1.v.position.dot(n);
            return new Quadric(n, d);
        }

        public void replace(Vertex vOld, Vertex vNew) {
            if (v1 == vOld) {
                v1 = vNew;
            }
            if (v2 == vOld) {
                v2 = vNew;
            }
            if (v3 == vOld) {
                v3 = vNew;
            }
            v1.faces.add(this);
            v2.faces.add(this);
            v3.faces.add(this);
        }

        @Override
        public String toString() {
            return "Face{" + "v1=" + v1 + ", v2=" + v2 + ", v3=" + v3 + '}';
        }
    }

    public static class Vertex {

        public final VertexPBR v;
        public Quadric q;
        public final Set<Edge> edges = new HashSet();
        public final Set<Face> faces = new HashSet();

        public Vertex(VertexPBR v) {
            this.v = v;
        }

        @Override
        public String toString() {
            return "Vertex{" + "v=" + v + ", q=" + q + '}';
        }
    }

    public static class Quadric {

        public final Matrix4d Q;
        public Matrix4d Qinv;

        public Quadric(Matrix4d Q) {
            this.Q = new Matrix4d(Q);
//            System.out.println(Q.m00() + " " + (Q.m00() * Q.m11() - Q.m01() * Q.m10()) + " "
//                    + Q.determinant3x3() + " " + Q.determinant());
//            Q.setRow(3, new Vector4d(0, 0, 0, 1));
//            if (Math.abs(Q.determinant()) < 1e-12) {
//                Qinv = null;
//            } else {
//                Qinv = Q.invert();
//            }
        }

        public Quadric(Vec3d n, double d) {
            Vector4d p = new Vector4d(n.x, n.y, n.z, d);
            Matrix4d Q = new Matrix4d().set(
                    p.mul(n.x, new Vector4d()), p.mul(n.y, new Vector4d()),
                    p.mul(n.z, new Vector4d()), p.mul(d, new Vector4d()));
            this.Q = new Matrix4d(Q);
//            Q.setRow(3, new Vector4d(0, 0, 0, 1));
//            if (Math.abs(Q.determinant()) < 1e-12) {
//                Qinv = null;
//            } else {
//                Qinv = Q.invert();
//            }
        }

        public Quadric add(Quadric other) {
            return new Quadric(Q.add(other.Q, new Matrix4d()));
        }

        public Vec3d minimum(Vec3d v1, Vec3d v2) {
            if (Qinv == null) {
                return v1.lerp(v2, .5);
            }
            Vector4d v = new Vector4d(0, 0, 0, 1).mul(Qinv);
            // System.out.println(v.w);
            return new Vec3d(v.x, v.y, v.z);
        }

        public double minVal(Vec3d v1, Vec3d v2) {
            Vec3d min = minimum(v1, v2);
            Vector4d v = new Vector4d(min.x, min.y, min.z, 1);
            return Math.abs(v.dot(v.mul(Q, new Vector4d())));
        }

        @Override
        public String toString() {
            return "Quadric{" + "Q=" + Q + '}';
        }
    }
}
