package com.example.fatblur.views;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fatblur.databinding.ActivityConnectCodeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Random;

public class ConnectCodeActivity extends AppCompatActivity {

    private ActivityConnectCodeBinding binding;
    private DatabaseReference mDatabase;
    private String myUid;
    private static final long EXPIRE_TIME = 10 * 60 * 1000; // 10 phút

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConnectCodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mDatabase = FirebaseDatabase.getInstance().getReference();
        myUid = FirebaseAuth.getInstance().getUid();

        binding.btnBack.setOnClickListener(v -> finish());

        // 1. Lấy hoặc tạo mã của mình ngay khi vào màn hình
        if (myUid != null) {
            getMyConnectCode();
        }

        // 2. Xử lý khi nhấn nút "Ghép ngay"
        binding.btnConnect.setOnClickListener(v -> handlePairing());

        // 3. Nút sao chép mã (Optional)
        binding.btnCopyCode.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("ConnectCode", binding.txtMyCode.getText());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Đã sao chép mã", Toast.LENGTH_SHORT).show();
        });
    }

    private void getMyConnectCode() {
        mDatabase.child("users").child(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentCode = snapshot.child("connectCode").getValue(String.class);
                Long createdAt = snapshot.child("codeCreatedAt").getValue(Long.class);
                long now = System.currentTimeMillis();

                // Nếu mã chưa có hoặc đã hết hạn 24h
                if (currentCode == null || createdAt == null || (now - createdAt) > EXPIRE_TIME) {
                    String newCode = generateRandom6DigitCode();
                    mDatabase.child("users").child(myUid).child("connectCode").setValue(newCode);
                    mDatabase.child("users").child(myUid).child("codeCreatedAt").setValue(now);
                    binding.txtMyCode.setText(newCode);
                } else {
                    binding.txtMyCode.setText(currentCode);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void handlePairing() {
        // Giả sử bạn dùng PinView hoặc EditText có ID là edtPartnerCode
        String inputCode = binding.pinViewPartnerCode.getText().toString().trim().toUpperCase();

        if (inputCode.length() < 6) {
            Toast.makeText(this, "Vui lòng nhập đủ 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tìm người dùng nào đang sở hữu mã này
        mDatabase.child("users").orderByChild("connectCode").equalTo(inputCode)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnap : snapshot.getChildren()) {
                                String partnerUid = userSnap.getKey();
                                Long partnerCodeCreatedAt = userSnap.child("codeCreatedAt").getValue(Long.class);

                                // Kiểm tra xem có tự kết nối chính mình không
                                if (partnerUid != null && partnerUid.equals(myUid)) {
                                    Toast.makeText(ConnectCodeActivity.this, "Bạn không thể kết nối với chính mình!", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Kiểm tra mã đối phương còn hạn không
                                if (partnerCodeCreatedAt != null && (System.currentTimeMillis() - partnerCodeCreatedAt) < EXPIRE_TIME) {
                                    performConnect(partnerUid);
                                } else {
                                    Toast.makeText(ConnectCodeActivity.this, "Mã này đã hết hạn!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(ConnectCodeActivity.this, "Mã không chính xác!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void performConnect(String partnerUid) {
        // Cập nhật partnerId cho cả 2 người (Kết nối 2 chiều)
        mDatabase.child("users").child(myUid).child("partnerId").setValue(partnerUid);
        mDatabase.child("users").child(partnerUid).child("partnerId").setValue(myUid)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Xóa mã mời của cả 2 để không dùng lại được nữa
                        mDatabase.child("users").child(myUid).child("connectCode").removeValue();
                        mDatabase.child("users").child(partnerUid).child("connectCode").removeValue();

                        Toast.makeText(ConnectCodeActivity.this, "Kết nối thành công!", Toast.LENGTH_SHORT).show();
                        finish(); // Đóng Activity này để quay về màn hình quản lý
                    }
                });
    }

    private String generateRandom6DigitCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}