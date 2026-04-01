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
            updateMessageUI(isChecked);
        });

        // 4. Lắng nghe thay đổi của Switch Ngày đặc biệt
        binding.switchSpecialDayNotify.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting(KEY_SPECIAL_NOTIFY, isChecked);
            updateSpecialDayUI(isChecked);
        });
    }

    private void loadSettings() {
        boolean isMsgEnabled = sharedPreferences.getBoolean(KEY_MSG_NOTIFY, true);
        boolean isSpecialEnabled = sharedPreferences.getBoolean(KEY_SPECIAL_NOTIFY, true);

        binding.switchMessageNotify.setChecked(isMsgEnabled);
        binding.switchSpecialDayNotify.setChecked(isSpecialEnabled);
        // Gọi hàm cập nhật UI ngay khi load để hiển thị đúng trạng thái ban đầu
        updateMessageUI(isMsgEnabled);
        updateSpecialDayUI(isSpecialEnabled);
    }

    private void saveSetting(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply(); // Lưu trong background
    }
    // Hàm làm xám màu phần tin nhắn
    private void updateMessageUI(boolean isEnabled) {
        float alphaValue = isEnabled ? 1.0f : 0.4f; // 1.0 là rõ nét, 0.4 là mờ (xám)

        // Làm mờ text tiêu đề hoặc các thành phần liên quan trong layout
        binding.switchMessageNotify.setAlpha(alphaValue);
        // Nếu bạn có mô tả chi tiết bên dưới cũng nên làm mờ
        // binding.txtMessageDescription.setAlpha(alphaValue);
    }

    private void updateSpecialDayUI(boolean isEnabled) {
        float alphaValue = isEnabled ? 1.0f : 0.4f;
        binding.switchSpecialDayNotify.setAlpha(alphaValue);
    }
}