package hcmute.edu.vn.documentfileeditor.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import hcmute.edu.vn.documentfileeditor.Activity.DocumentEditorActivity;
import hcmute.edu.vn.documentfileeditor.Activity.ExcelEditorActivity;
import hcmute.edu.vn.documentfileeditor.R;

public class DocumentsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_documents, container, false);

        setupFileClick(view, R.id.card_doc_1, DocumentEditorActivity.class);
        setupFileClick(view, R.id.card_doc_2, DocumentEditorActivity.class);
        setupFileClick(view, R.id.card_doc_3, ExcelEditorActivity.class);

        return view;
    }

    private void setupFileClick(View view, int viewId, Class<?> activityClass) {
        View card = view.findViewById(viewId);
        if (card != null) {
            card.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(getActivity(), activityClass);
                startActivity(intent);
            });
        }
    }
}
