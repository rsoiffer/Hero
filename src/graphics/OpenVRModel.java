package graphics;

import graphics.opengl.BufferObject;
import static graphics.opengl.GLObject.bindAll;
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
import util.math.Transformation;
import util.math.Vec2d;
import util.math.Vec3d;
import vr.ViveInput.ViveController;

public class OpenVRModel implements Renderable {

    private static final Shader DIFFUSE_SHADER = Shader.load("geometry_pass_diffuse");

    private final ViveController vc;
    private final int num;
    private final VertexArrayObject vao;
    private final BufferObject ebo;
    private final Texture diffuseTexture;

    public OpenVRModel(ViveController vc) {
        this.vc = vc;

        List<Vertex> vertices = new ArrayList();
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

        rm.rVertexData().forEach(rmv -> vertices.add(new Vertex(
                toVec3d(rmv.vPosition()),
                new Vec2d(rmv.rfTextureCoord(0), rmv.rfTextureCoord(1)),
                toVec3d(rmv.vNormal()),
                new Vec3d(0, 0, 0),
                new Vec3d(0, 0, 0))));
        for (int i : rm.IndexData().array()) {
            indices.add(i);
        }

        num = indices.size();
        vao = Vertex.createVAO(vertices);
        ebo = new BufferObject(GL_ELEMENT_ARRAY_BUFFER, indices.stream().mapToInt(i -> i).toArray());

        diffuseTexture = new Texture(GL_TEXTURE_2D);
        diffuseTexture.setParameter(GL_TEXTURE_MAX_LEVEL, 4);
        diffuseTexture.uploadData(rmtm.unWidth(), rmtm.unHeight(), rmtm.rubTextureMapData(4 * rmtm.unWidth() * rmtm.unHeight()));
    }

    @Override
    public void bindGeomShader() {
        bindAll(DIFFUSE_SHADER, diffuseTexture);
    }

    @Override
    public Transformation getTransform() {
        return new Transformation(vc.pose());
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
