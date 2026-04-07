package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

import hcmute.edu.vn.documentfileeditor.R;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private MaterialButton btnRegister;

    private CheckBox cbTerms;
    private LinearLayout llPasswordStrength;
    private View viewStrength1, viewStrength2, viewStrength3;
    private TextView tvPasswordStrength;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        TextView tvSignIn = findViewById(R.id.tv_sign_in);

        cbTerms = findViewById(R.id.cb_terms);
        llPasswordStrength = findViewById(R.id.ll_password_strength);
        viewStrength1 = findViewById(R.id.view_strength_1);
        viewStrength2 = findViewById(R.id.view_strength_2);
        viewStrength3 = findViewById(R.id.view_strength_3);
        tvPasswordStrength = findViewById(R.id.tv_password_strength);

        btnRegister.setOnClickListener(v -> register());
        tvSignIn.setOnClickListener(v -> finish());

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            llPasswordStrength.setVisibility(View.GONE);
            return;
        }

        llPasswordStrength.setVisibility(View.VISIBLE);
        int neutralColor = ContextCompat.getColor(this, R.color.neutral_200);
        int redColor = ContextCompat.getColor(this, R.color.red_600);
        int yellowColor = ContextCompat.getColor(this, R.color.amber_500);
        int greenColor = ContextCompat.getColor(this, R.color.green_500);

        if (password.length() < 6) {
            viewStrength1.setBackgroundColor(redColor);
            viewStrength2.setBackgroundColor(neutralColor);
            viewStrength3.setBackgroundColor(neutralColor);
            tvPasswordStrength.setText("Yếu");
            tvPasswordStrength.setTextColor(redColor);
        } else if (password.length() < 10) {
            viewStrength1.setBackgroundColor(yellowColor);
            viewStrength2.setBackgroundColor(yellowColor);
            viewStrength3.setBackgroundColor(neutralColor);
            tvPasswordStrength.setText("Trung bình");
            tvPasswordStrength.setTextColor(yellowColor);
        } else {
            viewStrength1.setBackgroundColor(greenColor);
            viewStrength2.setBackgroundColor(greenColor);
            viewStrength3.setBackgroundColor(greenColor);
            tvPasswordStrength.setText("Mạnh");
            tvPasswordStrength.setTextColor(greenColor);
        }
    }

    private void register() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (!isValidInput(name, email, password, confirmPassword)) {
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Vui lòng đồng ý với điều khoản sử dụng", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (auth.getCurrentUser() != null) {
                        UserProfileChangeRequest profileUpdates =
                                new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();

                        auth.getCurrentUser().updateProfile(profileUpdates)
                                .addOnCompleteListener(updateTask -> {
                                    setLoading(false);
                                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();
                                    openMainAndClearBackStack();
                                });
                    } else {
                        setLoading(false);
                        openMainAndClearBackStack();
                    }
                });
    }

    private boolean isValidInput(String name, String email, String password, String confirmPassword) {
        if (name.isEmpty()) {
            etName.setError("Enter your name");
            etName.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void setLoading(boolean isLoading) {
        btnRegister.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? "Creating account..." : "Đăng ký");
    }

    private void openMainAndClearBackStack() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
