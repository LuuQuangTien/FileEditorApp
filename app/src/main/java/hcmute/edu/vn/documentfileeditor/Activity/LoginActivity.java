package hcmute.edu.vn.documentfileeditor.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;

import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.AuthService;

public class LoginActivity extends AppCompatActivity {

    private AuthService authService;
    private EditText etEmail;
    private EditText etPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnGoogleLogin;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authService = new AuthService();
        if (authService.isSignedIn()) {
            openMainAndClearBackStack();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogleLogin = findViewById(R.id.btn_google_login);
        TextView tvCreateAccount = findViewById(R.id.tv_create_account);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);

        setupGoogleSignIn();

        btnLogin.setOnClickListener(v -> login());
        btnGoogleLogin.setOnClickListener(v -> loginWithGoogle());
        tvCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v -> sendPasswordReset());
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
                        Toast.makeText(this, "Google sign-in cancelled", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(LoginActivity.this, "Google login successful", Toast.LENGTH_SHORT).show();
                                openMainAndClearBackStack();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                setGoogleLoading(false);
                                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (ApiException e) {
                        setGoogleLoading(false);
                        Toast.makeText(this, "Google sign-in failed: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!isValidInput(email, password)) {
            return;
        }

        setLoading(true);
        authService.login(email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                openMainAndClearBackStack();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendPasswordReset() {
        String email = etEmail.getText().toString().trim();
        if (!authService.isValidEmail(email)) {
            etEmail.setError("Enter a valid email first");
            etEmail.requestFocus();
            return;
        }

        authService.sendPasswordReset(email, new AuthService.AuthCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(LoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loginWithGoogle() {
        setGoogleLoading(true);
        googleSignInClient.signOut().addOnCompleteListener(task ->
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent()));
    }

    private boolean isValidInput(String email, String password) {
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

        return true;
    }

    private void setLoading(boolean isLoading) {
        btnLogin.setEnabled(!isLoading);
        btnLogin.setText(isLoading ? "Signing in..." : "Sign In");
    }

    private void setGoogleLoading(boolean isLoading) {
        btnGoogleLogin.setEnabled(!isLoading);
        btnGoogleLogin.setText(isLoading ? "Connecting..." : "Google");
    }

    private void openMainAndClearBackStack() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
