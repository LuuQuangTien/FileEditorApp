package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;

import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.AuthService;

public class RegisterActivity extends AppCompatActivity {

    private AuthService authService;
    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private MaterialButton btnRegister;
    private MaterialButton btnGoogleRegister;

    private CheckBox cbTerms;
    private LinearLayout llPasswordStrength;
    private View viewStrength1;
    private View viewStrength2;
    private View viewStrength3;
    private TextView tvPasswordStrength;

    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authService = new AuthService();

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        btnGoogleRegister = findViewById(R.id.btn_google_register);
        TextView tvSignIn = findViewById(R.id.tv_sign_in);

        cbTerms = findViewById(R.id.cb_terms);
        llPasswordStrength = findViewById(R.id.ll_password_strength);
        viewStrength1 = findViewById(R.id.view_strength_1);
        viewStrength2 = findViewById(R.id.view_strength_2);
        viewStrength3 = findViewById(R.id.view_strength_3);
        tvPasswordStrength = findViewById(R.id.tv_password_strength);

        setupGoogleSignIn();

        btnRegister.setOnClickListener(v -> register());
        btnGoogleRegister.setOnClickListener(v -> registerWithGoogle());
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

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() == null) {
                        setGoogleLoading(false);
                        Toast.makeText(this, "Google sign-up cancelled", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                                .getResult(ApiException.class);
                        String idToken = account != null ? account.getIdToken() : null;
                        if (idToken == null || idToken.isEmpty()) {
                            setGoogleLoading(false);
                            Toast.makeText(this, "Missing Google ID token", Toast.LENGTH_LONG).show();
                            return;
                        }

                        authService.loginWithGoogle(idToken, new AuthService.AuthCallback() {
                            @Override
                            public void onSuccess() {
                                setGoogleLoading(false);
                                Toast.makeText(RegisterActivity.this, "Google account connected", Toast.LENGTH_SHORT).show();
                                openMainAndClearBackStack();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                setGoogleLoading(false);
                                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (ApiException e) {
                        setGoogleLoading(false);
                        Toast.makeText(this, "Google sign-up failed: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
                    }
                }
        );
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
            tvPasswordStrength.setText("Weak");
            tvPasswordStrength.setTextColor(redColor);
        } else if (password.length() < 10) {
            viewStrength1.setBackgroundColor(yellowColor);
            viewStrength2.setBackgroundColor(yellowColor);
            viewStrength3.setBackgroundColor(neutralColor);
            tvPasswordStrength.setText("Medium");
            tvPasswordStrength.setTextColor(yellowColor);
        } else {
            viewStrength1.setBackgroundColor(greenColor);
            viewStrength2.setBackgroundColor(greenColor);
            viewStrength3.setBackgroundColor(greenColor);
            tvPasswordStrength.setText("Strong");
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
            Toast.makeText(this, "Please agree to the terms of use", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        authService.register(name, email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Account created successfully", Toast.LENGTH_SHORT).show();
                openMainAndClearBackStack();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void registerWithGoogle() {
        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please agree to the terms of use", Toast.LENGTH_SHORT).show();
            return;
        }

        setGoogleLoading(true);
        googleSignInClient.signOut().addOnCompleteListener(task ->
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent()));
    }

    private boolean isValidInput(String name, String email, String password, String confirmPassword) {
        if (name.isEmpty()) {
            etName.setError("Enter your name");
            etName.requestFocus();
            return false;
        }

        if (!authService.isValidEmail(email)) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return false;
        }

        if (!authService.isValidPassword(password)) {
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
        btnRegister.setText(isLoading ? "Creating account..." : "Dang ky");
    }

    private void setGoogleLoading(boolean isLoading) {
        btnGoogleRegister.setEnabled(!isLoading);
        btnGoogleRegister.setText(isLoading ? "Connecting..." : "Google");
    }

    private void openMainAndClearBackStack() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
