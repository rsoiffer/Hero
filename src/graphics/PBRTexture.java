package graphics;

import graphics.opengl.GLObject;
import graphics.opengl.Texture;
import java.io.File;

public class PBRTexture extends GLObject {

    public static final int NUM_COMPONENTS = 8;
    private static final String[] NAMES = {
        "albedo", "normal", "metallic", "roughness",
        "ao", "height", "alpha", "emissive"};
    private static final PBRTexture DEFAULT = loadFromFolder("default");

    private final Texture[] textures;

    private PBRTexture(String folder, String extension) {
        super(0);
        textures = new Texture[NUM_COMPONENTS];
        for (int i = 0; i < NUM_COMPONENTS; i++) {
            String filename = folder + "/" + NAMES[i] + "." + extension;
            if (new File("sprites/" + filename).isFile()) {
                textures[i] = Texture.load(filename);
                textures[i].num = i;
            } else {
                textures[i] = DEFAULT.textures[i];
            }
        }
    }

    @Override
    public void bind() {
        bindAll(textures);
    }

    @Override
    public void destroy() {
    }

    public boolean hasAlpha() {
        return textures[6] != DEFAULT.textures[6];
    }

    public static PBRTexture loadFromFolder(String folder) {
        return new PBRTexture(folder, "png");
    }

    public static PBRTexture loadFromFolder(String folder, String ext) {
        return new PBRTexture(folder, ext);
    }
}
