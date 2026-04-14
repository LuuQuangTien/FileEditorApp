package hcmute.edu.vn.documentfileeditor.Activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;

import hcmute.edu.vn.documentfileeditor.R;

public class TranslateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        Spinner spinnerSource = findViewById(R.id.spinner_source);
        Spinner spinnerTarget = findViewById(R.id.spinner_target);

        String[] languages = {"English", "Vietnamese", "Chinese", "Japanese", "Korean", "Spanish", "French", "German"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        if (spinnerSource != null) {
            spinnerSource.setAdapter(adapter);
            spinnerSource.setSelection(0);
        }
        if (spinnerTarget != null) {
            spinnerTarget.setAdapter(adapter);
            spinnerTarget.setSelection(1);
        }
    }
}
