package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;

import java.io.InputStream;

import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.ImageService;

public class ImageFilterActivity extends AppCompatActivity {

    private ImageView imageView;
    private Bitmap originalBitmap;
    private Bitmap filteredBitmap;
    private ImageService imageService;

    private Slider sliderBrightness, sliderContrast, sliderSaturation, sliderBlur;
    private TextView tvBrightness, tvContrast, tvSaturation, tvBlur;

    private float currentBrightness = 100f;
    private float currentContrast = 100f;
    private float currentSaturation = 100f;

    private ColorMatrix baseFilterMatrix = new ColorMatrix(); // Default is identity

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
        setContentView(R.layout.activity_image_filter);

        imageService = new ImageService();
        imageView = findViewById(R.id.image_preview);

        sliderBrightness = findViewById(R.id.slider_brightness);
        sliderContrast = findViewById(R.id.slider_contrast);
        sliderSaturation = findViewById(R.id.slider_saturation);
        sliderBlur = findViewById(R.id.slider_blur);

        tvBrightness = findViewById(R.id.tv_brightness);
        tvContrast = findViewById(R.id.tv_contrast);
        tvSaturation = findViewById(R.id.tv_saturation);
        tvBlur = findViewById(R.id.tv_blur);

        openGallery();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetAll());
        findViewById(R.id.btn_download).setOnClickListener(v -> saveImage());

        setupFilterButtons();
        setupSliders();
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
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể mở ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFilterButtons() {
        findViewById(R.id.btn_filter_none).setOnClickListener(v -> {
            baseFilterMatrix.reset();
            applyFilters();
        });
        findViewById(R.id.btn_filter_bw).setOnClickListener(v -> {
            baseFilterMatrix.setSaturation(0);
            applyFilters();
        });
        findViewById(R.id.btn_filter_sepia).setOnClickListener(v -> {
            baseFilterMatrix.setSaturation(0);
            ColorMatrix sepia = new ColorMatrix();
            sepia.setScale(1f, .95f, .82f, 1f);
            baseFilterMatrix.postConcat(sepia);
            applyFilters();
        });
        findViewById(R.id.btn_filter_vintage).setOnClickListener(v -> {
            baseFilterMatrix.setSaturation(0.5f);
            ColorMatrix vintage = new ColorMatrix();
            vintage.setScale(1.1f, 0.9f, 0.7f, 1f);
            baseFilterMatrix.postConcat(vintage);
            applyFilters();
        });
        findViewById(R.id.btn_filter_cool).setOnClickListener(v -> {
            baseFilterMatrix.reset();
            ColorMatrix cool = new ColorMatrix();
            cool.setScale(0.8f, 0.8f, 1.2f, 1f);
            baseFilterMatrix.postConcat(cool);
            applyFilters();
        });
    }

    private void setupSliders() {
        sliderBrightness.addOnChangeListener((slider, value, fromUser) -> {
            currentBrightness = value;
            tvBrightness.setText("Brightness: " + (int) value + "%");
            applyFilters();
        });

        sliderContrast.addOnChangeListener((slider, value, fromUser) -> {
            currentContrast = value;
            tvContrast.setText("Contrast: " + (int) value + "%");
            applyFilters();
        });

        sliderSaturation.addOnChangeListener((slider, value, fromUser) -> {
            currentSaturation = value;
            tvSaturation.setText("Saturation: " + (int) value + "%");
            applyFilters();
        });

        sliderBlur.addOnChangeListener((slider, value, fromUser) -> {
            tvBlur.setText("Blur: " + (int) value + "px");
            applyFilters();
        });
    }

    private void applyFilters() {
        if (originalBitmap == null) return;
        filteredBitmap = imageService.applyFilters(originalBitmap, baseFilterMatrix,
                currentBrightness, currentContrast, currentSaturation);
        if (filteredBitmap != null) {
            imageView.setImageBitmap(filteredBitmap);
        }
    }

    private void saveImage() {
        Bitmap toSave = (filteredBitmap != null) ? filteredBitmap : originalBitmap;
        if (toSave == null) {
            Toast.makeText(this, "Không có ảnh để lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "Filtered_" + System.currentTimeMillis();
        Uri savedUri = imageService.saveBitmapToGallery(this, toSave, fileName);

        if (savedUri != null) {
            Toast.makeText(this, "Đã lưu ảnh vào Pictures/DocumentEditor", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Lưu ảnh thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetAll() {
        baseFilterMatrix.reset();
        filteredBitmap = null;

        sliderBrightness.setValue(100);
        sliderContrast.setValue(100);
        sliderSaturation.setValue(100);
        sliderBlur.setValue(0);

        currentBrightness = 100f;
        currentContrast = 100f;
        currentSaturation = 100f;

        tvBrightness.setText("Brightness: 100%");
        tvContrast.setText("Contrast: 100%");
        tvSaturation.setText("Saturation: 100%");
        tvBlur.setText("Blur: 0px");

        if (originalBitmap != null) {
            imageView.setImageBitmap(originalBitmap);
        }
    }
}
