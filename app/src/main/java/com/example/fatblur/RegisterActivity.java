package com.example.fatblur;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;

public class RegisterActivity extends AppCompatActivity {

//    private EditText edtName, edtPhone, edtEmail, edtPassword;
    private TextInputEditText edtEmail, edtPassword, edtName, edtPhone;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Ánh xạ View
        edtName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmailReg);
        edtPassword = findViewById(R.id.edtPasswordReg);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> performRegistration());
    }

    private void performRegistration() {
        String name = edtName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (validateInput(name, phone, email, password)) {
            // 1. Tạo tài khoản trên Firebase Authentication
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();
                            saveUserToDatabase(uid, email, name, phone);
                        } else {
                            Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: "
                                    + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void saveUserToDatabase(String uid, String email, String name, String phone) {
        long currentTime = System.currentTimeMillis(); // Lưu thời điểm theo ms
        String userCode = generateUserCode(); // Tạo mã chia sẻ để kết nối

        // 2. Khởi tạo đối tượng User theo Schema
        User newUser = new User();
        newUser.userId = uid;
        newUser.email = email;
        newUser.name = name;
        newUser.phone = phone;
        newUser.userCode = userCode;
        newUser.createdAt = currentTime;
        newUser.updatedAt = currentTime;
        newUser.isDeleted = false;
        newUser.partnerId = ""; // Mặc định chưa có đối tác

        // 3. Lưu vào Collection 'users'
        mDatabase.child("users").child(uid).setValue(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Mã của bạn: " + userCode, Toast.LENGTH_LONG).show();
                    finish(); // Trở về LoginActivity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Lỗi lưu Database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Hàm tạo mã userCode 6 ký tự ngẫu nhiên
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

    private boolean validateInput(String name, String phone, String email, String password) {
        if (TextUtils.isEmpty(name)) { edtName.setError("Nhập tên"); return false; }
        if (TextUtils.isEmpty(phone)) { edtPhone.setError("Nhập số điện thoại"); return false; }
        if (TextUtils.isEmpty(email)) { edtEmail.setError("Nhập email"); return false; }
        if (password.length() < 6) { edtPassword.setError("Mật khẩu ít nhất 6 ký tự"); return false; }
        return true;
    }
}