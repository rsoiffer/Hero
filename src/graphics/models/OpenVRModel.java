package graphics.models;

import graphics.models.Vertex.VertexPBR;
import graphics.opengl.BufferObject;
import graphics.opengl.Shader;
import graphics.opengl.Texture;
import graphics.opengl.VertexArrayObject;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.PointerBuffer;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glDrawElements;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER;
import org.lwjgl.openvr.HmdVector3;
import org.lwjgl.openvr.RenderModel;
import org.lwjgl.openvr.RenderModelTextureMap;
import org.lwjgl.openvr.VR;
import org.lwjgl.openvr.VRRenderModels;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import util.math.Vec2d;
import util.math.Vec3d;
import vr.ViveInput.ViveController;

public class OpenVRModel implements Model {

    private static final Shader DIFFUSE_SHADER = Shader.load("geometry_pass_diffuse");

    public final ViveController vc;
    public final Texture diffuseTexture;

    private final int num;
    private final VertexArrayObject vao;
    private final BufferObject ebo;

    public OpenVRModel(ViveController vc) {
        this.vc = vc;

        List<VertexPBR> vertices = new ArrayList();
        List<Integer> indices = new ArrayList();

        RenderModel rm;
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pb = stack.callocPointer(1);
            String renderModelName = vc.getPropertyString(VR.ETrackedDeviceProperty_Prop_RenderModelName_String);
            int success = VRRenderModels.VRRenderModels_LoadRenderModel_Async(renderModelName, null);
            if (success != 1) {
                throw new RuntimeException("Could not load OpenVR render model");
            }
            rm = RenderModel.create(pb.get());
        }
        RenderModelTextureMap rmtm;
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pb = stack.callocPointer(1);
            int success = VRRenderModels.VRRenderModels_LoadTexture_Async(rm.diffuseTextureId(), pb);
            if (success != 1) {
                throw new RuntimeException("Could not load OpenVR render model diffuse texture");
            }
            rmtm = RenderModelTextureMap.create(pb.get());
        }

        rm.rVertexData().forEach(rmv -> vertices.add(new VertexPBR(
                toVec3d(rmv.vPosition()),
                new Vec2d(rmv.rfTextureCoord(0), rmv.rfTextureCoord(1)),
                toVec3d(rmv.vNormal()),
                new Vec3d(0, 0, 0),
                new Vec3d(0, 0, 0))));
        for (int i : rm.IndexData().array()) {
            indices.add(i);
        }

        num = indices.size();
        vao = Vertex.createVAO(vertices, new int[]{3, 2, 3, 3, 3});
        ebo = new BufferObject(GL_ELEMENT_ARRAY_BUFFER, indices.stream().mapToInt(i -> i).toArray());

        diffuseTexture = new Texture(GL_TEXTURE_2D);
        diffuseTexture.setParameter(GL_TEXTURE_MAX_LEVEL, 4);
        diffuseTexture.uploadData(rmtm.unWidth(), rmtm.unHeight(), rmtm.rubTextureMapData(4 * rmtm.unWidth() * rmtm.unHeight()));
    }

    @Override
    public void render() {
        vao.bind();
        glDrawElements(GL_TRIANGLES, num, GL_UNSIGNED_INT, 0);
    }

    private static Vec3d toVec3d(HmdVector3 v) {
        return new Vec3d(v.v(0), v.v(1), v.v(2));
    }
}
