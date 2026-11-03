package app.noor.tv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Gold line icons for the quick tiles: settings, wifi, bluetooth, display, kahf (play), verse (book), sound. */
class IconView extends View {
    private final String kind;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    IconView(Context c, String kind) {
        super(c);
        this.kind = kind;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void onDraw(Canvas cv) {
        float s = Math.min(getWidth(), getHeight()), cx = getWidth() / 2f, cy = getHeight() / 2f, r = s * 0.40f;
        p.setShader(new LinearGradient(0, cy - r, 0, cy + r, 0xFFFFF0C0, 0xFFC9A04A, Shader.TileMode.CLAMP));
        p.setStrokeWidth(s * 0.075f);
        p.setStyle(Paint.Style.STROKE);
        switch (kind) {
            case "settings": {
                cv.drawCircle(cx, cy, r * 0.32f, p);
                cv.drawCircle(cx, cy, r * 0.68f, p);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    cv.drawLine(cx + (float) Math.cos(a) * r * 0.70f, cy + (float) Math.sin(a) * r * 0.70f,
                            cx + (float) Math.cos(a) * r * 0.98f, cy + (float) Math.sin(a) * r * 0.98f, p);
                }
                break;
            }
            case "wifi": {
                for (int i = 1; i <= 3; i++) {
                    float rr = r * 0.33f * i;
                    cv.drawArc(new RectF(cx - rr, cy + r * 0.45f - rr, cx + rr, cy + r * 0.45f + rr), 225, 90, false, p);
                }
                p.setStyle(Paint.Style.FILL);
                cv.drawCircle(cx, cy + r * 0.45f, s * 0.06f, p);
                break;
            }
            case "bluetooth": {
                Path b = new Path();
                b.moveTo(cx - r * 0.45f, cy - r * 0.45f);
                b.lineTo(cx + r * 0.45f, cy + r * 0.40f);
                b.lineTo(cx, cy + r * 0.85f);
                b.lineTo(cx, cy - r * 0.85f);
                b.lineTo(cx + r * 0.45f, cy - r * 0.40f);
                b.lineTo(cx - r * 0.45f, cy + r * 0.45f);
                cv.drawPath(b, p);
                break;
            }
            case "display": {
                cv.drawRoundRect(new RectF(cx - r, cy - r * 0.65f, cx + r, cy + r * 0.45f), s * 0.06f, s * 0.06f, p);
                cv.drawLine(cx - r * 0.45f, cy + r * 0.85f, cx + r * 0.45f, cy + r * 0.85f, p);
                break;
            }
            case "kahf": {
                cv.drawCircle(cx, cy, r, p);
                Path t = new Path();
                t.moveTo(cx - r * 0.30f, cy - r * 0.45f);
                t.lineTo(cx + r * 0.50f, cy);
                t.lineTo(cx - r * 0.30f, cy + r * 0.45f);
                t.close();
                p.setStyle(Paint.Style.FILL);
                cv.drawPath(t, p);
                break;
            }
            case "verse": {
                Path b = new Path();
                b.moveTo(cx, cy - r * 0.45f);
                b.quadTo(cx - r * 0.5f, cy - r * 0.75f, cx - r, cy - r * 0.55f);
                b.lineTo(cx - r, cy + r * 0.65f);
                b.quadTo(cx - r * 0.5f, cy + r * 0.45f, cx, cy + r * 0.75f);
                b.quadTo(cx + r * 0.5f, cy + r * 0.45f, cx + r, cy + r * 0.65f);
                b.lineTo(cx + r, cy - r * 0.55f);
                b.quadTo(cx + r * 0.5f, cy - r * 0.75f, cx, cy - r * 0.45f);
                b.lineTo(cx, cy + r * 0.75f);
                cv.drawPath(b, p);
                break;
            }
            default: { // sound
                Path sp = new Path();
                sp.moveTo(cx - r, cy - r * 0.3f);
                sp.lineTo(cx - r * 0.5f, cy - r * 0.3f);
                sp.lineTo(cx, cy - r * 0.8f);
                sp.lineTo(cx, cy + r * 0.8f);
                sp.lineTo(cx - r * 0.5f, cy + r * 0.3f);
                sp.lineTo(cx - r, cy + r * 0.3f);
                sp.close();
                cv.drawPath(sp, p);
                cv.drawArc(new RectF(cx - r * 0.2f, cy - r * 0.5f, cx + r * 0.6f, cy + r * 0.5f), -50, 100, false, p);
            }
        }
    }
}
