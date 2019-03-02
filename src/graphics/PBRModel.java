package graphics;

import static graphics.PBRTexture.PBR;
import graphics.opengl.BufferObject;
import static graphics.opengl.GLObject.bindAll;
import graphics.opengl.VertexArrayObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import util.math.Vec2d;
import util.math.Vec3d;

public class PBRModel {

    private final List<Vertex> vertices = new ArrayList();
    private int numVertices;
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

    public void createVAO() {
        numVertices = vertices.size();
        vao = VertexArrayObject.createVAO(() -> {
            float[] data = new float[14 * vertices.size()];
            for (int i = 0; i < vertices.size(); i++) {
                Vertex v = vertices.get(i);
                System.arraycopy(v.data(), 0, data, 14 * i, 14);
            }
            BufferObject vbo = new BufferObject(GL_ARRAY_BUFFER, data);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 14 * 4, 0);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 14 * 4, 12);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(2, 3, GL_FLOAT, false, 14 * 4, 20);
            glEnableVertexAttribArray(2);
            glVertexAttribPointer(3, 3, GL_FLOAT, false, 14 * 4, 32);
            glEnableVertexAttribArray(3);
            glVertexAttribPointer(4, 3, GL_FLOAT, false, 14 * 4, 44);
            glEnableVertexAttribArray(4);
        });
    }

    public void draw(PBRTexture tex) {
        PBRTexture.updateUniforms();
        bindAll(PBR, vao, tex);
        glDrawArrays(GL_TRIANGLES, 0, numVertices);
    }

    public static class Vertex {

        public final Vec3d position;
        public final Vec2d texCoord;
        public final Vec3d normal;
        public final Vec3d tangent;
        public final Vec3d bitangent;

        public Vertex(Vec3d position, Vec2d texCoord, Vec3d normal, Vec3d tangent, Vec3d bitangent) {
            this.position = position;
            this.texCoord = texCoord;
            this.normal = normal;
            this.tangent = tangent;
            this.bitangent = bitangent;
        }

        public float[] data() {
            return new float[]{
                (float) position.x, (float) position.y, (float) position.z,
                (float) texCoord.x, (float) texCoord.y,
                (float) normal.x, (float) normal.y, (float) normal.z,
                (float) tangent.x, (float) tangent.y, (float) tangent.z,
                (float) bitangent.x, (float) bitangent.y, (float) bitangent.z
            };
        }
    }
}
