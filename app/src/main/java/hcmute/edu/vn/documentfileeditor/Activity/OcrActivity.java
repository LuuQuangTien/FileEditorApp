package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

import hcmute.edu.vn.documentfileeditor.R;

public class OcrActivity extends AppCompatActivity {

    private EditText etExtractedText;
    private TextView tvFileName, tvFileSize;
    private TextRecognizer recognizer;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        // Giữ nguyên nút Back
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Khởi tạo các View từ UI của bạn
        MaterialButton btnSelectFile = findViewById(R.id.btn_select_file);
        MaterialButton btnCopy = findViewById(R.id.btn_copy);
        etExtractedText = findViewById(R.id.et_extracted_text);
        tvFileName = findViewById(R.id.tv_file_name_ocr);
        tvFileSize = findViewById(R.id.tv_file_size_ocr);

        // Khởi tạo ML Kit
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Đăng ký ActivityResultLauncher để chọn ảnh
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            processImage(imageUri);
                        }
                    }
                }
        );

        if (btnSelectFile != null) {
            btnSelectFile.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickImageLauncher.launch(intent);
            });
        }

        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                String text = etExtractedText.getText().toString();
                if (!text.isEmpty()) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Extracted Text", text);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Đã sao chép văn bản!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void processImage(Uri uri) {
        try {
            if (tvFileName != null) tvFileName.setText(uri.getLastPathSegment());
            if (tvFileSize != null) tvFileSize.setText("Đang xử lý...");
            if (etExtractedText != null) etExtractedText.setText("Đang quét chữ, vui lòng đợi...");

            InputImage image = InputImage.fromFilePath(this, uri);
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        if (etExtractedText != null) {
                            String result = visionText.getText();
                            etExtractedText.setText(result.isEmpty() ? "Không tìm thấy văn bản nào." : result);
                        }
                        if (tvFileSize != null) tvFileSize.setText("Xong");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi OCR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
