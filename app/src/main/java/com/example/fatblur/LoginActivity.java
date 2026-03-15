package com.example.fatblur;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView txtRegister;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onStart() {
        super.onStart();
        // Kiểm tra phiên đăng nhập hiện tại để vào thẳng ứng dụng
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            goToMainActivity();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

        // Khởi tạo Firebase Auth và Database Reference
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Ánh xạ các thành phần giao diện
        edtEmail = findViewById(R.id.edtEmailLogin);
        edtPassword = findViewById(R.id.edtPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtGoToRegister);

        // Xử lý sự kiện nhấn nút Đăng nhập
        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (validateInput(email, password)) {
                performLogin(email, password);
            }
        });

        // Chuyển hướng sang màn hình Đăng ký
        txtRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập Email");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            return false;
        }
        return true;
    }

    private void performLogin(String email, String password) {
        // Thực hiện đăng nhập bằng Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        // Cập nhật trạng thái online ngay khi đăng nhập
                        updateUserStatus(uid);
                        // Kiểm tra thông tin người dùng và đối tác
                        checkPartnerStatus(uid);
                    } else {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkPartnerStatus(String uid) {
        // Truy xuất thông tin từ Collection 'users' [cite: 3]
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    // Nếu chưa có partnerId, hiển thị mã của bản thân để kết nối
                    if (TextUtils.isEmpty(user.partnerId)) {
                        Toast.makeText(LoginActivity.this, "Mã kết nối của bạn: " + user.userCode,
                                Toast.LENGTH_LONG).show();
                    }
                    goToMainActivity();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Lỗi truy xuất dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserStatus(String uid) {
        // Cập nhật trạng thái thời gian thực trong 'user_status'
        mDatabase.child("user_status").child(uid).child("isOnline").setValue(true);
        mDatabase.child("user_status").child(uid).child("lastActiveAt").setValue(System.currentTimeMillis());
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}