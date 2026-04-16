package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import java.io.InputStream;

import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.ImageService;

public class CropImageActivity extends AppCompatActivity {

    private ImageView imagePreview;
    private Bitmap originalBitmap;
    private Bitmap croppedBitmap;
    private ImageService imageService;
    private float currentRatio = 0f; // 0 means Free

    private Slider sliderWidth, sliderHeight;
    private TextView tvWidth, tvHeight;

    private MaterialButton btnFree, btn11, btn169, btn43, btn916;

    // Launcher để chọn ảnh từ Gallery
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
        setContentView(R.layout.activity_crop_image);

        imageService = new ImageService();
        imagePreview = findViewById(R.id.image_preview);
        sliderWidth = findViewById(R.id.slider_width);
        sliderHeight = findViewById(R.id.slider_height);
        tvWidth = findViewById(R.id.tv_width);
        tvHeight = findViewById(R.id.tv_height);

        btnFree = findViewById(R.id.btn_free);
        btn11 = findViewById(R.id.btn_1_1);
        btn169 = findViewById(R.id.btn_16_9);
        btn43 = findViewById(R.id.btn_4_3);
        btn916 = findViewById(R.id.btn_9_16);

        // Mở Gallery ngay khi vào — không dùng ảnh sample nữa
        openGallery();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetImage());

        // Nút Download — lưu ảnh vào Gallery
        findViewById(R.id.btn_download).setOnClickListener(v -> saveImage());

        btnFree.setOnClickListener(v -> setAspectRatio(0f, btnFree));
        btn11.setOnClickListener(v -> setAspectRatio(1f, btn11));
        btn43.setOnClickListener(v -> setAspectRatio(4f / 3f, btn43));
        btn169.setOnClickListener(v -> setAspectRatio(16f / 9f, btn169));
        btn916.setOnClickListener(v -> setAspectRatio(9f / 16f, btn916));

        sliderWidth.addOnChangeListener((slider, value, fromUser) -> {
            tvWidth.setText("Width: " + (int) value + "px");
            if (fromUser && currentRatio != 0) {
                float newHeight = value / currentRatio;
                if (newHeight <= sliderHeight.getValueTo() && newHeight >= sliderHeight.getValueFrom()) {
                    sliderHeight.setValue(newHeight);
                }
            }
        });

        sliderHeight.addOnChangeListener((slider, value, fromUser) -> {
            tvHeight.setText("Height: " + (int) value + "px");
            if (fromUser && currentRatio != 0) {
                float newWidth = value * currentRatio;
                if (newWidth <= sliderWidth.getValueTo() && newWidth >= sliderWidth.getValueFrom()) {
                    sliderWidth.setValue(newWidth);
                }
            }
        });

        findViewById(R.id.btn_apply).setOnClickListener(v -> applyCrop());

        // Initialize default selection
        setAspectRatio(0f, btnFree);
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
                imagePreview.setImageBitmap(originalBitmap);

                // Cập nhật slider theo kích thước ảnh thật
                float maxDim = Math.max(originalBitmap.getWidth(), originalBitmap.getHeight());
                sliderWidth.setValueTo(maxDim);
                sliderHeight.setValueTo(maxDim);
                sliderWidth.setValue(Math.min(originalBitmap.getWidth(), maxDim));
                sliderHeight.setValue(Math.min(originalBitmap.getHeight(), maxDim));
                tvWidth.setText("Width: " + originalBitmap.getWidth() + "px");
                tvHeight.setText("Height: " + originalBitmap.getHeight() + "px");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể mở ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void setAspectRatio(float ratio, MaterialButton selectedBtn) {
        currentRatio = ratio;

        btnFree.setStrokeColorResource(R.color.neutral_200);
        btn11.setStrokeColorResource(R.color.neutral_200);
        btn43.setStrokeColorResource(R.color.neutral_200);
        btn169.setStrokeColorResource(R.color.neutral_200);
        btn916.setStrokeColorResource(R.color.neutral_200);

        selectedBtn.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFF3B82F6));

        if (ratio != 0) {
            float width = sliderWidth.getValue();
            float newHeight = width / ratio;
            if (newHeight <= sliderHeight.getValueTo() && newHeight >= sliderHeight.getValueFrom()) {
                sliderHeight.setValue(newHeight);
            }
        }
    }

    private void applyCrop() {
        if (originalBitmap == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh trước", Toast.LENGTH_SHORT).show();
            return;
        }

        int targetW = (int) sliderWidth.getValue();
        int targetH = (int) sliderHeight.getValue();

        croppedBitmap = imageService.cropBitmap(originalBitmap, targetW, targetH);

        if (croppedBitmap != null) {
            imagePreview.setImageBitmap(croppedBitmap);
            Toast.makeText(this, "Đã cắt ảnh: " + croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImage() {
        Bitmap toSave = (croppedBitmap != null) ? croppedBitmap : originalBitmap;
        if (toSave == null) {
            Toast.makeText(this, "Không có ảnh để lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "Cropped_" + System.currentTimeMillis();
        Uri savedUri = imageService.saveBitmapToGallery(this, toSave, fileName);

        if (savedUri != null) {
            Toast.makeText(this, "Đã lưu ảnh vào Pictures/DocumentEditor", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Lưu ảnh thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetImage() {
        croppedBitmap = null;
        if (originalBitmap != null) {
            imagePreview.setImageBitmap(originalBitmap);
            sliderWidth.setValue(Math.min(originalBitmap.getWidth(), sliderWidth.getValueTo()));
            sliderHeight.setValue(Math.min(originalBitmap.getHeight(), sliderHeight.getValueTo()));
        }
        setAspectRatio(0f, btnFree);
    }
}