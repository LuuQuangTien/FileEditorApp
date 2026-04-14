package hcmute.edu.vn.documentfileeditor.Activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;

import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.ImageService;

public class RotateImageActivity extends AppCompatActivity {

    private ImageView imageView;
    private Bitmap originalBitmap;
    private ImageService imageService;

    private float currentRotation = 0f;
    private boolean isFlippedH = false;
    private boolean isFlippedV = false;

    private Slider sliderRotation;
    private TextView tvRotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotate_image);

        imageService = new ImageService();

        imageView = findViewById(R.id.image_preview);
        sliderRotation = findViewById(R.id.slider_rotation);
        tvRotation = findViewById(R.id.tv_rotation);

        loadImage();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetAll());
        findViewById(R.id.btn_download).setOnClickListener(v ->
                Toast.makeText(this, "Image Downloaded!", Toast.LENGTH_SHORT).show()
        );

        setupButtons();
        setupSlider();
    }

    private void loadImage() {
        try {
            originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sample);
            if (originalBitmap != null) {
                applyTransformations();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        Bitmap result = imageService.applyTransformations(originalBitmap, currentRotation, isFlippedH, isFlippedV);
        if (result != null) {
            imageView.setImageBitmap(result);
        }
    }

    private void resetAll() {
        currentRotation = 0f;
        isFlippedH = false;
        isFlippedV = false;
        sliderRotation.setValue(0f);
        applyTransformations();
    }
}
