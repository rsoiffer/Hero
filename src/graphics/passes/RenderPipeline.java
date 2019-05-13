package graphics.passes;

import engine.Behavior;
import engine.Layer;
import static engine.Layer.RENDER3D;
import graphics.Camera;
import graphics.Color;
import graphics.opengl.GLState;
import graphics.sprites.Sprite;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glFlush;
import util.math.Transformation;
import util.math.Vec2d;
import util.math.Vec3d;
import vr.EyeCamera;
import vr.Vive;

public class RenderPipeline extends Behavior {

    public Color skyColor = new Color(.4, .7, 1, 1);
    public Vec3d sunColor = new Vec3d(10, 9, 8).mul(.35);
    public Vec3d sunDirection = new Vec3d(.3, -.15, 1);
    public boolean isVR;

    private List<Camera> cameras;
    private List<Vec2d> framebufferSizes;
    private final List<GeometryPass> gpList = new ArrayList();
    private final List<ShadowPass> spList = new ArrayList();
    private final List<LightingPass> lpList = new ArrayList();

    @Override
    public void createInner() {
        cameras = isVR ? Arrays.asList(new EyeCamera(true), new EyeCamera(false)) : Arrays.asList(Camera.camera3d);
        framebufferSizes = isVR ? Arrays.asList(Vive.getRecommendedRenderTargetSize(), Vive.getRecommendedRenderTargetSize()) : Arrays.asList((Vec2d) null);

        for (int i = 0; i < 5; i++) {
            ShadowPass sp = new ShadowPass();
            sp.cameras = cameras;
            sp.zMin = i == 0 ? -1 : (1 - Math.pow(.2, i + 1));
            sp.zMax = 1 - Math.pow(.2, i + 2);
            sp.sunDirection = sunDirection;
            spList.add(sp);
        }

        for (int i = 0; i < cameras.size(); i++) {
            GeometryPass gp = new GeometryPass(framebufferSizes.get(i));
            gp.camera = cameras.get(i);
            gpList.add(gp);

            LightingPass lp = new LightingPass(framebufferSizes.get(i), gp, spList);
            lp.camera = cameras.get(i);
            lp.skyColor = skyColor;
            lp.sunColor = sunColor;
            lp.sunDirection = sunDirection;
            lpList.add(lp);
        }
    }

    @Override
    public Layer layer() {
        return RENDER3D;
    }

    @Override
    public void step() {
        if (isVR) {
            EyeCamera.waitUpdatePos(Camera.camera3d.position);
        }
        gpList.forEach(Runnable::run);
        spList.forEach(Runnable::run);
        lpList.forEach(Runnable::run);
        if (isVR) {
            Vive.submit(true, lpList.get(0).colorBuffer());
            Vive.submit(false, lpList.get(1).colorBuffer());
            glFlush();
        }

        Camera.current = Camera.camera2d;
        GLState.disable(GL_DEPTH_TEST);
        GLState.bindFramebuffer(null);
        if (isVR) {
            Sprite.drawTexture(lpList.get(0).colorBuffer(), Transformation.create(new Vec2d(.25, .5), new Vec2d(.48, 0), new Vec2d(0, .96)), Color.WHITE);
            Sprite.drawTexture(lpList.get(1).colorBuffer(), Transformation.create(new Vec2d(.75, .5), new Vec2d(.48, 0), new Vec2d(0, .96)), Color.WHITE);
        } else {
            Sprite.drawTexture(lpList.get(0).colorBuffer(), Transformation.create(new Vec2d(.5, .5), 0, 1), Color.WHITE);
        }
    }
}
