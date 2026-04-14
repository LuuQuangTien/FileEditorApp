package hcmute.edu.vn.documentfileeditor.Service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Service layer encapsulating document scanning business logic.
 * Handles OCR text extraction and PDF generation from scanned images.
 */
public class ScanService {

    private static final String TAG = "ScanService";

    public static final int SCRIPT_LATIN = 0;
    public static final int SCRIPT_CHINESE = 1;
    public static final int SCRIPT_JAPANESE = 2;

    private TextRecognizer latinRecognizer;
    private TextRecognizer chineseRecognizer;
    private TextRecognizer japaneseRecognizer;

    public ScanService() {
        latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    /**
     * Returns a TextRecognizer for the specified script.
     */
    private TextRecognizer getRecognizer(int scriptType) {
        switch (scriptType) {
            case SCRIPT_CHINESE:
                if (chineseRecognizer == null) {
                    chineseRecognizer = TextRecognition.getClient(
                            new ChineseTextRecognizerOptions.Builder().build());
                }
                return chineseRecognizer;
            case SCRIPT_JAPANESE:
                if (japaneseRecognizer == null) {
                    japaneseRecognizer = TextRecognition.getClient(
                            new JapaneseTextRecognizerOptions.Builder().build());
                }
                return japaneseRecognizer;
            default:
                return latinRecognizer;
        }
    }

    /**
     * Extracts text from an image URI using ML Kit OCR.
     *
     * @param context    application context
     * @param imageUri   the URI of the image to process
     * @param scriptType one of SCRIPT_LATIN, SCRIPT_CHINESE, SCRIPT_JAPANESE
     * @param callback   result callback
     */
    public void extractText(Context context, Uri imageUri, int scriptType, OcrCallback callback) {
        try {
            InputImage image = InputImage.fromFilePath(context, imageUri);
            TextRecognizer recognizer = getRecognizer(scriptType);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String result = visionText.getText();
                        if (result == null || result.trim().isEmpty()) {
                            callback.onSuccess("Không tìm thấy văn bản. Hãy thử chụp lại với ánh sáng tốt hơn.");
                        } else {
                            callback.onSuccess(result);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "OCR failed", e);
                        callback.onFailure("Lỗi OCR: " + e.getMessage());
                    });
        } catch (IOException e) {
            Log.e(TAG, "Failed to load image", e);
            callback.onFailure("Không thể đọc ảnh: " + e.getMessage());
        }
    }

    /**
     * Saves a bitmap as a PDF file.
     *
     * @param context  application context
     * @param bitmap   the scanned image bitmap
     * @param fileName the desired file name (without extension)
     * @param callback result callback
     */
    public void saveAsPdf(Context context, Bitmap bitmap, String fileName, SaveCallback callback) {
        if (bitmap == null) {
            callback.onFailure("Không có ảnh để lưu");
            return;
        }

        try {
            PdfDocument pdfDocument = new PdfDocument();

            // Scale to A4 (595 x 842 points at 72dpi)
            int pageWidth = 595;
            int pageHeight = 842;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            // Scale bitmap to fit page with aspect ratio
            float bitmapAspect = (float) bitmap.getWidth() / bitmap.getHeight();
            float pageAspect = (float) pageWidth / pageHeight;

            float scaledWidth, scaledHeight;
            float offsetX = 0, offsetY = 0;

            if (bitmapAspect > pageAspect) {
                // Bitmap is wider — fit to width
                scaledWidth = pageWidth;
                scaledHeight = pageWidth / bitmapAspect;
                offsetY = (pageHeight - scaledHeight) / 2;
            } else {
                // Bitmap is taller — fit to height
                scaledHeight = pageHeight;
                scaledWidth = pageHeight * bitmapAspect;
                offsetX = (pageWidth - scaledWidth) / 2;
            }

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                    bitmap, (int) scaledWidth, (int) scaledHeight, true);
            page.getCanvas().drawBitmap(scaledBitmap, offsetX, offsetY, null);
            pdfDocument.finishPage(page);

            // Save to app-specific directory
            File pdfDir = new File(context.getFilesDir(), "scanned_pdfs");
            if (!pdfDir.exists()) {
                pdfDir.mkdirs();
            }

            String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            File pdfFile = new File(pdfDir, safeName + ".pdf");

            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            fos.close();
            pdfDocument.close();

            Log.d(TAG, "PDF saved: " + pdfFile.getAbsolutePath());
            callback.onSuccess(pdfFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "PDF save failed", e);
            callback.onFailure("Lỗi lưu PDF: " + e.getMessage());
        }
    }

    /**
     * Closes all recognizers to free resources.
     */
    public void close() {
        if (latinRecognizer != null) latinRecognizer.close();
        if (chineseRecognizer != null) chineseRecognizer.close();
        if (japaneseRecognizer != null) japaneseRecognizer.close();
    }

    /**
     * Callback for OCR text extraction results.
     */
    public interface OcrCallback {
        void onSuccess(String extractedText);
        void onFailure(String errorMessage);
    }

    /**
     * Callback for PDF save results.
     */
    public interface SaveCallback {
        void onSuccess(String filePath);
        void onFailure(String errorMessage);
    }
}
