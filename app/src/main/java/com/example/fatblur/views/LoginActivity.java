package com.example.fatblur.views;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fatblur.R;
import com.example.fatblur.models.SessionManager;
import com.example.fatblur.models.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView txtRegister, txtForgotPassword; // Thêm TextView quên mật khẩu
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            // Khi mở app lại, kiểm tra xem máy này có còn giữ "quyền" đăng nhập không
            checkDeviceOnlineStatus(mAuth.getCurrentUser().getUid());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        edtEmail = findViewById(R.id.edtEmailLogin);
        edtPassword = findViewById(R.id.edtPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtGoToRegister);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);

        txtForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (validateInput(email, password)) {
                performLogin(email, password);
            }
        });

        txtRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Khôi phục mật khẩu");
        builder.setMessage("Nhập email bạn đã đăng ký để nhận liên kết đặt lại mật khẩu:");

        // Tạo EditText và bọc trong FrameLayout để có khoảng cách (Padding)
        final EditText input = new EditText(this);
        input.setHint("Email của bạn");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50; // Padding bên trái
        params.rightMargin = 50; // Padding bên phải
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                sendResetEmail(email);
            } else {
                Toast.makeText(this, "Vui lòng nhập email hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void sendResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Liên kết đặt lại mật khẩu đã được gửi đến email của bạn!",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Lỗi: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void performLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        checkDeviceOnlineStatus(uid);
                    } else {
                        Toast.makeText(LoginActivity.this, "Lỗi: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkPartnerStatus(String uid) {
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if (TextUtils.isEmpty(user.partnerId)) {
                        Toast.makeText(LoginActivity.this,
                                "Bạn chưa kết nối. Hãy vào Hồ sơ để lấy mã nhé!",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Chào mừng " + user.name + " trở lại!",
                                Toast.LENGTH_SHORT).show();
                    }
                    goToMainActivity();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

//    private void updateUserStatus(String uid) {
//        mDatabase.child("user_status").child(uid).child("isOnline").setValue(true);
//        mDatabase.child("user_status").child(uid).child("lastActiveAt").setValue(System.currentTimeMillis());
//    }

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

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void checkDeviceOnlineStatus(String uid) {
        mDatabase.child("user_status").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOnline = snapshot.child("isOnline").getValue(Boolean.class);
                String serverSessionId = snapshot.child("currentSessionId").getValue(String.class);
                String localSessionId = SessionManager.getSessionId(LoginActivity.this);

                // --- LOGIC THÔNG MINH TẠI ĐÂY ---
                if (serverSessionId != null && serverSessionId.equals(localSessionId)) {
                    // Nếu ID trên server trùng với ID máy này -> Chính là mình, cho vào luôn
                    startNewSession(uid);
                } else if (isOnline != null && isOnline) {
                    // Nếu ID khác VÀ đang online -> Có máy khác đang dùng thật
                    showKickDialog(uid);
                } else {
                    // Các trường hợp còn lại (offline hoặc chưa có session) -> Vào luôn
                    startNewSession(uid);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showKickDialog(String uid) {
        if (isFinishing()) return; // Tránh crash nếu activity đang đóng
        new AlertDialog.Builder(this)
                .setTitle("Cảnh báo đăng nhập")
                .setMessage("Tài khoản đang được đăng nhập ở thiết bị khác. Bạn có muốn đăng xuất thiết bị đó không?")
                .setPositiveButton("Đồng ý", (dialog, which) -> startNewSession(uid))
                .setNegativeButton("Hủy", (dialog, which) -> mAuth.signOut())
                .setCancelable(false)
                .show();
    }

    private void startNewSession(String uid) {
        // Nếu là lần đầu đăng nhập hoặc bị ghi đè, tạo mã mới
        // Nếu là chính mình quay lại, có thể giữ mã cũ nhưng cập nhật Online là đủ
        String currentLocalId = SessionManager.getSessionId(this);
        String finalSessionId = currentLocalId.isEmpty() ? UUID.randomUUID().toString() : currentLocalId;

        SessionManager.saveSessionId(this, finalSessionId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentSessionId", finalSessionId);
        updates.put("isOnline", true);
        updates.put("lastActiveAt", System.currentTimeMillis());

        mDatabase.child("user_status").child(uid).updateChildren(updates)
                .addOnSuccessListener(aVoid -> checkPartnerStatus(uid));
    }

}