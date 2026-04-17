package hcmute.edu.vn.documentfileeditor.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import hcmute.edu.vn.documentfileeditor.Activity.CropImageActivity;
import hcmute.edu.vn.documentfileeditor.Activity.EnhanceImageActivity;
import hcmute.edu.vn.documentfileeditor.Activity.ImageFilterActivity;
import hcmute.edu.vn.documentfileeditor.Activity.RotateImageActivity;
import hcmute.edu.vn.documentfileeditor.Activity.ScanActivity;
import hcmute.edu.vn.documentfileeditor.R;

public class ToolsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);

        // Document Tools
        setupToolItem(view, R.id.tool_pdf_word, R.drawable.ic_file_type, "PDF to Word", "Convert PDF to editable DOCX");
        setupToolItem(view, R.id.tool_pdf_excel, R.drawable.ic_file_type, "PDF to Excel", "Extract tables from PDF");
        setupToolItem(view, R.id.tool_image_pdf, R.drawable.ic_file_image, "Image to PDF", "Convert images to PDF");
        setupToolItem(view, R.id.tool_merge_pdf, R.drawable.ic_combine, "Merge PDF", "Combine multiple PDFs");
        setupToolItem(view, R.id.tool_split_pdf, R.drawable.ic_split, "Split PDF", "Split PDF into pages");
        setupToolItem(view, R.id.tool_annotate_pdf, R.drawable.ic_highlighter, "Annotate PDF", "Highlight & add notes");

        // Image Tools
        setupToolItem(view, R.id.tool_crop, R.drawable.ic_crop, "Crop Image", "Crop and resize images");
        setupToolItem(view, R.id.tool_rotate, R.drawable.ic_rotate_cw, "Rotate Image", "Rotate and flip images");
        setupToolItem(view, R.id.tool_filter, R.drawable.ic_wand, "Apply Filters", "Add filters and effects");
        setupToolItem(view, R.id.tool_scan, R.drawable.ic_file_image, "Document Scanner", "Scan docs with camera");
        setupToolItem(view, R.id.tool_enhance, R.drawable.ic_sparkles, "Enhance Image", "Improve clarity & quality");

        return view;
    }

    private void setupToolItem(View parentView, int includeId, int iconRes, String name, String desc) {
        View toolView = parentView.findViewById(includeId);
        if (toolView != null) {
            ImageView icon = toolView.findViewById(R.id.icon_tool);
            TextView tvName = toolView.findViewById(R.id.tv_tool_name);
            TextView tvDesc = toolView.findViewById(R.id.tv_tool_desc);
            if (icon != null) icon.setImageResource(iconRes);
            if (tvName != null) tvName.setText(name);
            if (tvDesc != null) tvDesc.setText(desc);
            
            toolView.setOnClickListener(v -> {
                android.content.Intent intent = null;
                if (includeId == R.id.tool_scan) {
                    intent = new android.content.Intent(getActivity(), ScanActivity.class);
                } else if (includeId == R.id.tool_crop) {
                    intent = new android.content.Intent(getActivity(), CropImageActivity.class);
                } else if (includeId == R.id.tool_rotate) {
                    intent = new android.content.Intent(getActivity(), RotateImageActivity.class);
                } else if (includeId == R.id.tool_filter) {
                    intent = new android.content.Intent(getActivity(), ImageFilterActivity.class);
                } else if (includeId == R.id.tool_enhance) {
                    intent = new android.content.Intent(getActivity(), EnhanceImageActivity.class);
                }

                if (intent != null) {
                    startActivity(intent);
                } else {
                    android.widget.Toast.makeText(getActivity(), name + " coming soon", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
