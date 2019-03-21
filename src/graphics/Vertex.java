package graphics;

import graphics.opengl.BufferObject;
import graphics.opengl.VertexArrayObject;
import java.util.List;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import util.math.Vec2d;
import util.math.Vec3d;

public class Vertex {

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

    public static VertexArrayObject createVAO(List<Vertex> vertices) {
        return VertexArrayObject.createVAO(() -> {
            float[] data = new float[14 * vertices.size()];
            for (int i = 0; i < vertices.size(); i++) {
                System.arraycopy(vertices.get(i).data(), 0, data, 14 * i, 14);
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

    private float[] data() {
        return new float[]{
            (float) position.x, (float) position.y, (float) position.z,
            (float) texCoord.x, (float) texCoord.y,
            (float) normal.x, (float) normal.y, (float) normal.z,
            (float) tangent.x, (float) tangent.y, (float) tangent.z,
            (float) bitangent.x, (float) bitangent.y, (float) bitangent.z
        };
    }

    @Override
    public String toString() {
        return "Vertex{" + "position=" + position + ", texCoord=" + texCoord + ", normal=" + normal + ", tangent=" + tangent + ", bitangent=" + bitangent + '}';
    }
}
