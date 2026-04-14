package hcmute.edu.vn.documentfileeditor.Service;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;

/**
 * Service layer encapsulating image processing business logic.
 * Decouples image transformation algorithms from Activity lifecycle,
 * making them testable and reusable.
 */
public class ImageService {

    /**
     * Applies color filters (brightness, contrast, saturation) and a base filter matrix to a bitmap.
     *
     * @param original        the original bitmap
     * @param baseFilterMatrix the base color matrix (e.g. sepia, B&W, vintage)
     * @param brightness      brightness percentage (100 = normal)
     * @param contrast        contrast percentage (100 = normal)
     * @param saturation      saturation percentage (100 = normal)
     * @return a new bitmap with filters applied
     */
    public Bitmap applyFilters(Bitmap original, ColorMatrix baseFilterMatrix,
                               float brightness, float contrast, float saturation) {
        if (original == null) return null;

        ColorMatrix cm = new ColorMatrix();
        cm.postConcat(baseFilterMatrix);

        // Saturation
        ColorMatrix satMatrix = new ColorMatrix();
        satMatrix.setSaturation(saturation / 100f);
        cm.postConcat(satMatrix);

        // Brightness / Contrast
        float scale = contrast / 100f;
        float trans = (brightness - 100f);
        float[] mat = new float[]{
                scale, 0, 0, 0, trans,
                0, scale, 0, 0, trans,
                0, 0, scale, 0, trans,
                0, 0, 0, 1, 0,
        };
        ColorMatrix bcMatrix = new ColorMatrix(mat);
        cm.postConcat(bcMatrix);

        Bitmap bmp = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(original, 0, 0, paint);

        return bmp;
    }

    /**
     * Applies rotation and flip transformations to a bitmap.
     *
     * @param original  the original bitmap
     * @param rotation  rotation angle in degrees
     * @param flipH     whether to flip horizontally
     * @param flipV     whether to flip vertically
     * @return a new bitmap with transformations applied
     */
    public Bitmap applyTransformations(Bitmap original, float rotation, boolean flipH, boolean flipV) {
        if (original == null) return null;

        Matrix matrix = new Matrix();

        float scaleX = flipH ? -1f : 1f;
        float scaleY = flipV ? -1f : 1f;
        matrix.postScale(scaleX, scaleY, original.getWidth() / 2f, original.getHeight() / 2f);
        matrix.postRotate(rotation, original.getWidth() / 2f, original.getHeight() / 2f);

        return Bitmap.createBitmap(
                original, 0, 0,
                original.getWidth(), original.getHeight(),
                matrix, true
        );
    }

    /**
     * Applies a simulated enhancement filter (brightness/contrast boost).
     *
     * @param original the original bitmap
     * @return a new bitmap with enhancement applied
     */
    public Bitmap applyEnhancement(Bitmap original) {
        if (original == null) return null;

        Bitmap bmp = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setColorFilter(new LightingColorFilter(0xFFDDDDDD, 0x00222222));
        canvas.drawBitmap(bmp, 0, 0, paint);
        return bmp;
    }
}
