package graphics;

import graphics.opengl.BufferObject;
import static graphics.opengl.GLObject.bindAll;
import graphics.opengl.VertexArrayObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import util.math.Vec2d;
import util.math.Vec3d;

public class CustomModel {

    private final List<Vertex> vertices = new ArrayList();
    private int numVertices;
    private BufferObject vbo;
    private VertexArrayObject vao;

    public void addRectangle(Vec3d p, Vec3d edge1, Vec3d edge2, Vec2d uv, Vec2d uvd1, Vec2d uvd2) {
        addTriangle(p, uv, p.add(edge1), uv.add(uvd1), p.add(edge1).add(edge2), uv.add(uvd1).add(uvd2));
        addTriangle(p, uv, p.add(edge1).add(edge2), uv.add(uvd1).add(uvd2), p.add(edge2), uv.add(uvd2));
    }

    public void addTriangle(Vec3d p1, Vec2d uv1, Vec3d p2, Vec2d uv2, Vec3d p3, Vec2d uv3) {
        Vec3d edge1 = p2.sub(p1), edge2 = p3.sub(p1);
        Vec2d duv1 = uv2.sub(uv1), duv2 = uv3.sub(uv1);
        Vec3d normal = edge1.cross(edge2).normalize();
        Vec3d tangent = edge1.mul(duv2.y).add(edge2.mul(-duv1.y)).normalize();
        Vec3d bitangent = edge1.mul(-duv2.x).add(edge2.mul(duv1.x)).normalize();
        vertices.addAll(Arrays.asList(
                new Vertex(p1, uv1, normal, tangent, bitangent),
                new Vertex(p2, uv2, normal, tangent, bitangent),
                new Vertex(p3, uv3, normal, tangent, bitangent)
        ));
    }

    public void clear() {
        vertices.clear();
    }

    public void createVAO() {
        numVertices = vertices.size();
        vbo = Vertex.createVBO(vertices);
        vao = Vertex.createVAO(vbo);
    }

    public void draw() {
        bindAll(vao);
        glDrawArrays(GL_TRIANGLES, 0, numVertices);
    }

    public void smoothVertexNormals() {
        HashMap<Vec3d, Vec3d> normals = new HashMap();
        for (Vertex v : vertices) {
            normals.compute(v.position, (key, val) -> val == null ? v.normal : val.add(v.normal));
        }
        for (int i = 0; i < vertices.size(); i++) {
            Vertex v = vertices.get(i);
            Vertex v2 = new Vertex(v.position, v.texCoord, normals.get(v.position).normalize(), v.tangent, v.bitangent);
            vertices.set(i, v2);
        }
    }

    public void updateVBO() {
        numVertices = vertices.size();
        Vertex.fillVBO(vbo, vertices);
    }
}
