package graphics.models;

import graphics.models.Vertex.VertexPBR;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.joml.Matrix4d;
import org.joml.Vector4d;
import util.math.Vec3d;

public class ModelSimplifier2 {

    private Set<Vertex> allVertices;
    private TreeSet<Edge> allEdges;
    private Set<Face> allFaces;

    public ModelSimplifier2(CustomModel m) {
        allVertices = new HashSet();
        allEdges = new TreeSet<>(Comparator.comparingDouble(e -> e.contractionError));
        allFaces = new HashSet();
        for (int i = 0; i < m.vertices.size(); i += 3) {
            VertexPBR v1 = m.vertices.get(i);
            VertexPBR v2 = m.vertices.get(i + 1);
            VertexPBR v3 = m.vertices.get(i + 2);
            addFace(v1, v2, v3);
        }
        Map<Vec3d, List<Vertex>> posToVertexMap = new HashMap();
        for (Vertex v : allVertices) {
            List<Vertex> l = posToVertexMap.get(v.v.position);
            if (l == null) {
                l = new LinkedList();
                l.add(v);
                posToVertexMap.put(v.v.position, l);
            } else {
                for (Vertex v2 : l) {
                    Edge e = new Edge(v, v2);
                    e.updateQuadric();
                    allEdges.add(e);
                }
                l.add(v);
            }
        }
    }

    public void addFace(VertexPBR p1, VertexPBR p2, VertexPBR p3) {
        Vertex v1 = new Vertex(p1);
        Vertex v2 = new Vertex(p2);
        Vertex v3 = new Vertex(p3);
        Edge e12 = new Edge(v1, v2);
        Edge e13 = new Edge(v1, v3);
        Edge e23 = new Edge(v2, v3);
        Face f = new Face(e12, e13, e23);
        v1.q = v2.q = v3.q = Quadric.fromFace(f);
        e12.updateQuadric();
        e13.updateQuadric();
        e23.updateQuadric();
        allVertices.add(v1);
        allVertices.add(v2);
        allVertices.add(v3);
        allEdges.add(e12);
        allEdges.add(e13);
        allEdges.add(e23);
        allFaces.add(f);
    }

    public void simplify(double amt) {
        double goal = allFaces.size() * amt;
        while (allFaces.size() > goal) {
            if (allEdges.first().contractionError > 1e-2) {
                break;
            }
            simplifyOnce();
        }
    }

    private void simplifyOnce() {
        Edge e = allEdges.pollFirst();
        e.v1.v = new VertexPBR(e.contractionPos,
                e.v1.v.texCoord.lerp(e.v2.v.texCoord, .5),
                e.v1.v.normal.lerp(e.v2.v.normal, .5),
                e.v1.v.tangent.lerp(e.v2.v.tangent, .5),
                e.v1.v.bitangent.lerp(e.v2.v.bitangent, .5));

        for (Edge e2 : new LinkedList<>(e.v2.edges)) {
            e2.replace(e.v2, e.v1);
        }

        Map<Vertex, Edge> v1Edges = new HashMap();
        for (Edge e2 : new LinkedList<>(e.v1.edges)) {
            if (!e2.degenerate) {
                if (v1Edges.containsKey(e2.other(e.v1))) {
                    e2.setDegenerate(false);
                    Edge e3 = v1Edges.get(e2.other(e.v1));
                    for (Face f : new LinkedList<>(e2.faces)) {
                        f.replace(e2, e3);
                    }
                } else {
                    v1Edges.put(e2.other(e.v1), e2);
                    allEdges.remove(e2);
                    e2.updateQuadric();
                    allEdges.add(e2);
                }
            }
        }
        // System.out.println(allEdges.size() + " " + allFaces.size() + " " + e.contractionError);
    }

    public CustomModel toModel() {
        CustomModel m = new CustomModel();
        allFaces.forEach(f -> {
            List<Vertex> l = f.vertices();
            if (l.size() == 3) {
                for (Vertex v : f.vertices()) {
                    m.vertices.add(v.v);
                }
            } else {
                System.out.println(l);
            }
        });
        return m;
    }

    private static class Vertex {

        private static int idMax;
        private int id = idMax++;

        private VertexPBR v;
        private Quadric q;
        private List<Edge> edges = new LinkedList();

        public Vertex(VertexPBR v) {
            this.v = v;
        }

        @Override
        public String toString() {
            return "" + v.position;
        }
    }

    private class Edge {

        private Vertex v1, v2;
        private Quadric q;
        private Vec3d contractionPos;
        private double contractionError;
        private List<Face> faces = new LinkedList();
        private boolean degenerate;

        public Edge(Vertex v1, Vertex v2) {
            if (v1.id > v2.id) {
                this.v1 = v2;
                this.v2 = v1;
            } else {
                this.v1 = v1;
                this.v2 = v2;
            }
            v1.edges.add(this);
            v2.edges.add(this);
        }

        private Vec3d dir() {
            return v2.v.position.sub(v1.v.position);
        }

        private Vec3d midpoint() {
            return v1.v.position.lerp(v2.v.position, .5);
        }

        private Vertex other(Vertex v) {
            if (v == v1) {
                return v2;
            }
            if (v == v2) {
                return v1;
            }
            throw new RuntimeException("Invalid argument");
        }

        private void replace(Vertex vOld, Vertex vNew) {
            if (v1 == vOld) {
                v1.edges.remove(this);
                v1 = vNew;
                v1.edges.add(this);
            }
            if (v2 == vOld) {
                v2.edges.remove(this);
                v2 = vNew;
                v2.edges.add(this);
            }
            if (v1 == v2) {
                setDegenerate(true);
            }
        }

        private void setDegenerate(boolean removeFaces) {
            degenerate = true;
            allEdges.remove(this);
            v1.edges.remove(this);
            v2.edges.remove(this);
            if (removeFaces) {
                allFaces.removeAll(faces);
                for (Face f : new LinkedList<>(faces)) {
                    for (Edge e : f.edges()) {
                        e.faces.remove(f);
                    }
                }
            }
        }

        private void updateQuadric() {
            q = v1.q.add(v2.q);
            contractionPos = q.minimumPos(midpoint());
            contractionError = q.minimumVal(midpoint()) + dir().length() * .01;
        }
    }

    private class Face {

        private Edge e12, e13, e23;

        public Face(Edge e12, Edge e13, Edge e23) {
            this.e12 = e12;
            this.e13 = e13;
            this.e23 = e23;
            e12.faces.add(this);
            e13.faces.add(this);
            e23.faces.add(this);
        }

        private List<Edge> edges() {
            return Arrays.asList(e12, e13, e23);
        }

        private Vec3d normal() {
            return e12.dir().cross(e13.dir()).normalize();
        }

        private void replace(Edge eOld, Edge eNew) {
            if (e12 == eOld) {
                e12.faces.remove(this);
                e12 = eNew;
                e12.faces.add(this);
            }
            if (e13 == eOld) {
                e13.faces.remove(this);
                e13 = eNew;
                e13.faces.add(this);
            }
            if (e23 == eOld) {
                e23.faces.remove(this);
                e23 = eNew;
                e23.faces.add(this);
            }
        }

        private List<Vertex> vertices() {
            return new LinkedList(Stream.of(e12, e13, e23).flatMap(e -> Stream.of(e.v1, e.v2)).collect(Collectors.toSet()));
        }
    }

    private static class Quadric {

        private final Matrix4d Q, Qinv;

        public Quadric(Matrix4d Q) {
            this.Q = Q;
            if (Math.abs(Q.determinant()) < 1e-12) {
                Qinv = null;
            } else {
                Qinv = new Matrix4d(Q);
                Qinv.setRow(3, new Vector4d(0, 0, 0, 1));
                Qinv.invert();
            }
        }

        public Quadric add(Quadric other) {
            return new Quadric(Q.add(other.Q, new Matrix4d()));
        }

        public static Quadric fromFace(Face f) {
            Vec3d n = f.normal();
            double d = -f.e12.v1.v.position.dot(n);
            Vector4d p = new Vector4d(n.x, n.y, n.z, d);
            Matrix4d Q = new Matrix4d().set(
                    p.mul(n.x, new Vector4d()), p.mul(n.y, new Vector4d()),
                    p.mul(n.z, new Vector4d()), p.mul(d, new Vector4d()));
            return new Quadric(Q);
        }

        public Vec3d minimumPos(Vec3d fallback) {
            if (Qinv == null) {
                return fallback;
            }
            Vector4d v = new Vector4d(0, 0, 0, 1).mul(Qinv);
            return new Vec3d(v.x, v.y, v.z);
        }

        public double minimumVal(Vec3d fallback) {
            Vec3d min = minimumPos(fallback);
            Vector4d v = new Vector4d(min.x, min.y, min.z, 1);
            return Math.abs(v.dot(v.mul(Q, new Vector4d())));
        }
    }
}
