package graphics.models;

import graphics.models.Vertex.VertexColor;
import graphics.opengl.BufferObject;
import graphics.opengl.VertexArrayObject;
import static graphics.voxels.VoxelRenderer.DIRS;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glDrawElements;
import static org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER;
import util.Resources;
import static util.math.MathUtils.mod;
import util.math.Vec3d;

public class VoxelModel2 implements Model {

    private static final Map<String, VoxelModel2> MODEL_CACHE = new HashMap();

    public static VoxelModel2 load(String fileName) {
        if (!MODEL_CACHE.containsKey(fileName)) {
            VoxelModel2 s = new VoxelModel2(fileName);
            MODEL_CACHE.put(fileName, s);
        }
        return MODEL_CACHE.get(fileName);
    }

    private static final int[] DEFAULT_COLOR_PALETTE = {
        0x00000000, 0xffffffff, 0xffccffff, 0xff99ffff, 0xff66ffff, 0xff33ffff, 0xff00ffff, 0xffffccff, 0xffccccff, 0xff99ccff, 0xff66ccff, 0xff33ccff, 0xff00ccff, 0xffff99ff, 0xffcc99ff, 0xff9999ff,
        0xff6699ff, 0xff3399ff, 0xff0099ff, 0xffff66ff, 0xffcc66ff, 0xff9966ff, 0xff6666ff, 0xff3366ff, 0xff0066ff, 0xffff33ff, 0xffcc33ff, 0xff9933ff, 0xff6633ff, 0xff3333ff, 0xff0033ff, 0xffff00ff,
        0xffcc00ff, 0xff9900ff, 0xff6600ff, 0xff3300ff, 0xff0000ff, 0xffffffcc, 0xffccffcc, 0xff99ffcc, 0xff66ffcc, 0xff33ffcc, 0xff00ffcc, 0xffffcccc, 0xffcccccc, 0xff99cccc, 0xff66cccc, 0xff33cccc,
        0xff00cccc, 0xffff99cc, 0xffcc99cc, 0xff9999cc, 0xff6699cc, 0xff3399cc, 0xff0099cc, 0xffff66cc, 0xffcc66cc, 0xff9966cc, 0xff6666cc, 0xff3366cc, 0xff0066cc, 0xffff33cc, 0xffcc33cc, 0xff9933cc,
        0xff6633cc, 0xff3333cc, 0xff0033cc, 0xffff00cc, 0xffcc00cc, 0xff9900cc, 0xff6600cc, 0xff3300cc, 0xff0000cc, 0xffffff99, 0xffccff99, 0xff99ff99, 0xff66ff99, 0xff33ff99, 0xff00ff99, 0xffffcc99,
        0xffcccc99, 0xff99cc99, 0xff66cc99, 0xff33cc99, 0xff00cc99, 0xffff9999, 0xffcc9999, 0xff999999, 0xff669999, 0xff339999, 0xff009999, 0xffff6699, 0xffcc6699, 0xff996699, 0xff666699, 0xff336699,
        0xff006699, 0xffff3399, 0xffcc3399, 0xff993399, 0xff663399, 0xff333399, 0xff003399, 0xffff0099, 0xffcc0099, 0xff990099, 0xff660099, 0xff330099, 0xff000099, 0xffffff66, 0xffccff66, 0xff99ff66,
        0xff66ff66, 0xff33ff66, 0xff00ff66, 0xffffcc66, 0xffcccc66, 0xff99cc66, 0xff66cc66, 0xff33cc66, 0xff00cc66, 0xffff9966, 0xffcc9966, 0xff999966, 0xff669966, 0xff339966, 0xff009966, 0xffff6666,
        0xffcc6666, 0xff996666, 0xff666666, 0xff336666, 0xff006666, 0xffff3366, 0xffcc3366, 0xff993366, 0xff663366, 0xff333366, 0xff003366, 0xffff0066, 0xffcc0066, 0xff990066, 0xff660066, 0xff330066,
        0xff000066, 0xffffff33, 0xffccff33, 0xff99ff33, 0xff66ff33, 0xff33ff33, 0xff00ff33, 0xffffcc33, 0xffcccc33, 0xff99cc33, 0xff66cc33, 0xff33cc33, 0xff00cc33, 0xffff9933, 0xffcc9933, 0xff999933,
        0xff669933, 0xff339933, 0xff009933, 0xffff6633, 0xffcc6633, 0xff996633, 0xff666633, 0xff336633, 0xff006633, 0xffff3333, 0xffcc3333, 0xff993333, 0xff663333, 0xff333333, 0xff003333, 0xffff0033,
        0xffcc0033, 0xff990033, 0xff660033, 0xff330033, 0xff000033, 0xffffff00, 0xffccff00, 0xff99ff00, 0xff66ff00, 0xff33ff00, 0xff00ff00, 0xffffcc00, 0xffcccc00, 0xff99cc00, 0xff66cc00, 0xff33cc00,
        0xff00cc00, 0xffff9900, 0xffcc9900, 0xff999900, 0xff669900, 0xff339900, 0xff009900, 0xffff6600, 0xffcc6600, 0xff996600, 0xff666600, 0xff336600, 0xff006600, 0xffff3300, 0xffcc3300, 0xff993300,
        0xff663300, 0xff333300, 0xff003300, 0xffff0000, 0xffcc0000, 0xff990000, 0xff660000, 0xff330000, 0xff0000ee, 0xff0000dd, 0xff0000bb, 0xff0000aa, 0xff000088, 0xff000077, 0xff000055, 0xff000044,
        0xff000022, 0xff000011, 0xff00ee00, 0xff00dd00, 0xff00bb00, 0xff00aa00, 0xff008800, 0xff007700, 0xff005500, 0xff004400, 0xff002200, 0xff001100, 0xffee0000, 0xffdd0000, 0xffbb0000, 0xffaa0000,
        0xff880000, 0xff770000, 0xff550000, 0xff440000, 0xff220000, 0xff110000, 0xffeeeeee, 0xffdddddd, 0xffbbbbbb, 0xffaaaaaa, 0xff888888, 0xff777777, 0xff555555, 0xff444444, 0xff222222, 0xff111111
    };

    private Vec3d originalSize;
    private final int num;
    private final VertexArrayObject vao;
    private final BufferObject ebo;

    private VoxelModel2(String fileName) {
        Map<Vec3d, Integer> colors = null;
        int[] colorPalette = DEFAULT_COLOR_PALETTE;

        byte[] bytes = Resources.loadFileAsBytes("models/" + fileName);
        int pos = 8;
        while (pos < bytes.length) {
            String chunkName = new String(bytes, pos, 4);
            int chunkSize = readInt(bytes, pos + 4);
            if (chunkName.equals("SIZE")) {
                originalSize = new Vec3d(readInt(bytes, pos + 12), readInt(bytes, pos + 16), readInt(bytes, pos + 20));
            }
            if (chunkName.equals("XYZI")) {
                colors = loadXYZI(bytes, pos);
            }
            if (chunkName.equals("RGBA")) {
                colorPalette = loadRGBA(bytes, pos);
            }
            pos += 12 + chunkSize;
        }

        List<VertexColor> vertices = new ArrayList();
        for (Entry<Vec3d, Integer> e : colors.entrySet()) {
            int colorHex = colorPalette[e.getValue()];
            Vec3d color = new Vec3d(mod(colorHex, 256), mod(colorHex >> 8, 256), mod(colorHex >> 16, 256)).div(255);
            for (Vec3d dir : DIRS) {
                if (!colors.containsKey(e.getKey().add(dir))) {
                    int x0 = (int) (e.getKey().x + Math.max(dir.x, 0)), y0 = (int) (e.getKey().y + Math.max(dir.y, 0)), z0 = (int) (e.getKey().z + Math.max(dir.z, 0));
                    for (int x = 0; x <= (dir.x == 0 ? 1 : 0); x++) {
                        for (int y = 0; y <= (dir.y == 0 ? 1 : 0); y++) {
                            for (int z = 0; z <= (dir.z == 0 ? 1 : 0); z++) {
                                vertices.add(new VertexColor(new Vec3d(x0 + x, y0 + y, z0 + z), color, dir));
                            }
                        }
                    }
                }
            }
        }
        List<Integer> indices = new ArrayList();
        for (int i = 0; i < vertices.size(); i += 4) {
            indices.addAll(Arrays.asList(i, i + 1, i + 2, i + 1, i + 2, i + 3));
        }
        num = indices.size();
        vao = Vertex.createVAO(vertices, new int[]{3, 3, 3});
        ebo = new BufferObject(GL_ELEMENT_ARRAY_BUFFER, indices.stream().mapToInt(i -> i).toArray());
    }

    private static int[] loadRGBA(byte[] bytes, int pos) {
        int[] colorPalette = new int[256];
        for (int i = 0; i < 255; i++) {
            colorPalette[i + 1] = readInt(bytes, pos + 12 + 4 * i);
        }
        return colorPalette;
    }

    private static Map<Vec3d, Integer> loadXYZI(byte[] bytes, int pos) {
        Map<Vec3d, Integer> colors = new HashMap();
        int numBlocks = readInt(bytes, pos + 12);
        for (int i = 0; i < numBlocks; i++) {
            int x = mod(bytes[pos + 16 + 4 * i], 256);
            int y = mod(bytes[pos + 16 + 4 * i + 1], 256);
            int z = mod(bytes[pos + 16 + 4 * i + 2], 256);
            int colorID = mod(bytes[pos + 16 + 4 * i + 3], 256);
            colors.put(new Vec3d(x, y, z), colorID);
        }
        return colors;
    }

    private static int readInt(byte[] bytes, int pos) {
        return mod(bytes[pos], 256)
                + (mod(bytes[pos + 1], 256) << 8)
                + (mod(bytes[pos + 2], 256) << 16)
                + (mod(bytes[pos + 3], 256) << 24);
    }

    @Override
    public void render() {
        vao.bind();
        glDrawElements(GL_TRIANGLES, num, GL_UNSIGNED_INT, 0);
    }
}
