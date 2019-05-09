package graphics.models;

import graphics.models.Vertex.VertexPBR;
import util.math.Vec3d;

public class ModelSimplifier3 {

    public static CustomModel simplify(CustomModel originalModel, double mod) {
        CustomModel newModel = new CustomModel();
        for (int i = 0; i < originalModel.vertices.size() / 3; i++) {
            VertexPBR v1 = originalModel.vertices.get(3 * i);
            VertexPBR v2 = originalModel.vertices.get(3 * i + 1);
            VertexPBR v3 = originalModel.vertices.get(3 * i + 2);
            Vec3d newP1 = v1.position.div(mod).floor().mul(mod);
            Vec3d newP2 = v2.position.div(mod).floor().mul(mod);
            Vec3d newP3 = v3.position.div(mod).floor().mul(mod);
            if (newP1.equals(newP2)) {
                continue;
            }
            if (newP1.equals(newP3)) {
                continue;
            }
            if (newP2.equals(newP3)) {
                continue;
            }
            newModel.addTriangle(newP1, v1.texCoord, newP2, v2.texCoord, newP3, v3.texCoord);
        }
        return newModel;
    }
}
