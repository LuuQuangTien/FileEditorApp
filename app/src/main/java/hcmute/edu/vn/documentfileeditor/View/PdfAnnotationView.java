package hcmute.edu.vn.documentfileeditor.View;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EditText;
import android.app.AlertDialog;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PdfAnnotationView extends AppCompatImageView {

    public enum Mode { NONE, HIGHLIGHT, TEXT }

    public static class Annotation {
        public enum Type { HIGHLIGHT, TEXT }
        public Type type;
        public RectF rect; // For highlight
        public PointF point; // For text
        public String text;
        
        public Annotation(RectF rect) {
            this.type = Type.HIGHLIGHT;
            this.rect = rect;
        }
        
        public Annotation(PointF point, String text) {
            this.type = Type.TEXT;
            this.point = point;
            this.text = text;
        }
    }

    private Mode currentMode = Mode.NONE;
    
    // Store annotations per page. Key is page index.
    private Map<Integer, List<Annotation>> pageAnnotations = new HashMap<>();
    private int currentPageIndex = -1;

    private Paint highlightPaint;
    private Paint textPaint;

    private PointF touchStart = new PointF();
    private PointF touchCurrent = new PointF();
    private boolean isDrawing = false;

    public PdfAnnotationView(Context context) {
        super(context);
        init();
    }

    public PdfAnnotationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        highlightPaint = new Paint();
        highlightPaint.setColor(Color.argb(128, 255, 255, 0)); // Semi-transparent yellow
        highlightPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(60f); // Will be scaled later
        textPaint.setFakeBoldText(true);
    }

    public void setMode(Mode mode) {
        this.currentMode = mode;
    }

    public void setCurrentPage(int pageIndex, Bitmap pageBitmap) {
        this.currentPageIndex = pageIndex;
        setImageBitmap(pageBitmap);
        if (!pageAnnotations.containsKey(pageIndex)) {
            pageAnnotations.put(pageIndex, new ArrayList<>());
        }
        invalidate();
    }

    public List<Annotation> getAnnotationsForPage(int pageIndex) {
        return pageAnnotations.getOrDefault(pageIndex, new ArrayList<>());
    }

    public Map<Integer, List<Annotation>> getAllAnnotations() {
        return pageAnnotations;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentPageIndex == -1) return;

        // Image Matrix is used to map between view coordinates and bitmap coordinates
        float[] values = new float[9];
        getImageMatrix().getValues(values);
        float scaleX = values[android.graphics.Matrix.MSCALE_X];
        float scaleY = values[android.graphics.Matrix.MSCALE_Y];
        float transX = values[android.graphics.Matrix.MTRANS_X];
        float transY = values[android.graphics.Matrix.MTRANS_Y];

        // Draw saved annotations
        List<Annotation> annotations = pageAnnotations.get(currentPageIndex);
        if (annotations != null) {
            for (Annotation ann : annotations) {
                if (ann.type == Annotation.Type.HIGHLIGHT) {
                    RectF viewRect = new RectF(
                            ann.rect.left * scaleX + transX,
                            ann.rect.top * scaleY + transY,
                            ann.rect.right * scaleX + transX,
                            ann.rect.bottom * scaleY + transY
                    );
                    canvas.drawRect(viewRect, highlightPaint);
                } else if (ann.type == Annotation.Type.TEXT) {
                    float viewX = ann.point.x * scaleX + transX;
                    float viewY = ann.point.y * scaleY + transY;
                    canvas.drawText(ann.text, viewX, viewY, textPaint);
                }
            }
        }

        // Draw current dragging highlight
        if (isDrawing && currentMode == Mode.HIGHLIGHT) {
            float left = Math.min(touchStart.x, touchCurrent.x);
            float top = Math.min(touchStart.y, touchCurrent.y);
            float right = Math.max(touchStart.x, touchCurrent.x);
            float bottom = Math.max(touchStart.y, touchCurrent.y);
            canvas.drawRect(left, top, right, bottom, highlightPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentMode == Mode.NONE || currentPageIndex == -1) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (currentMode == Mode.HIGHLIGHT) {
                    touchStart.set(x, y);
                    touchCurrent.set(x, y);
                    isDrawing = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDrawing && currentMode == Mode.HIGHLIGHT) {
                    touchCurrent.set(x, y);
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (currentMode == Mode.HIGHLIGHT && isDrawing) {
                    touchCurrent.set(x, y);
                    isDrawing = false;
                    addHighlightAnnotation(touchStart, touchCurrent);
                    invalidate();
                } else if (currentMode == Mode.TEXT) {
                    promptForText(new PointF(x, y));
                }
                break;
        }
        return true;
    }

    private void addHighlightAnnotation(PointF start, PointF end) {
        float[] values = new float[9];
        getImageMatrix().getValues(values);
        float scaleX = values[android.graphics.Matrix.MSCALE_X];
        float scaleY = values[android.graphics.Matrix.MSCALE_Y];
        float transX = values[android.graphics.Matrix.MTRANS_X];
        float transY = values[android.graphics.Matrix.MTRANS_Y];

        // Convert view rect to bitmap rect
        float left = (Math.min(start.x, end.x) - transX) / scaleX;
        float top = (Math.min(start.y, end.y) - transY) / scaleY;
        float right = (Math.max(start.x, end.x) - transX) / scaleX;
        float bottom = (Math.max(start.y, end.y) - transY) / scaleY;

        RectF bitmapRect = new RectF(left, top, right, bottom);
        if (Math.abs(left - right) > 5 && Math.abs(top - bottom) > 5) {
            pageAnnotations.get(currentPageIndex).add(new Annotation(bitmapRect));
        }
    }

    private void promptForText(PointF viewPoint) {
        EditText input = new EditText(getContext());
        new AlertDialog.Builder(getContext())
                .setTitle("Add Note")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String text = input.getText().toString();
                    if (!text.isEmpty()) {
                        float[] values = new float[9];
                        getImageMatrix().getValues(values);
                        float scaleX = values[android.graphics.Matrix.MSCALE_X];
                        float scaleY = values[android.graphics.Matrix.MSCALE_Y];
                        float transX = values[android.graphics.Matrix.MTRANS_X];
                        float transY = values[android.graphics.Matrix.MTRANS_Y];

                        float bmpX = (viewPoint.x - transX) / scaleX;
                        float bmpY = (viewPoint.y - transY) / scaleY;

                        pageAnnotations.get(currentPageIndex).add(new Annotation(new PointF(bmpX, bmpY), text));
                        invalidate();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
