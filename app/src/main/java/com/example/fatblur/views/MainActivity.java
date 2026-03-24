package com.example.fatblur.views;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.fatblur.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onStart() {
        super.onStart();
        // Kiểm tra nếu người dùng đã đăng nhập thì cập nhật trạng thái ngay
        if (mAuth.getCurrentUser() != null) {
            updateUserStatus(mAuth.getCurrentUser().getUid());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_status) {
                selectedFragment = new StatusFragment();
            } else if (id == R.id.nav_calendar) {
                // selectedFragment = new CalendarFragment();
            } else if (id == R.id.nav_chat) {
                // selectedFragment = new ChatFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        bottomNav.setSelectedItemId(R.id.nav_status);
    }

    private void updateUserStatus(String uid) {
        // Cập nhật các thông tin Realtime theo thiết kế CSDL
        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("isOnline", true); // Đánh dấu đang trực tuyến
        statusUpdate.put("lastActiveAt", System.currentTimeMillis()); // Lưu thời điểm hoạt động cuối
        // Tạm thời để mặc định, bạn có thể lấy dữ liệu thật sau
        statusUpdate.put("batteryLevel", 100);

        mDatabase.child("user_status").child(uid).updateChildren(statusUpdate)
                .addOnFailureListener(e -> {
                    // Log lỗi nếu cần thiết
                });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}