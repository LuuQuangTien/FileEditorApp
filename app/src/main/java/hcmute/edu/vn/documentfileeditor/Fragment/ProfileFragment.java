package hcmute.edu.vn.documentfileeditor.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import hcmute.edu.vn.documentfileeditor.R;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        setupSettingItem(view, R.id.setting_edit_profile, R.drawable.ic_user, "Edit Profile");
        setupSettingItem(view, R.id.setting_email, R.drawable.ic_mail, "Email Settings");
        
        setupSettingToggle(view, R.id.setting_notifications, R.drawable.ic_bell, "Notifications", true);
        setupSettingToggle(view, R.id.setting_dark_mode, R.drawable.ic_sun, "Dark Mode", false);
        setupSettingItem(view, R.id.setting_language, R.drawable.ic_globe, "Language");
        
        setupSettingItem(view, R.id.setting_help, R.drawable.ic_help, "Help & Support");
        setupSettingItem(view, R.id.setting_privacy, R.drawable.ic_shield, "Privacy Policy");
        setupSettingItem(view, R.id.setting_terms, R.drawable.ic_file_text, "Terms of Service");
        
        return view;
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
