package com.example.fatblur;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Ánh xạ nút đăng xuất
        btnLogout = findViewById(R.id.btnLogout);

        // 2. Thiết lập sự kiện click
        btnLogout.setOnClickListener(v -> {
            // Lệnh đăng xuất của Firebase
            FirebaseAuth.getInstance().signOut();

            // Chuyển hướng về LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            // Xóa hết các Activity trước đó để người dùng không bấm Back quay lại được
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Đóng MainActivity
            finish();
        });
    }
}