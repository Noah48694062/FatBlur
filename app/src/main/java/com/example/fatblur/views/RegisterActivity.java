package com.example.fatblur.views;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fatblur.R;
import com.example.fatblur.models.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText edtEmail, edtPassword, edtConfirmPassword, edtName, edtPhone, edtBirthday, edtBio;
    private RadioGroup rgGender;
    private Button btnRegister;
    private TextView txtLogin;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 1. Ánh xạ toàn bộ View từ XML mới
        edtName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        edtBirthday = findViewById(R.id.edtBirthdayReg);
        rgGender = findViewById(R.id.rgGender);
        edtEmail = findViewById(R.id.edtEmailReg);
        edtBio = findViewById(R.id.edtBioReg);
        edtPassword = findViewById(R.id.edtPasswordReg);
        edtConfirmPassword = findViewById(R.id.edtConfirmPasswordReg);
        btnRegister = findViewById(R.id.btnRegister);
        txtLogin = findViewById(R.id.txtGoToLogin);

        // 2. Xử lý chọn ngày sinh (DatePicker)
        edtBirthday.setOnClickListener(v -> showDatePicker());

        // 3. Xử lý đăng ký
        btnRegister.setOnClickListener(v -> performRegistration());

        // 4. Quay lại Đăng nhập
        txtLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            // Định dạng lại ngày để đảm bảo luôn có 2 chữ số (VD: 05/09/2005 thay vì 5/9/2005)
            String date = String.format("%02d/%02d/%d", dayOfMonth, (month1 + 1), year1);
            edtBirthday.setText(date);
        }, year, month, day);

        // CHỈNH SỬA TẠI ĐÂY: Chặn chọn ngày trong tương lai
        // System.currentTimeMillis() lấy thời gian hiện tại của hệ thống
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        datePickerDialog.show();
    }

    private void performRegistration() {
        String name = edtName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String birthday = edtBirthday.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String bio = edtBio.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        // Lấy giới tính
        int selectedId = rgGender.getCheckedRadioButtonId();
        String gender = "";
        if (selectedId != -1) {
            RadioButton rb = findViewById(selectedId);
            gender = rb.getText().toString();
        }

        if (validateInput(name, phone, birthday, gender, email, password, confirmPassword)) {
            String finalGender = gender;
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();
                            saveUserToDatabase(uid, email, name, phone, birthday, finalGender, bio);
                        } else {
                            Toast.makeText(RegisterActivity.this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();

                        }
                    });
        }
    }

    private void saveUserToDatabase(String uid, String email, String name, String phone, String birthday, String gender, String bio) {
        long currentTime = System.currentTimeMillis();

        User newUser = new User();
        newUser.userId = uid;
        newUser.email = email;
        newUser.name = name;
        newUser.phone = phone;
        newUser.birthday = birthday;
        newUser.gender = gender;
        newUser.bio = bio;
        newUser.userCode = ""; // CHỈNH SỬA: Không tạo mã lúc đăng ký
        newUser.partnerId = "";
        newUser.isDeleted = false;
        newUser.createdAt = currentTime;
        newUser.updatedAt = currentTime;

        mDatabase.child("users").child(uid).setValue(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Dòng này cực kỳ quan trọng để biết tại sao bị từ chối
                    Toast.makeText(RegisterActivity.this, "Lỗi Database: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInput(String name, String phone, String bday, String gen, String email, String pass, String confirm) {
        if (TextUtils.isEmpty(name)) { edtName.setError("Nhập họ tên"); return false; }
        if (TextUtils.isEmpty(phone)) { edtPhone.setError("Nhập SĐT"); return false; }
        if (TextUtils.isEmpty(bday)) { edtBirthday.setError("Chọn ngày sinh"); return false; }
        if (TextUtils.isEmpty(gen)) { Toast.makeText(this, "Vui lòng chọn giới tính", Toast.LENGTH_SHORT).show(); return false; }
        if (TextUtils.isEmpty(email)) { edtEmail.setError("Nhập email"); return false; }
        if (pass.length() < 6) { edtPassword.setError("Mật khẩu ít nhất 6 ký tự"); return false; }
        if (!pass.equals(confirm)) { edtConfirmPassword.setError("Mật khẩu không khớp"); return false; }
        return true;
    }
}