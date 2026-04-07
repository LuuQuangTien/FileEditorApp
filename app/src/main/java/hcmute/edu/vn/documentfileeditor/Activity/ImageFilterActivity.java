package hcmute.edu.vn.documentfileeditor.Activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import hcmute.edu.vn.documentfileeditor.R;

public class ImageFilterActivity extends AppCompatActivity {

    private ImageView imageView;
    private Bitmap originalBitmap;

    private Slider sliderBrightness, sliderContrast, sliderSaturation, sliderBlur;
    private TextView tvBrightness, tvContrast, tvSaturation, tvBlur;

    private float currentBrightness = 100f;
    private float currentContrast = 100f;
    private float currentSaturation = 100f;

    private ColorMatrix baseFilterMatrix = new ColorMatrix(); // Default is identity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_filter);

        imageView = findViewById(R.id.image_preview);

        sliderBrightness = findViewById(R.id.slider_brightness);
        sliderContrast = findViewById(R.id.slider_contrast);
        sliderSaturation = findViewById(R.id.slider_saturation);
        sliderBlur = findViewById(R.id.slider_blur);

        tvBrightness = findViewById(R.id.tv_brightness);
        tvContrast = findViewById(R.id.tv_contrast);
        tvSaturation = findViewById(R.id.tv_saturation);
        tvBlur = findViewById(R.id.tv_blur);

        loadImage();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_reset).setOnClickListener(v -> resetAll());
        findViewById(R.id.btn_download).setOnClickListener(v ->
            Toast.makeText(this, "Image Downloaded!", Toast.LENGTH_SHORT).show()
        );

        setupFilterButtons();
        setupSliders();
    }

    private void loadImage() {
        try {
            originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sample);
            if (originalBitmap != null) {
                imageView.setImageBitmap(originalBitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        // Simplistic approximations for others
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
            tvBrightness.setText("Brightness: " + (int)value + "%");
            applyFilters();
        });

        sliderContrast.addOnChangeListener((slider, value, fromUser) -> {
            currentContrast = value;
            tvContrast.setText("Contrast: " + (int)value + "%");
            applyFilters();
        });

        sliderSaturation.addOnChangeListener((slider, value, fromUser) -> {
            currentSaturation = value;
            tvSaturation.setText("Saturation: " + (int)value + "%");
            applyFilters();
        });
        
        sliderBlur.addOnChangeListener((slider, value, fromUser) -> {
            tvBlur.setText("Blur: " + (int)value + "px");
            // Native blur without RenderScript/BlurMaskFilter is complex; simulating placeholder logic.
            // A full implementation might use Toolkit or RenderScript
            applyFilters(); 
        });
    }

    private void applyFilters() {
        if (originalBitmap == null) return;

        ColorMatrix cm = new ColorMatrix();
        cm.postConcat(baseFilterMatrix);

        // Saturation
        ColorMatrix satMatrix = new ColorMatrix();
        satMatrix.setSaturation(currentSaturation / 100f);
        cm.postConcat(satMatrix);

        // Brightness / Contrast
        // Simplified matrix
        float scale = currentContrast / 100f;
        float trans = (currentBrightness - 100f);
        float[] mat = new float[] {
                scale, 0, 0, 0, trans,
                0, scale, 0, 0, trans,
                0, 0, scale, 0, trans,
                0, 0, 0, 1, 0,
        };
        ColorMatrix bcMatrix = new ColorMatrix(mat);
        cm.postConcat(bcMatrix);

        Bitmap bmp = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(originalBitmap, 0, 0, paint);

        imageView.setImageBitmap(bmp);
    }

    private void resetAll() {
        baseFilterMatrix.reset();
        
        sliderBrightness.setValue(100);
        sliderContrast.setValue(100);
        sliderSaturation.setValue(100);
        sliderBlur.setValue(0);
        
        loadInitialState();
    }
    
    private void loadInitialState() {
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
