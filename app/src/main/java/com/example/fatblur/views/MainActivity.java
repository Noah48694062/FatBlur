package com.example.fatblur.views;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.fatblur.R;
import com.example.fatblur.models.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ValueEventListener mSessionListener;

    // Khai báo 2 mã định danh quyền riêng biệt
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private static final int LOCATION_PERMISSION_CODE = 100;

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            updateUserStatus(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuth.getCurrentUser() != null) {
            updateUserStatus(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 1. Khởi tạo các hệ thống nền
        createNotificationChannel();
        setupPresenceSystem();
        setupSessionListener();

        // 2. Yêu cầu các quyền cần thiết ngay khi vào app
        checkAndRequestNotificationPermission();
        checkAndRequestLocationPermission();

        // 3. Thiết lập Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_status) selectedFragment = new StatusFragment();
            else if (id == R.id.nav_calendar) selectedFragment = new CalendarFragment();
            else if (id == R.id.nav_chat) selectedFragment = new ChatFragment();
            else if (id == R.id.nav_profile) selectedFragment = new ProfileFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_status);
        }
    }

    // --- HÀM XIN QUYỀN VỊ TRÍ ---
    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        }
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Đã bật thông báo kỷ niệm.");
            }
        }
        // Xử lý khi người dùng bấm Allow cho Vị trí
        else if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Đã nhận quyền vị trí!");
                // Refresh lại fragment status để map nhận quyền ngay lập tức
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new StatusFragment())
                        .commit();
            } else {
                showToast("Bạn cần cấp quyền vị trí để ứng dụng hoạt động chính xác.");
            }
        }
    }

    private void setupSessionListener() {
        if (mAuth.getCurrentUser() == null) return;
        LoginActivity.isLoggingOut = false;
        String uid = mAuth.getUid();

        mSessionListener = mDatabase.child("user_status").child(uid).child("currentSessionId")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String serverSessionId = snapshot.getValue(String.class);
                        String localSessionId = SessionManager.getSessionId(MainActivity.this);

                        if (serverSessionId != null && !serverSessionId.equals(localSessionId)) {
                            showKickedOutDialog();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showKickedOutDialog() {
        if (LoginActivity.isLoggingOut || isFinishing()) return;

        new AlertDialog.Builder(this)
                .setTitle("Phiên đăng nhập hết hạn")
                .setMessage("Tài khoản đã đăng nhập từ thiết bị khác. Bạn sẽ được đăng xuất để bảo mật.")
                .setPositiveButton("Đồng ý", (dialog, which) -> performLogout())
                .setCancelable(false)
                .show();
    }

    private void performLogout() {
        if (mSessionListener != null && mAuth.getCurrentUser() != null) {
            String uid = mAuth.getUid();
            mDatabase.child("user_status").child(uid).child("currentSessionId").removeEventListener(mSessionListener);
        }
        mAuth.signOut();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSessionListener != null && mAuth.getCurrentUser() != null) {
            String uid = mAuth.getUid();
            mDatabase.child("user_status").child(uid).child("currentSessionId").removeEventListener(mSessionListener);
        }
    }

    private void setupPresenceSystem() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getUid();
        mDatabase.child("user_status").child(uid).child("isOnline").onDisconnect().setValue(false);
    }

    private void updateUserStatus(boolean isOnline) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getUid();
        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("isOnline", isOnline);
        statusUpdate.put("lastActiveAt", System.currentTimeMillis());
        statusUpdate.put("batteryLevel", getBatteryPercentage());
        mDatabase.child("user_status").child(uid).updateChildren(statusUpdate);
    }

    private int getBatteryPercentage() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            return (int) ((level / (float) scale) * 100);
        }
        return 100;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("special_day_channel", "Kỷ niệm SecretLove", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}