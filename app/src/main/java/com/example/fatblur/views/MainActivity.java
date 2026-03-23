package com.example.fatblur.views;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.fatblur.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Lắng nghe sự kiện bấm nút trên thanh điều hướng
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_status) {
                // Chức năng: Theo dõi trạng thái (vị trí, pin...)
                //showToast("Màn hình: Theo dõi trạng thái");
                selectedFragment = new StatusFragment();
            } else if (id == R.id.nav_calendar) {
                // Chức năng: Quản lý ngày đặc biệt
                //showToast("Màn hình: Quản lý ngày đặc biệt");
                //selectedFragment = new CalendarFragment();
            } else if (id == R.id.nav_chat) {
                // Chức năng: Giao tiếp và tương tác
                //showToast("Màn hình: Nhắn tin");
                //selectedFragment = new ChatFragment();
            } else if (id == R.id.nav_profile) {
                // Chức năng: Quản lý tài khoản (hồ sơ)
                //showToast("Màn hình: Hồ sơ cá nhân");
                selectedFragment = new ProfileFragment();
            }

            // Thực hiện thay thế Fragment vào FrameLayout
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Mặc định chọn nút đầu tiên khi mở app
        bottomNav.setSelectedItemId(R.id.nav_status);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}