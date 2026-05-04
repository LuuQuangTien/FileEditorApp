package hcmute.edu.vn.documentfileeditor.Service;

import android.util.Patterns;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class AuthService {
    public interface AuthCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    private final FirebaseAuth auth;

    public AuthService() {
        this.auth = FirebaseAuth.getInstance();
    }
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    public void login(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";
                        callback.onFailure(message);
                    }
                });
    }

    public void loginWithGoogle(String idToken, AuthCallback callback) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Google sign-in failed";
                        callback.onFailure(message);
                    }
                });
    }

    public void register(String name, String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";
                        callback.onFailure(message);
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        UserProfileChangeRequest profileUpdates =
                                new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();
                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(updateTask -> callback.onSuccess());
                    } else {
                        callback.onSuccess();
                    }
                });
    }

    public void sendPasswordReset(String email, AuthCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Could not send reset email";
                        callback.onFailure(message);
                    }
                });
    }

    public void signOut() {
        auth.signOut();
    }

    public boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}
