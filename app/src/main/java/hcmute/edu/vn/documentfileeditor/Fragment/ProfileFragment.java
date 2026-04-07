package hcmute.edu.vn.documentfileeditor.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import hcmute.edu.vn.documentfileeditor.Activity.LoginActivity;
import hcmute.edu.vn.documentfileeditor.R;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        bindUserProfile(view);
        
        setupSettingItem(view, R.id.setting_edit_profile, R.drawable.ic_user, "Edit Profile");
        setupSettingItem(view, R.id.setting_email, R.drawable.ic_mail, "Email Settings");
        
        setupSettingToggle(view, R.id.setting_notifications, R.drawable.ic_bell, "Notifications", true);
        setupSettingToggle(view, R.id.setting_dark_mode, R.drawable.ic_sun, "Dark Mode", false);
        setupSettingItem(view, R.id.setting_language, R.drawable.ic_globe, "Language");
        
        setupSettingItem(view, R.id.setting_help, R.drawable.ic_help, "Help & Support");
        setupSettingItem(view, R.id.setting_privacy, R.drawable.ic_shield, "Privacy Policy");
        setupSettingItem(view, R.id.setting_terms, R.drawable.ic_file_text, "Terms of Service");
        setupLogout(view);
        
        return view;
    }

    private void bindUserProfile(View view) {
        TextView tvName = view.findViewById(R.id.tv_profile_name);
        TextView tvEmail = view.findViewById(R.id.tv_profile_email);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (tvName != null) tvName.setText("Guest");
            if (tvEmail != null) tvEmail.setText("Not signed in");
            return;
        }

        String email = user.getEmail();
        String displayName = user.getDisplayName();
        if ((displayName == null || displayName.trim().isEmpty()) && email != null) {
            int atIndex = email.indexOf("@");
            displayName = atIndex > 0 ? email.substring(0, atIndex) : email;
        }

        if (tvName != null) tvName.setText(displayName != null ? displayName : "User");
        if (tvEmail != null) tvEmail.setText(email != null ? email : "No email");
    }

    private void setupLogout(View view) {
        View logoutButton = view.findViewById(R.id.btn_logout);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();

                android.content.Intent intent = new android.content.Intent(requireActivity(), LoginActivity.class);
                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        }
    }

    private void setupSettingItem(View parentView, int includeId, int iconRes, String label) {
        View settingView = parentView.findViewById(includeId);
        if (settingView != null) {
            ImageView icon = settingView.findViewById(R.id.icon_setting);
            TextView tvLabel = settingView.findViewById(R.id.tv_setting_label);
            if (icon != null) icon.setImageResource(iconRes);
            if (tvLabel != null) tvLabel.setText(label);
        }
    }

    private void setupSettingToggle(View parentView, int includeId, int iconRes, String label, boolean isChecked) {
        View settingView = parentView.findViewById(includeId);
        if (settingView != null) {
            ImageView icon = settingView.findViewById(R.id.icon_setting);
            TextView tvLabel = settingView.findViewById(R.id.tv_setting_label);
            com.google.android.material.materialswitch.MaterialSwitch toggle = settingView.findViewById(R.id.switch_setting);
            if (icon != null) icon.setImageResource(iconRes);
            if (tvLabel != null) tvLabel.setText(label);
            if (toggle != null) toggle.setChecked(isChecked);
        }
    }
}
