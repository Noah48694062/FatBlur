package com.example.fatblur.views;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fatblur.R;
import com.example.fatblur.models.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText edtEmail, edtPassword, edtConfirmPassword, edtName, edtPhone;
    private Button btnRegister;
    private TextView txtLogin; // Dòng chuyển về đăng nhập
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 1. Ánh xạ View chính xác theo ID trong XML mới
        edtName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmailReg);
        edtPassword = findViewById(R.id.edtPasswordReg);
        edtConfirmPassword = findViewById(R.id.edtConfirmPasswordReg); // Ô mới thêm
        btnRegister = findViewById(R.id.btnRegister);
        txtLogin = findViewById(R.id.txtGoToLogin); // Dòng mới thêm

        // 2. Xử lý sự kiện nhấn nút Đăng ký
        btnRegister.setOnClickListener(v -> performRegistration());

        // 3. Xử lý sự kiện nhấn vào "Đăng nhập ngay"
        txtLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Đóng màn hình đăng ký
        });
    }

    private void performRegistration() {
        String name = edtName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        // Kiểm tra tính hợp lệ của dữ liệu đầu vào
        if (validateInput(name, phone, email, password, confirmPassword)) {
            // Hiển thị thông báo đang xử lý (tùy chọn)
            Toast.makeText(this, "Đang tạo tài khoản...", Toast.LENGTH_SHORT).show();

            // Tạo tài khoản trên Firebase Authentication
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();
                            saveUserToDatabase(uid, email, name, phone);
                        } else {
                            Toast.makeText(RegisterActivity.this, "Lỗi: "
                                    + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void saveUserToDatabase(String uid, String email, String name, String phone) {
        long currentTime = System.currentTimeMillis();
        String userCode = generateUserCode();

        // Khởi tạo đối tượng User khớp với Collection: users
        User newUser = new User();
        newUser.userId = uid;
        newUser.email = email;
        newUser.name = name;
        newUser.phone = phone;
        newUser.userCode = userCode;
        newUser.partnerId = ""; // Mặc định chưa kết nối
        newUser.isDeleted = false; // Mặc định tài khoản hoạt động
        newUser.createdAt = currentTime;
        newUser.updatedAt = currentTime;

        // Lưu dữ liệu vào Realtime Database tại nút 'users'
        mDatabase.child("users").child(uid).setValue(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Mã của bạn: " + userCode, Toast.LENGTH_LONG).show();
                    // Đưa người dùng về màn hình chính hoặc màn hình nhập mã kết nối
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Lỗi lưu Database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Tạo mã kết nối ngẫu nhiên 6 ký tự (Ví dụ: AB12CD) [cite: 2]
    private String generateUserCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random rnd = new Random();
        while (code.length() < 6) {
            int index = (int) (rnd.nextFloat() * characters.length());
            code.append(characters.charAt(index));
        }
        return code.toString();
    }

    private boolean validateInput(String name, String phone, String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(name)) {
            edtName.setError("Vui lòng nhập họ tên"); return false;
        }
        if (TextUtils.isEmpty(phone)) {
            edtPhone.setError("Vui lòng nhập số điện thoại"); return false;
        }
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập email"); return false;
        }
        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu phải có ít nhất 6 ký tự"); return false;
        }
        // Kiểm tra xác nhận mật khẩu
        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không khớp"); return false;
        }
        return true;
    }
}