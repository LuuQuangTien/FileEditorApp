package hcmute.edu.vn.documentfileeditor.Activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import java.io.InputStream;

import hcmute.edu.vn.documentfileeditor.R;

public class CropImageActivity extends AppCompatActivity {

    private ImageView imagePreview;
    private Bitmap originalBitmap;
    private float currentRatio = 0f; // 0 means Free
    
    private Slider sliderWidth, sliderHeight;
    private TextView tvWidth, tvHeight;

    private MaterialButton btnFree, btn11, btn169, btn43, btn916;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);

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

        loadSampleImage();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetImage());
        findViewById(R.id.btn_download).setOnClickListener(v ->
                Toast.makeText(this, "Downloaded!", Toast.LENGTH_SHORT).show()
        );

        btnFree.setOnClickListener(v -> setAspectRatio(0f, btnFree));
        btn11.setOnClickListener(v -> setAspectRatio(1f, btn11));
        btn43.setOnClickListener(v -> setAspectRatio(4f/3f, btn43));
        btn169.setOnClickListener(v -> setAspectRatio(16f/9f, btn169));
        btn916.setOnClickListener(v -> setAspectRatio(9f/16f, btn916));

        sliderWidth.addOnChangeListener((slider, value, fromUser) -> {
            tvWidth.setText("Width: " + (int)value + "px");
            if (fromUser && currentRatio != 0) {
                float newHeight = value / currentRatio;
                if (newHeight <= sliderHeight.getValueTo() && newHeight >= sliderHeight.getValueFrom()) {
                    sliderHeight.setValue(newHeight);
                }
            }
        });

        sliderHeight.addOnChangeListener((slider, value, fromUser) -> {
            tvHeight.setText("Height: " + (int)value + "px");
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

    private void setAspectRatio(float ratio, MaterialButton selectedBtn) {
        currentRatio = ratio;
        
        btnFree.setStrokeColorResource(R.color.neutral_200);
        btn11.setStrokeColorResource(R.color.neutral_200);
        btn43.setStrokeColorResource(R.color.neutral_200);
        btn169.setStrokeColorResource(R.color.neutral_200);
        btn916.setStrokeColorResource(R.color.neutral_200);

        // Highlight selected
        // selectedBtn.setStrokeColorResource(R.color.blue_500); // Assuming blue_500 exists
        selectedBtn.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFF3B82F6)); // Tailwind blue-500

        if (ratio != 0) {
            float width = sliderWidth.getValue();
            float newHeight = width / ratio;
            if (newHeight <= sliderHeight.getValueTo() && newHeight >= sliderHeight.getValueFrom()) {
                sliderHeight.setValue(newHeight);
            }
        }
    }

    private void loadSampleImage() {
        try {
            InputStream is = getAssets().open("sample.jpg");
            originalBitmap = BitmapFactory.decodeStream(is);
            imagePreview.setImageBitmap(originalBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyCrop() {
        if (originalBitmap == null) return;
        Toast.makeText(this, "Crop applied with " + (int)sliderWidth.getValue() + "x" + (int)sliderHeight.getValue(), Toast.LENGTH_SHORT).show();
    }

    private void resetImage() {
        imagePreview.setImageBitmap(originalBitmap);
        setAspectRatio(0f, btnFree);
        sliderWidth.setValue(300);
        sliderHeight.setValue(300);
    }
}