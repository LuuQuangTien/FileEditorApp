package hcmute.edu.vn.documentfileeditor.Activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import hcmute.edu.vn.documentfileeditor.R;

public class CrashReportActivity extends AppCompatActivity {
    public static final String EXTRA_ERROR_MESSAGE = "extra_error_message";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_report);

        TextView tvError = findViewById(R.id.tv_crash_error);
        String errorMessage = getIntent().getStringExtra(EXTRA_ERROR_MESSAGE);
        tvError.setText(errorMessage != null ? errorMessage : "Unknown crash");

        findViewById(R.id.btn_crash_close).setOnClickListener(v -> finishAffinity());
    }
}
