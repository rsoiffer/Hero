package graphics;

import graphics.opengl.GLObject;
import graphics.opengl.Texture;

public class PBRTexture extends GLObject {

    public static final PBRTexture DEFAULT = loadFromFolder("default");

    private final Texture albedo, normal, metallic, roughness, ao, height;

    public PBRTexture(String albedoName, String normalName, String metallicName,
            String roughnessName, String aoName, String heightName) {
        super(0);
        albedo = Texture.load(albedoName);
        normal = Texture.load(normalName);
        metallic = Texture.load(metallicName);
        roughness = Texture.load(roughnessName);
        ao = Texture.load(aoName);
        height = Texture.load(heightName);
        albedo.num = 0;
        normal.num = 1;
        metallic.num = 2;
        roughness.num = 3;
        ao.num = 4;
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
        return new PBRTexture(name, "default/normal.png", "default/metallic.png",
                "default/roughness.png", "default/ao.png", "default/height.png");
    }

    public static PBRTexture loadFromFolder(String name) {
        return new PBRTexture(name + "/albedo.png", name + "/normal.png", name + "/metallic.png",
                name + "/roughness.png", name + "/ao.png", name + "/height.png");
    }

    public static PBRTexture loadFromFolder(String name, String ext) {
        return new PBRTexture(name + "/albedo." + ext, name + "/normal." + ext, name + "/metallic." + ext,
                name + "/roughness." + ext, name + "/ao." + ext, name + "/height." + ext);
    }
}
