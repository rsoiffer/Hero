package graphics;

import graphics.opengl.GLObject;
import graphics.opengl.Texture;

public class PBRTexture extends GLObject {

    // public static final ShaderProgram PBR = Resources.loadShaderProgram("pbr");
//    static {
//        PBR.setUniform("albedoMap", 0);
//        PBR.setUniform("normalMap", 1);
//        PBR.setUniform("metallicMap", 2);
//        PBR.setUniform("roughnessMap", 3);
//        PBR.setUniform("aoMap", 4);
//        PBR.setUniform("heightMap", 5);
//        PBR.setUniform("shadowMap", 6);
////        PBR.setUniform("lightPositions[0]", new Vec3d(-10, -10, 1000));
////        PBR.setUniform("lightColors[0]", new Vec3d(1500000, 1500000, 1500000));
////        PBR.setUniform("lightPositions[1]", new Vec3d(0, 0, 10));
////        PBR.setUniform("lightColors[1]", new Vec3d(2500, 1500, 1500));
//    }
//    public static void updateUniforms() {
//        PBR.setUniform("projection", Camera.current.projectionMatrix());
//        PBR.setUniform("view", Camera.current.viewMatrix());
//        PBR.setUniform("model", new Matrix4d());
//        Vector4d v = new Vector4d(0, 0, 0, 1).mul(Camera.current.viewMatrix().invert());
//        PBR.setUniform("camPos", new Vec3d(v.x, v.y, v.z));
//
//        PBR.setUniform("heightScale", (float) -.02);
//        PBR.setUniform("heightOffset", (float) .5);
//    }
    public final Texture albedo, normal, metallic, roughness, ao, height;

    public PBRTexture(String name) {
        super(0);
        albedo = Texture.load(name + "/albedo.png");
        albedo.num = 0;
        normal = Texture.load(name + "/normal.png");
        normal.num = 1;
        metallic = Texture.load(name + "/metallic.png");
        metallic.num = 2;
        roughness = Texture.load(name + "/roughness.png");
        roughness.num = 3;
        ao = Texture.load(name + "/ao.png");
        ao.num = 4;
        height = Texture.load(name + "/height.png");
        height.num = 5;
    }

    private PBRTexture(String name, String albedoName) {
        super(0);
        albedo = Texture.load(albedoName);
        albedo.num = 0;
        normal = Texture.load(name + "/normal.png");
        normal.num = 1;
        metallic = Texture.load(name + "/metallic.png");
        metallic.num = 2;
        roughness = Texture.load(name + "/roughness.png");
        roughness.num = 3;
        ao = Texture.load(name + "/ao.png");
        ao.num = 4;
        height = Texture.load(name + "/height.png");
        height.num = 5;
    }

    @Override
    public void bind() {
        bindAll(albedo, normal, metallic, roughness, ao, height);
    }

    @Override
    public void destroy() {
        albedo.destroy();
        normal.destroy();
        metallic.destroy();
        roughness.destroy();
        ao.destroy();
        height.destroy();
    }

    public static PBRTexture loadAlbedo(String name) {
        return new PBRTexture("default", name);
    }
}
