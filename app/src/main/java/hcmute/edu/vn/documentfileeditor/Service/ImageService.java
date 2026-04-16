package hcmute.edu.vn.documentfileeditor.Service;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Service layer encapsulating image processing business logic.
 * Decouples image transformation algorithms from Activity lifecycle,
 * making them testable and reusable.
 */
public class ImageService {

    private static final String TAG = "ImageService";

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

    /**
     * Crops a bitmap to the specified width and height, centered.
     *
     * @param original    the original bitmap
     * @param targetWidth desired width in pixels
     * @param targetHeight desired height in pixels
     * @return a new cropped bitmap
     */
    public Bitmap cropBitmap(Bitmap original, int targetWidth, int targetHeight) {
        if (original == null) return null;

        int srcW = original.getWidth();
        int srcH = original.getHeight();

        // Clamp target dimensions to source
        int cropW = Math.min(targetWidth, srcW);
        int cropH = Math.min(targetHeight, srcH);

        // Center crop
        int x = (srcW - cropW) / 2;
        int y = (srcH - cropH) / 2;

        return Bitmap.createBitmap(original, x, y, cropW, cropH);
    }

    /**
     * Saves a bitmap to the device's Pictures gallery using MediaStore (Android 10+)
     * or direct file write (Android 9 and below).
     *
     * @param context  application context
     * @param bitmap   the bitmap to save
     * @param fileName desired file name (without extension)
     * @return the saved file Uri, or null if failed
     */
    public Uri saveBitmapToGallery(Context context, Bitmap bitmap, String fileName) {
        if (bitmap == null || context == null) return null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ — use MediaStore (no WRITE_EXTERNAL_STORAGE needed)
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + ".png");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DocumentEditor");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream os = context.getContentResolver().openOutputStream(uri);
                    if (os != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                        os.flush();
                        os.close();
                    }
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    context.getContentResolver().update(uri, values, null, null);
                    Log.d(TAG, "Image saved to gallery: " + uri);
                    return uri;
                }
            } else {
                // Android 9 and below — direct file write
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "DocumentEditor");
                if (!dir.exists()) dir.mkdirs();

                File file = new File(dir, fileName + ".png");
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                Log.d(TAG, "Image saved to: " + file.getAbsolutePath());
                return Uri.fromFile(file);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save image: " + e.getMessage());
        }
        return null;
    }
}
