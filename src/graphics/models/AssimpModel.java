package graphics.models;

import graphics.models.Vertex.VertexPBR;
import graphics.opengl.BufferObject;
import graphics.opengl.VertexArrayObject;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import static org.lwjgl.assimp.Assimp.aiGetErrorString;
import static org.lwjgl.assimp.Assimp.aiImportFile;
import static org.lwjgl.assimp.Assimp.aiProcess_CalcTangentSpace;
import static org.lwjgl.assimp.Assimp.aiProcess_GenNormals;
import static org.lwjgl.assimp.Assimp.aiProcess_JoinIdenticalVertices;
import static org.lwjgl.assimp.Assimp.aiProcess_Triangulate;
import static org.lwjgl.assimp.Assimp.aiReleaseImport;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glDrawElements;
import static org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER;
import util.math.Vec2d;
import util.math.Vec3d;

public class AssimpModel implements Model {

    private final int num;
    private final VertexArrayObject vao;
    private final BufferObject ebo;

    public AssimpModel(AIScene scene) {
        List<VertexPBR> vertices = new ArrayList();
        List<Integer> indices = new ArrayList();

        for (int i = 0; i < scene.mNumMeshes(); i++) {
            AIMesh mesh = AIMesh.create(scene.mMeshes().get(i));
            System.out.println(mesh.mMaterialIndex());
            if (mesh.mTextureCoords(0) == null) {
                System.err.println("Warning: Skipping submesh because mTextureCoords(0) is null");
                continue;
            }
            int vertexOffset = vertices.size();
            for (int j = 0; j < mesh.mNumVertices(); j++) {
                vertices.add(new VertexPBR(
                        toVec3d(mesh.mVertices().get(j)),
                        toVec2d(mesh.mTextureCoords(0).get(j)),
                        toVec3d(mesh.mNormals().get(j)),
                        toVec3d(mesh.mTangents().get(j)),
                        toVec3d(mesh.mBitangents().get(j))
                ));
            }
            mesh.mFaces().forEach(aiFace -> {
                indices.add(vertexOffset + aiFace.mIndices().get(0));
                indices.add(vertexOffset + aiFace.mIndices().get(1));
                indices.add(vertexOffset + aiFace.mIndices().get(2));
            });
        }

        num = indices.size();
        vao = Vertex.createVAO(vertices, new int[]{3, 2, 3, 3, 3});
        ebo = new BufferObject(GL_ELEMENT_ARRAY_BUFFER, indices.stream().mapToInt(i -> i).toArray());
    }

    public static AssimpModel load(String name) {
        int flags = aiProcess_JoinIdenticalVertices
                | aiProcess_Triangulate
                | aiProcess_GenNormals
                | aiProcess_CalcTangentSpace;
        AIScene scene = aiImportFile("models/" + name, flags);
        if (scene == null) {
            throw new RuntimeException(aiGetErrorString());
        }
        AssimpModel m = new AssimpModel(scene);
        aiReleaseImport(scene);
        return m;
    }

    @Override
    public void render() {
        vao.bind();
        glDrawElements(GL_TRIANGLES, num, GL_UNSIGNED_INT, 0);
    }

    private static Vec2d toVec2d(AIVector3D v) {
        return new Vec2d(v.x(), v.y());
    }

    private static Vec3d toVec3d(AIVector3D v) {
        return new Vec3d(v.x(), v.y(), v.z());
    }
}
