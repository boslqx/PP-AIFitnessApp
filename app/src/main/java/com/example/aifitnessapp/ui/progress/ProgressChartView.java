package com.example.aifitnessapp.ui.progress;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.List;

/*
 * Custom View that draws a line chart for any float dataset.
 *
 * HOW CANVAS DRAWING WORKS:
 * - Canvas is a 2D drawing surface. We get one in onDraw().
 * - Paint defines the style (color, stroke width, etc.) for each draw call.
 * - We convert data values to pixel coordinates using a simple scale formula:
 *     x = padding + (index / (count-1)) * chartWidth
 *     y = padding + chartHeight - ((value - minVal) / range * chartHeight)
 *   Y is inverted because screen Y=0 is at the TOP, but chart Y=0 is at the BOTTOM.
 */
public class ProgressChartView extends View {

    // ── Data ──────────────────────────────────────────────────
    private List<Float>  values;
    private List<String> labels;   // x-axis labels (e.g. "Mon", "Mar 1")
    private String       unit;     // e.g. "kg", "kcal", "hrs"
    private int          lineColor = Color.parseColor("#4CAF50");

    // ── Paint objects (created once, reused every frame) ──────
    private Paint linePaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint dotPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint fillPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint gridPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint labelPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint valuePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Layout ────────────────────────────────────────────────
    private float padLeft   = 60f;   // room for Y-axis labels
    private float padRight  = 20f;
    private float padTop    = 30f;
    private float padBottom = 40f;   // room for X-axis labels

    public ProgressChartView(Context context) {
        super(context);
        init();
    }

    public ProgressChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Line style
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        // Dot at each data point
        dotPaint.setStyle(Paint.Style.FILL);

        // Gradient fill under the line
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAlpha(40); // semi-transparent

        // Grid lines
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(Color.parseColor("#E0E0E0"));
        gridPaint.setPathEffect(new DashPathEffect(new float[]{8, 6}, 0));

        // X-axis labels
        labelPaint.setTextSize(28f);
        labelPaint.setColor(Color.parseColor("#9E9E9E"));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        // Y-axis value labels
        valuePaint.setTextSize(26f);
        valuePaint.setColor(Color.parseColor("#9E9E9E"));
        valuePaint.setTextAlign(Paint.Align.RIGHT);
    }

    /*
     * Called by the Activity to provide data.
     * Triggers a redraw via invalidate().
     */
    public void setData(List<Float> values, List<String> labels, String unit, int color) {
        this.values    = values;
        this.labels    = labels;
        this.unit      = unit;
        this.lineColor = color;
        linePaint.setColor(color);
        dotPaint.setColor(color);
        fillPaint.setColor(color);
        invalidate(); // triggers onDraw()
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (values == null || values.size() < 2) {
            drawEmptyState(canvas);
            return;
        }

        float w = getWidth();
        float h = getHeight();
        float chartW = w - padLeft - padRight;
        float chartH = h - padTop - padBottom;

        // ── Calculate value range ──────────────────────────────
        float minVal = Float.MAX_VALUE, maxVal = Float.MIN_VALUE;
        for (float v : values) {
            if (v < minVal) minVal = v;
            if (v > maxVal) maxVal = v;
        }
        // Add 10% padding above/below so points don't sit on the edge
        float range = maxVal - minVal;
        if (range == 0) range = 1f; // prevent division by zero for flat data
        float displayMin = minVal - range * 0.1f;
        float displayMax = maxVal + range * 0.1f;
        float displayRange = displayMax - displayMin;

        // ── Draw horizontal grid lines (4 lines) ──────────────
        for (int i = 0; i <= 4; i++) {
            float frac = i / 4f;
            float y = padTop + chartH * (1f - frac);
            canvas.drawLine(padLeft, y, padLeft + chartW, y, gridPaint);

            // Y-axis value label
            float labelVal = displayMin + displayRange * frac;
            canvas.drawText(formatValue(labelVal), padLeft - 8f, y + 9f, valuePaint);
        }

        // ── Convert data to pixel coordinates ─────────────────
        int n = values.size();
        float[] px = new float[n];
        float[] py = new float[n];
        for (int i = 0; i < n; i++) {
            px[i] = padLeft + (i / (float)(n - 1)) * chartW;
            py[i] = padTop  + chartH * (1f - (values.get(i) - displayMin) / displayRange);
        }

        // ── Draw fill area under the line ─────────────────────
        Path fillPath = new Path();
        fillPath.moveTo(px[0], padTop + chartH);   // start at bottom-left
        for (int i = 0; i < n; i++) fillPath.lineTo(px[i], py[i]);
        fillPath.lineTo(px[n-1], padTop + chartH); // close to bottom-right
        fillPath.close();
        canvas.drawPath(fillPath, fillPaint);

        // ── Draw the line ──────────────────────────────────────
        Path linePath = new Path();
        linePath.moveTo(px[0], py[0]);
        for (int i = 1; i < n; i++) linePath.lineTo(px[i], py[i]);
        canvas.drawPath(linePath, linePaint);

        // ── Draw dots + x-axis labels ─────────────────────────
        for (int i = 0; i < n; i++) {
            canvas.drawCircle(px[i], py[i], 8f, dotPaint);

            // Only draw label for first, last, and every ~3rd point to avoid crowding
            if (labels != null && i < labels.size()) {
                if (i == 0 || i == n-1 || i % 3 == 0) {
                    canvas.drawText(labels.get(i), px[i], h - 8f, labelPaint);
                }
            }
        }
    }

    private void drawEmptyState(Canvas canvas) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor("#BDBDBD"));
        p.setTextSize(36f);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Not enough data yet", getWidth() / 2f, getHeight() / 2f, p);
        p.setTextSize(28f);
        canvas.drawText("Log at least 2 days to see your chart",
                getWidth() / 2f, getHeight() / 2f + 50f, p);
    }

    // Format numbers nicely for Y-axis labels
    private String formatValue(float val) {
        if (Math.abs(val) >= 1000) return String.format("%.0fk", val / 1000f);
        if (val == Math.floor(val))  return String.format("%.0f", val);
        return String.format("%.1f", val);
    }
}