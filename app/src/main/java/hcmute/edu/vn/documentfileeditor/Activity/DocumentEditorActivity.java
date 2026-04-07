package hcmute.edu.vn.documentfileeditor.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import hcmute.edu.vn.documentfileeditor.R;

public class DocumentEditorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_editor);

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Save button
        MaterialButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> Toast.makeText(this, "Document saved!", Toast.LENGTH_SHORT).show());

        // More button
        ImageView btnMore = findViewById(R.id.btn_more);
        btnMore.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(this, v);
            popup.getMenu().add("Share");
            popup.getMenu().add("Export as PDF");
            popup.getMenu().add("Download");
            popup.setOnMenuItemClickListener(item -> {
                Toast.makeText(this, item.getTitle() + " coming soon", Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        });

        // Formatting buttons with toggle highlight
        setupFormatToggle(R.id.btn_bold);
        setupFormatToggle(R.id.btn_italic);
        setupFormatToggle(R.id.btn_underline);

        // Non-toggle formatting
        int[] simpleButtons = {
            R.id.btn_align_left, R.id.btn_align_center, R.id.btn_align_right,
            R.id.btn_list, R.id.btn_list_ordered, R.id.btn_insert_image
        };
        for (int id : simpleButtons) {
            findViewById(id).setOnClickListener(v ->
                Toast.makeText(this, "Format action", Toast.LENGTH_SHORT).show()
            );
        }

        // AI FAB
        FrameLayout fabAi = findViewById(R.id.fab_ai);
        fabAi.setOnClickListener(v -> showAIBottomSheet());
    }

    private void setupFormatToggle(int viewId) {
        ImageView btn = findViewById(viewId);
        btn.setOnClickListener(v -> {
            boolean selected = !v.isSelected();
            v.setSelected(selected);
            if (selected) {
                v.setBackgroundColor(getResources().getColor(R.color.blue_100, null));
            } else {
                v.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        });
    }

    private void showAIBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_ai_assistant, null);
        dialog.setContentView(sheetView);

        LinearLayout suggestionArea = sheetView.findViewById(R.id.ai_suggestion_area);
        TextView tvSuggestion = sheetView.findViewById(R.id.tv_ai_suggestion);

        // AI Action buttons
        sheetView.findViewById(R.id.btn_ai_improve).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText("Enhanced version:\n\nThis document serves as an exemplary template. Feel free to modify this content and leverage advanced AI capabilities to refine your composition.\n\nAccess the AI assistant to receive professional writing guidance, comprehensive grammar corrections, and intelligent content recommendations.");
        });

        sheetView.findViewById(R.id.btn_ai_fix).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText("Grammar corrections applied:\n- Fixed punctuation\n- Corrected spelling\n- Improved sentence structure\n\nYour text is now grammatically correct!");
        });

        sheetView.findViewById(R.id.btn_ai_shorten).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText("Condensed version:\n\nEditable sample document. Use AI features for writing improvements.\n\nClick AI button for assistance, corrections, and suggestions.");
        });

        sheetView.findViewById(R.id.btn_ai_expand).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText("Expanded version:\n\nThis comprehensive document serves as a detailed sample template for your editing needs. You have complete freedom to modify and customize this text according to your requirements. Additionally, you can harness the power of artificial intelligence features to significantly enhance and improve the quality of your writing.");
        });

        sheetView.findViewById(R.id.btn_ai_translate).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.VISIBLE);
            tvSuggestion.setText("Translated to Vietnamese:\n\nĐây là một tài liệu mẫu. Bạn có thể chỉnh sửa văn bản này và sử dụng các tính năng AI để cải thiện bài viết của mình.\n\nNhấp vào nút AI để nhận trợ giúp viết, sửa lỗi ngữ pháp và đề xuất nội dung.");
        });

        sheetView.findViewById(R.id.btn_ai_edit_image).setOnClickListener(v -> {
            Toast.makeText(this, "Edit Image coming soon", Toast.LENGTH_SHORT).show();
        });

        // Apply suggestion
        sheetView.findViewById(R.id.btn_apply_suggestion).setOnClickListener(v -> {
            String suggestion = tvSuggestion.getText().toString();
            if (!suggestion.isEmpty()) {
                android.widget.EditText editor = findViewById(R.id.et_editor);
                String[] lines = suggestion.split("\n", 2);
                if (lines.length > 1) {
                    editor.setText(lines[1].trim());
                } else {
                    editor.setText(suggestion);
                }
                Toast.makeText(this, "AI suggestion applied!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        // Dismiss suggestion
        sheetView.findViewById(R.id.btn_dismiss_suggestion).setOnClickListener(v -> {
            suggestionArea.setVisibility(View.GONE);
            tvSuggestion.setText("");
        });

        dialog.show();
    }
}
