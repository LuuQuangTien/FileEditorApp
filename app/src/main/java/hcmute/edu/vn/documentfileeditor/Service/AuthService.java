package hcmute.edu.vn.documentfileeditor.Service;

import android.util.Patterns;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Service layer encapsulating all authentication-related business logic.
 * Decouples UI (Activity/Fragment) from direct Firebase Auth dependency,
 * following the Dependency Inversion Principle.
 */
public class AuthService {

    /**
     * Callback for authentication operations.
     */
    public interface AuthCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    private final FirebaseAuth auth;

    public AuthService() {
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Returns the currently signed-in user, or null if not signed in.
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * Returns whether there is a currently signed-in user.
     */
    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    /**
     * Gets the current user's UID, or null if not signed in.
     */
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Signs in with email and password.
     */
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

    /**
     * Creates a new account with email, password, and display name.
     */
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

    /**
     * Sends a password reset email.
     */
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

    /**
     * Signs out the current user.
     */
    public void signOut() {
        auth.signOut();
    }

    /**
     * Validates an email address format.
     */
    public boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Validates that a password meets minimum length requirements.
     */
    public boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}
