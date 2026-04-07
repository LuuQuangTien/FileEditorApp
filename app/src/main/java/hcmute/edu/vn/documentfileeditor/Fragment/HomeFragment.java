package hcmute.edu.vn.documentfileeditor.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import hcmute.edu.vn.documentfileeditor.Activity.DocumentEditorActivity;
import hcmute.edu.vn.documentfileeditor.Activity.OcrActivity;
import hcmute.edu.vn.documentfileeditor.Activity.ScanActivity;
import hcmute.edu.vn.documentfileeditor.Activity.TranslateActivity;
import hcmute.edu.vn.documentfileeditor.R;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        setupQuickActions(view);
        setupRecentFiles(view);
        
        return view;
    }

    private void setupQuickActions(View view) {
        View actionScan = view.findViewById(R.id.card_action_scan);
        View actionOcr = view.findViewById(R.id.card_action_ocr);
        View actionTranslate = view.findViewById(R.id.card_action_translate);

        if (actionScan != null) {
            actionScan.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(getActivity(), ScanActivity.class);
                startActivity(intent);
            });
        }
        
        if (actionOcr != null) {
            actionOcr.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(getActivity(), OcrActivity.class);
                startActivity(intent);
            });
        }
        
        if (actionTranslate != null) {
            actionTranslate.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(getActivity(), TranslateActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupRecentFiles(View view) {
        int[] recentIds = {R.id.card_recent_1, R.id.card_recent_2, R.id.card_recent_3};
        for (int id : recentIds) {
            View card = view.findViewById(id);
            if (card != null) {
                card.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(getActivity(), DocumentEditorActivity.class);
                    startActivity(intent);
                });
            }
        }
    }
}
