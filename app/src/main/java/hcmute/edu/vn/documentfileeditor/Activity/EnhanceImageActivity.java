package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.InputStream;

import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.ImageService;

public class EnhanceImageActivity extends AppCompatActivity {

    private ImageView imageView;
    private Bitmap originalBitmap;
    private Bitmap enhancedBitmap;
    private ImageService imageService;

    private LinearLayout processingOverlay, badgeEnhanced, optionsContainer, resultsContainer;
    private MaterialButton btnReset, btnDownload, btnDownloadLarge, btnTryDifferent;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        loadImageFromUri(imageUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enhance_image);

        imageService = new ImageService();

        imageView = findViewById(R.id.image_preview);
        processingOverlay = findViewById(R.id.processing_overlay);
        badgeEnhanced = findViewById(R.id.badge_enhanced);
        optionsContainer = findViewById(R.id.options_container);
        resultsContainer = findViewById(R.id.results_container);

        btnReset = findViewById(R.id.btn_reset);
        btnDownload = findViewById(R.id.btn_download);
        btnDownloadLarge = findViewById(R.id.btn_download_large);
        btnTryDifferent = findViewById(R.id.btn_try_different);

        openGallery();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnReset.setOnClickListener(v -> reset());
        btnTryDifferent.setOnClickListener(v -> {
            reset();
            openGallery();
        });

        // Nút lưu ảnh thật
        View.OnClickListener saveAction = v -> saveImage();
        btnDownload.setOnClickListener(saveAction);
        btnDownloadLarge.setOnClickListener(saveAction);

        View.OnClickListener enhanceAction = v -> performEnhancement();

        findViewById(R.id.btn_auto_enhance).setOnClickListener(enhanceAction);
        findViewById(R.id.btn_upscale).setOnClickListener(enhanceAction);
        findViewById(R.id.btn_denoise).setOnClickListener(enhanceAction);
        findViewById(R.id.btn_sharpen).setOnClickListener(enhanceAction);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void loadImageFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            originalBitmap = BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (originalBitmap != null) {
                imageView.setImageBitmap(originalBitmap);
                // Hiện lại options nếu đang ẩn
                optionsContainer.setVisibility(View.VISIBLE);
                resultsContainer.setVisibility(View.GONE);
                badgeEnhanced.setVisibility(View.GONE);
                btnReset.setVisibility(View.GONE);
                btnDownload.setEnabled(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể mở ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void performEnhancement() {
        if (originalBitmap == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh trước", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show processing overlay
        processingOverlay.setVisibility(View.VISIBLE);
        optionsContainer.setVisibility(View.GONE);

        // Xử lý trên background thread rồi cập nhật UI
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            enhancedBitmap = imageService.applyEnhancement(originalBitmap);
            if (enhancedBitmap != null) {
                imageView.setImageBitmap(enhancedBitmap);
            }

            processingOverlay.setVisibility(View.GONE);
            badgeEnhanced.setVisibility(View.VISIBLE);
            resultsContainer.setVisibility(View.VISIBLE);

            btnReset.setVisibility(View.VISIBLE);
            btnDownload.setEnabled(true);
        }, 1500);
    }

    private void saveImage() {
        Bitmap toSave = (enhancedBitmap != null) ? enhancedBitmap : originalBitmap;
        if (toSave == null) {
            Toast.makeText(this, "Không có ảnh để lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "Enhanced_" + System.currentTimeMillis();
        Uri savedUri = imageService.saveBitmapToGallery(this, toSave, fileName);

        if (savedUri != null) {
            Toast.makeText(this, "Đã lưu ảnh vào Pictures/DocumentEditor", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Lưu ảnh thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    private void reset() {
        enhancedBitmap = null;
        if (originalBitmap != null) {
            imageView.setImageBitmap(originalBitmap);
        }

        badgeEnhanced.setVisibility(View.GONE);
        resultsContainer.setVisibility(View.GONE);
        optionsContainer.setVisibility(View.VISIBLE);

        btnReset.setVisibility(View.GONE);
        btnDownload.setEnabled(false);
    }
}