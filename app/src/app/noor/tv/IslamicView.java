package app.noor.tv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/**
 * Animated Islamic background (about 30 fps, cheap enough for a 2 GB TV):
 * cached emerald gradient + khatam tessellation, three 3D-tilting star rosettes, a gold light sweep,
 * rising gold particles, a shimmering mihrab arch and a softly turning 3D crescent.
 */
class IslamicView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean drawArch;
    private final Camera cam = new Camera();
    private final Matrix m = new Matrix();
    private final long t0 = SystemClock.uptimeMillis();
    private Bitmap base;
    private float[][] parts;
    private Path archOuter, archInner, rosette, moon;
    private LinearGradient sweep, band, moonShade;
    private RadialGradient glow;
    private final Matrix sm = new Matrix();

    IslamicView(Context c, boolean drawArch) {
        super(c);
        this.drawArch = drawArch;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        if (w == 0 || h == 0) return;
        base = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        Canvas cv = new Canvas(base);
        fill.setShader(new LinearGradient(0, 0, w * 0.3f, h, Noor.EMERALD, Noor.NIGHT, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, fill);
        float s = h / 7.5f;
        line.setColor(0x24D9B45A);
        line.setStrokeWidth(Math.max(1.2f, h / 900f));
        for (float y = -s; y < h + s; y += s)
            for (float x = -s; x < w + s; x += s) {
                cv.drawPath(star(x + s / 2, y + s / 2, s * 0.42f, 0), line);
                cv.drawPath(star(x, y, s * 0.16f, 0), line);
            }
        fill.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.62f,
                new int[]{0xE6041C18, 0x99041C18, 0x00041C18}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, fill);
        fill.setShader(null);

        Random r = new Random(7);
        parts = new float[46][];
        for (int i = 0; i < parts.length; i++)  // x, y-phase, speed, size, twinkle-phase
            parts[i] = new float[]{r.nextFloat() * w, r.nextFloat(), 0.012f + r.nextFloat() * 0.03f,
                    1.2f + r.nextFloat() * 2.8f, r.nextFloat() * 6.28f};

        float aw = w * 0.36f, top = h * 0.10f, bottom = h * 0.93f;
        archOuter = arch(w / 2f, aw, top, bottom);
        archInner = arch(w / 2f, aw - h * 0.022f, top + h * 0.03f, bottom - h * 0.012f);
        sweep = new LinearGradient(-w * 0.12f, 0, w * 0.12f, h * 0.35f,
                new int[]{0x00FFE9B0, 0x1CFFE9B0, 0x00FFE9B0}, null, Shader.TileMode.CLAMP);
        band = new LinearGradient(0, -h * 0.2f, 0, h * 0.2f,
                new int[]{Noor.GOLD, 0xFFFFF2C8, Noor.GOLD}, null, Shader.TileMode.CLAMP);
        float cr = h * 0.024f, mx = w / 2f, my = h * 0.10f - cr * 1.7f;
        glow = new RadialGradient(mx, my, cr * 3.2f, 0x55D9B45A, 0x00D9B45A, Shader.TileMode.CLAMP);
        moonShade = new LinearGradient(mx - cr, my - cr, mx + cr, my + cr, 0xFFFFF0C0, 0xFFB8892E, Shader.TileMode.CLAMP);
        moon = new Path();
        moon.addCircle(mx, my, cr, Path.Direction.CW);
        Path cut = new Path();
        cut.addCircle(mx + cr * 0.45f, my - cr * 0.2f, cr * 0.85f, Path.Direction.CW);
        moon.op(cut, Path.Op.DIFFERENCE);
        rosette = new Path();
        rosette.addPath(star(0, 0, 1f, 0));
        rosette.addPath(star(0, 0, 1f, (float) (Math.PI / 8)));
        rosette.addCircle(0, 0, 0.42f, Path.Direction.CW);
        rosette.addPath(star(0, 0, 0.42f, (float) (Math.PI / 8)));
    }

    @Override
    protected void onDraw(Canvas cv) {
        if (base == null) return;
        float w = getWidth(), h = getHeight();
        float t = (SystemClock.uptimeMillis() - t0) / 1000f;
        cv.drawBitmap(base, 0, 0, null);

        // 3D star rosettes, tilting and turning slowly at different depths
        float[][] ros = drawArch
                ? new float[][]{{0.10f, 0.78f, 0.20f, 6f}, {0.90f, 0.22f, 0.17f, -5f}, {0.88f, 0.84f, 0.11f, 9f}}
                : new float[][]{{0.06f, 0.92f, 0.24f, 5f}, {0.95f, 0.12f, 0.16f, -6f}, {0.62f, 0.10f, 0.08f, 10f}};
        for (int i = 0; i < ros.length; i++) {
            float cx = w * ros[i][0], cy = h * ros[i][1], rr = h * ros[i][2];
            cv.save();
            cam.save();
            cam.rotateY((float) Math.sin(t * 0.35 + i * 2) * 38);
            cam.rotateX((float) Math.cos(t * 0.27 + i) * 22);
            cam.getMatrix(m);
            cam.restore();
            m.preTranslate(-cx, -cy);
            m.postTranslate(cx, cy);
            cv.concat(m);
            cv.translate(cx, cy);
            cv.rotate(t * ros[i][3]);
            cv.scale(rr, rr);
            line.setStrokeWidth(10f / rr);
            line.setColor(0x14D9B45A);
            cv.drawPath(rosette, line);
            line.setStrokeWidth(2.2f / rr);
            line.setColor(0x6ED9B45A);
            cv.drawPath(rosette, line);
            cv.restore();
        }

        // light sweep
        float period = 11f, sx = ((t % period) / period) * w * 1.8f - w * 0.4f;
        sm.setTranslate(sx, 0);
        sweep.setLocalMatrix(sm);
        fill.setShader(sweep);
        cv.drawRect(Math.max(0, sx - w * 0.5f), 0, Math.min(w, sx + w * 0.5f), h, fill);
        fill.setShader(null);

        // rising gold particles
        for (float[] p : parts) {
            float y = h * (1f - ((p[1] + t * p[2]) % 1f));
            float tw = 0.45f + 0.55f * (float) Math.abs(Math.sin(t * 1.3 + p[4]));
            fill.setColor(((int) (tw * 150) << 24) | 0xE8C77A);
            cv.drawCircle(p[0] + (float) Math.sin(t * 0.6 + p[4]) * 12, y, p[3], fill);
        }

        if (drawArch) {
            // shimmering arch: a bright band travels up the gold
            float band = ((t % 6f) / 6f) * h * 1.4f;
            sm.setTranslate(0, h - band);
            this.band.setLocalMatrix(sm);
            line.setShader(this.band);
            line.setStrokeWidth(h / 300f);
            cv.drawPath(archOuter, line);
            line.setShader(null);
            line.setColor(Noor.GOLD_SOFT);
            line.setStrokeWidth(h / 900f);
            cv.drawPath(archInner, line);

            // 3D crescent with a glow
            float cr = h * 0.024f, cx = w / 2f, cy = h * 0.10f - cr * 1.7f;
            fill.setShader(glow);
            cv.drawCircle(cx, cy, cr * 3.2f, fill);
            cv.save();
            cam.save();
            cam.rotateY((float) Math.sin(t * 0.8) * 32);
            cam.getMatrix(m);
            cam.restore();
            m.preTranslate(-cx, -cy);
            m.postTranslate(cx, cy);
            cv.concat(m);
            fill.setShader(moonShade);
            cv.drawPath(moon, fill);
            fill.setShader(null);
            cv.restore();
        }
        if (isShown()) postInvalidateDelayed(50);
    }

    @Override
    protected void onVisibilityChanged(View v, int vis) {
        super.onVisibilityChanged(v, vis);
        if (vis == VISIBLE) invalidate();
    }

    private Path arch(float cx, float half, float top, float bottom) {
        Path p = new Path();
        float spring = top + (bottom - top) * 0.30f;
        p.moveTo(cx - half, bottom);
        p.lineTo(cx - half, spring);
        p.cubicTo(cx - half, spring - (spring - top) * 0.55f, cx - half * 0.25f, top + (spring - top) * 0.25f, cx, top);
        p.cubicTo(cx + half * 0.25f, top + (spring - top) * 0.25f, cx + half, spring - (spring - top) * 0.55f, cx + half, spring);
        p.lineTo(cx + half, bottom);
        return p;
    }

    /** Eight-pointed star: two overlapping squares, turned by rot. */
    private static Path star(float cx, float cy, float r, float rot) {
        Path p = new Path();
        for (int sq = 0; sq < 2; sq++) {
            double a0 = rot + sq * Math.PI / 4;
            for (int i = 0; i <= 4; i++) {
                double a = a0 + i * Math.PI / 2;
                float x = cx + (float) (r * Math.cos(a)), y = cy + (float) (r * Math.sin(a));
                if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            p.close();
        }
        return p;
    }
}
