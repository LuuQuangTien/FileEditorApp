package hcmute.edu.vn.documentfileeditor.Activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.ImageService;

public class EnhanceImageActivity extends AppCompatActivity {

    private ImageView imageView;
    private Bitmap originalBitmap;
    private ImageService imageService;
    
    private LinearLayout processingOverlay, badgeEnhanced, optionsContainer, resultsContainer;
    private MaterialButton btnReset, btnDownload, btnDownloadLarge, btnTryDifferent;

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

        loadImage();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        btnReset.setOnClickListener(v -> reset());
        btnTryDifferent.setOnClickListener(v -> reset());

        View.OnClickListener downloadAction = v -> 
            Toast.makeText(this, "Image Downloaded!", Toast.LENGTH_SHORT).show();
        btnDownload.setOnClickListener(downloadAction);
        btnDownloadLarge.setOnClickListener(downloadAction);

        View.OnClickListener enhanceAction = v -> simulateEnhancement();

        findViewById(R.id.btn_auto_enhance).setOnClickListener(enhanceAction);
        findViewById(R.id.btn_upscale).setOnClickListener(enhanceAction);
        findViewById(R.id.btn_denoise).setOnClickListener(enhanceAction);
        findViewById(R.id.btn_sharpen).setOnClickListener(enhanceAction);
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

    private void simulateEnhancement() {
        if (originalBitmap == null) return;
        
        // Show processing
        processingOverlay.setVisibility(View.VISIBLE);
        optionsContainer.setVisibility(View.GONE);
        
        // Simulate delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Bitmap enhanced = imageService.applyEnhancement(originalBitmap);
            if (enhanced != null) {
                imageView.setImageBitmap(enhanced);
            }
            
            processingOverlay.setVisibility(View.GONE);
            badgeEnhanced.setVisibility(View.VISIBLE);
            resultsContainer.setVisibility(View.VISIBLE);
            
            btnReset.setVisibility(View.VISIBLE);
            btnDownload.setEnabled(true);
        }, 1500);
    }

    private void reset() {
        imageView.setImageBitmap(originalBitmap);
        
        badgeEnhanced.setVisibility(View.GONE);
        resultsContainer.setVisibility(View.GONE);
        optionsContainer.setVisibility(View.VISIBLE);
        
        btnReset.setVisibility(View.GONE);
        btnDownload.setEnabled(false);
    }
}