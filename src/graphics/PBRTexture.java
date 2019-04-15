package graphics;

import graphics.opengl.GLObject;
import graphics.opengl.Texture;
import java.io.File;

public class PBRTexture extends GLObject {

    private static final PBRTexture DEFAULT = loadFromFolder("default");

    private final Texture albedo, normal, metallic, roughness, ao, height, alpha;

    public PBRTexture(String albedoName, String normalName, String metallicName,
            String roughnessName, String aoName, String heightName, String alphaName) {
        super(0);
        if (new File("sprites/" + albedoName).isFile()) {
            albedo = Texture.load(albedoName);
            albedo.num = 0;
        } else {
            albedo = DEFAULT.albedo;
        }
        if (new File("sprites/" + normalName).isFile()) {
            normal = Texture.load(normalName);
            normal.num = 1;
        } else {
            normal = DEFAULT.normal;
        }
        if (new File("sprites/" + metallicName).isFile()) {
            metallic = Texture.load(metallicName);
            metallic.num = 2;
        } else {
            metallic = DEFAULT.metallic;
        }
        if (new File("sprites/" + roughnessName).isFile()) {
            roughness = Texture.load(roughnessName);
            roughness.num = 3;
        } else {
            roughness = DEFAULT.roughness;
        }
        if (new File("sprites/" + aoName).isFile()) {
            ao = Texture.load(aoName);
            ao.num = 4;
        } else {
            ao = DEFAULT.ao;
        }
        if (new File("sprites/" + heightName).isFile()) {
            height = Texture.load(heightName);
            height.num = 5;
        } else {
            height = DEFAULT.height;
        }
        if (new File("sprites/" + alphaName).isFile()) {
            alpha = Texture.load(alphaName);
            alpha.num = 6;
        } else {
            alpha = DEFAULT.alpha;
        }
    }

    @Override
    public void bind() {
        bindAll(albedo, normal, metallic, roughness, ao, height, alpha);
    }

    @Override
    public void destroy() {
        albedo.destroy();
        normal.destroy();
        metallic.destroy();
        roughness.destroy();
        ao.destroy();
        height.destroy();
        alpha.destroy();
    }

    public boolean hasAlpha() {
        return alpha != DEFAULT.alpha;
    }

    public static PBRTexture loadFromFolder(String name) {
        return new PBRTexture(name + "/albedo.png", name + "/normal.png", name + "/metallic.png",
                name + "/roughness.png", name + "/ao.png", name + "/height.png", name + "/alpha.png");
    }

    public static PBRTexture loadFromFolder(String name, String ext) {
        return new PBRTexture(name + "/albedo." + ext, name + "/normal." + ext, name + "/metallic." + ext,
                name + "/roughness." + ext, name + "/ao." + ext, name + "/height." + ext, name + "/alpha." + ext);
    }
}
