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

import com.google.android.material.slider.Slider;

import java.io.InputStream;

import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.ImageService;

public class RotateImageActivity extends AppCompatActivity {

    private ImageView imageView;
    private Bitmap originalBitmap;
    private Bitmap currentBitmap;
    private ImageService imageService;

    private float currentRotation = 0f;
    private boolean isFlippedH = false;
    private boolean isFlippedV = false;

    private Slider sliderRotation;
    private TextView tvRotation;

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
        setContentView(R.layout.activity_rotate_image);

        imageService = new ImageService();

        imageView = findViewById(R.id.image_preview);
        sliderRotation = findViewById(R.id.slider_rotation);
        tvRotation = findViewById(R.id.tv_rotation);

        openGallery();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetAll());
        findViewById(R.id.btn_download).setOnClickListener(v -> saveImage());

        setupButtons();
        setupSlider();
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
                applyTransformations();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể mở ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupButtons() {
        findViewById(R.id.btn_rotate_ccw).setOnClickListener(v -> {
            currentRotation -= 90f;
            if (currentRotation < 0) currentRotation += 360f;
            sliderRotation.setValue(currentRotation);
        });

        findViewById(R.id.btn_rotate_cw).setOnClickListener(v -> {
            currentRotation += 90f;
            if (currentRotation > 360) currentRotation -= 360f;
            sliderRotation.setValue(currentRotation);
        });

        findViewById(R.id.btn_flip_h).setOnClickListener(v -> {
            isFlippedH = !isFlippedH;
            applyTransformations();
        });

        findViewById(R.id.btn_flip_v).setOnClickListener(v -> {
            isFlippedV = !isFlippedV;
            applyTransformations();
        });
    }

    private void setupSlider() {
        sliderRotation.addOnChangeListener((slider, value, fromUser) -> {
            currentRotation = value;
            tvRotation.setText("Rotation: " + (int) currentRotation + "°");
            applyTransformations();
        });
    }

    private void applyTransformations() {
        if (originalBitmap == null) return;
        currentBitmap = imageService.applyTransformations(originalBitmap, currentRotation, isFlippedH, isFlippedV);
        if (currentBitmap != null) {
            imageView.setImageBitmap(currentBitmap);
        }
    }

    private void saveImage() {
        Bitmap toSave = (currentBitmap != null) ? currentBitmap : originalBitmap;
        if (toSave == null) {
            Toast.makeText(this, "Không có ảnh để lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "Rotated_" + System.currentTimeMillis();
        Uri savedUri = imageService.saveBitmapToGallery(this, toSave, fileName);

        if (savedUri != null) {
            Toast.makeText(this, "Đã lưu ảnh vào Pictures/DocumentEditor", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Lưu ảnh thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetAll() {
        currentRotation = 0f;
        isFlippedH = false;
        isFlippedV = false;
        sliderRotation.setValue(0f);
        currentBitmap = null;
        applyTransformations();
    }
}
