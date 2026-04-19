package hcmute.edu.vn.documentfileeditor.Service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import hcmute.edu.vn.documentfileeditor.View.PdfAnnotationView;

public class PdfService {
    private static final String TAG = "PdfService";

    public interface PdfCallback {
        void onSuccess(String pdfLocalPath);
        void onFailure(String error);
    }

    /**
     * Merge multiple PDFs into one rasterized PDF.
     */
    public void mergePdfs(Context context, List<Uri> pdfUris, String outputFileName, PdfCallback callback) {
        new Thread(() -> {
            try {
                PdfDocument outputPdf = new PdfDocument();
                int pageCount = 1;

                for (Uri uri : pdfUris) {
                    ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                    if (pfd != null) {
                        PdfRenderer renderer = new PdfRenderer(pfd);
                        for (int i = 0; i < renderer.getPageCount(); i++) {
                            PdfRenderer.Page page = renderer.openPage(i);
                            
                            // A4 size
                            int width = 595;
                            int height = 842;
                            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                            bitmap.eraseColor(Color.WHITE);
                            
                            // Render page to bitmap
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                            
                            // Write bitmap to new PDF
                            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, pageCount++).create();
                            PdfDocument.Page outputPage = outputPdf.startPage(pageInfo);
                            Canvas canvas = outputPage.getCanvas();
                            canvas.drawBitmap(bitmap, 0, 0, null);
                            outputPdf.finishPage(outputPage);
                            
                            page.close();
                            bitmap.recycle();
                        }
                        renderer.close();
                        pfd.close();
                    }
                }

                File pdfDir = new File(context.getFilesDir(), "scanned_pdfs");
                if (!pdfDir.exists()) {
                    pdfDir.mkdirs();
                }
                String safeName = outputFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
                File resultPdf = new File(pdfDir, safeName + ".pdf");
                FileOutputStream fos = new FileOutputStream(resultPdf);
                outputPdf.writeTo(fos);
                outputPdf.close();
                fos.close();

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onSuccess(resultPdf.getAbsolutePath());
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to merge PDFs", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onFailure("Lỗi khi gộp PDF: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Split PDF by a comma-separated range string (e.g. "1,3,5-7").
     * Pages are 1-indexed.
     */
    public void splitPdf(Context context, Uri pdfUri, String rangeStr, String outputFileName, PdfCallback callback) {
        new Thread(() -> {
            try {
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(pdfUri, "r");
                if (pfd == null) {
                    throw new Exception("Could not open file");
                }
                PdfRenderer renderer = new PdfRenderer(pfd);
                int totalPages = renderer.getPageCount();

                // Parse range string
                boolean[] includePage = new boolean[totalPages];
                if (rangeStr == null || rangeStr.trim().isEmpty() || rangeStr.equalsIgnoreCase("all")) {
                    for (int i = 0; i < totalPages; i++) includePage[i] = true;
                } else {
                    String[] parts = rangeStr.split(",");
                    for (String part : parts) {
                        part = part.trim();
                        if (part.contains("-")) {
                            String[] bounds = part.split("-");
                            if (bounds.length == 2) {
                                int start = Integer.parseInt(bounds[0].trim()) - 1;
                                int end = Integer.parseInt(bounds[1].trim()) - 1;
                                for (int i = Math.max(0, start); i <= Math.min(totalPages - 1, end); i++) {
                                    includePage[i] = true;
                                }
                            }
                        } else {
                            try {
                                int p = Integer.parseInt(part) - 1;
                                if (p >= 0 && p < totalPages) includePage[p] = true;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                PdfDocument outputPdf = new PdfDocument();
                int outPageCount = 1;

                for (int i = 0; i < totalPages; i++) {
                    if (includePage[i]) {
                        PdfRenderer.Page page = renderer.openPage(i);
                        
                        int width = 595;
                        int height = 842;
                        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        bitmap.eraseColor(Color.WHITE);
                        
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        
                        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, outPageCount++).create();
                        PdfDocument.Page outputPage = outputPdf.startPage(pageInfo);
                        outputPage.getCanvas().drawBitmap(bitmap, 0, 0, null);
                        outputPdf.finishPage(outputPage);
                        
                        page.close();
                        bitmap.recycle();
                    }
                }
                renderer.close();
                pfd.close();

                File pdfDir = new File(context.getFilesDir(), "scanned_pdfs");
                if (!pdfDir.exists()) {
                    pdfDir.mkdirs();
                }
                String safeName = outputFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
                File resultPdf = new File(pdfDir, safeName + ".pdf");
                FileOutputStream fos = new FileOutputStream(resultPdf);
                outputPdf.writeTo(fos);
                outputPdf.close();
                fos.close();

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onSuccess(resultPdf.getAbsolutePath());
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to split PDF", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onFailure("Lỗi khi tách PDF: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Re-renders original PDF with annotations and saves it.
     */
    public void saveAnnotatedPdf(Context context, Uri pdfUri, Map<Integer, List<PdfAnnotationView.Annotation>> annotations, String outputFileName, PdfCallback callback) {
        new Thread(() -> {
            try {
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(pdfUri, "r");
                if (pfd == null) throw new Exception("Could not open file");
                PdfRenderer renderer = new PdfRenderer(pfd);
                PdfDocument outputPdf = new PdfDocument();

                Paint highlightPaint = new Paint();
                highlightPaint.setColor(Color.argb(128, 255, 255, 0));
                highlightPaint.setStyle(Paint.Style.FILL);

                Paint textPaint = new Paint();
                textPaint.setColor(Color.RED);
                textPaint.setTextSize(60f);
                textPaint.setFakeBoldText(true);

                for (int i = 0; i < renderer.getPageCount(); i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    int width = 595;
                    int height = 842;
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(Color.WHITE);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    
                    Canvas canvas = new Canvas(bitmap);
                    List<PdfAnnotationView.Annotation> pageAnns = annotations.get(i);
                    if (pageAnns != null) {
                        for (PdfAnnotationView.Annotation ann : pageAnns) {
                            if (ann.type == PdfAnnotationView.Annotation.Type.HIGHLIGHT) {
                                canvas.drawRect(ann.rect, highlightPaint);
                            } else if (ann.type == PdfAnnotationView.Annotation.Type.TEXT) {
                                canvas.drawText(ann.text, ann.point.x, ann.point.y, textPaint);
                            }
                        }
                    }

                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, i + 1).create();
                    PdfDocument.Page outputPage = outputPdf.startPage(pageInfo);
                    outputPage.getCanvas().drawBitmap(bitmap, 0, 0, null);
                    outputPdf.finishPage(outputPage);

                    page.close();
                    bitmap.recycle();
                }

                renderer.close();
                pfd.close();

                File pdfDir = new File(context.getFilesDir(), "scanned_pdfs");
                if (!pdfDir.exists()) pdfDir.mkdirs();
                String safeName = outputFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
                File resultPdf = new File(pdfDir, safeName + ".pdf");
                FileOutputStream fos = new FileOutputStream(resultPdf);
                outputPdf.writeTo(fos);
                outputPdf.close();
                fos.close();

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onSuccess(resultPdf.getAbsolutePath());
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to save annotated PDF", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onFailure("Lỗi khi lưu PDF: " + e.getMessage());
                });
            }
        }).start();
    }
}
