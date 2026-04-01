package com.example.fatblur.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fatblur.databinding.ActivityNotificationSettingsBinding;

public class NotificationSettingsActivity extends AppCompatActivity {

    private ActivityNotificationSettingsBinding binding;
    private SharedPreferences sharedPreferences;

    // Tên file lưu trữ và các key
    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_MSG_NOTIFY = "message_notification";
    private static final String KEY_SPECIAL_NOTIFY = "special_day_notification";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 1. Tải trạng thái đã lưu trước đó (mặc định là true - bật)
        loadSettings();

        // 2. Sự kiện nút quay lại
        binding.btnBack.setOnClickListener(v -> finish());

        // 3. Lắng nghe thay đổi của Switch Tin nhắn
        binding.switchMessageNotify.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting(KEY_MSG_NOTIFY, isChecked);
        });

        // 4. Lắng nghe thay đổi của Switch Ngày đặc biệt
        binding.switchSpecialDayNotify.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting(KEY_SPECIAL_NOTIFY, isChecked);
        });
    }

    private void loadSettings() {
        boolean isMsgEnabled = sharedPreferences.getBoolean(KEY_MSG_NOTIFY, true);
        boolean isSpecialEnabled = sharedPreferences.getBoolean(KEY_SPECIAL_NOTIFY, true);

        binding.switchMessageNotify.setChecked(isMsgEnabled);
        binding.switchSpecialDayNotify.setChecked(isSpecialEnabled);
    }

    private void saveSetting(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply(); // Lưu trong background
    }
}