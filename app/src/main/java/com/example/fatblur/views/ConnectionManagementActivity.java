package com.example.fatblur.views;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fatblur.R;
import com.example.fatblur.databinding.ActivityConnectionManagementBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ConnectionManagementActivity extends AppCompatActivity {

    private ActivityConnectionManagementBinding binding;
    private DatabaseReference mDatabase;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Khởi tạo ViewBinding
        binding = ActivityConnectionManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. Khởi tạo Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        myUid = FirebaseAuth.getInstance().getUid();

        // 3. Sự kiện nút quay lại
        binding.btnBack.setOnClickListener(v -> finish());

        // 4. Sự kiện nút "Kết nối đối phương" (Nút này nằm trong layoutNoConnection)
        // Khi bấm sẽ chuyển sang trang nhập mã 6 số
        binding.layoutNoConnection.btnConnectPartner.setOnClickListener(v -> {
            Intent intent = new Intent(ConnectionManagementActivity.this, ConnectCodeActivity.class);
            startActivity(intent);
        });

        // 5. Bắt đầu theo dõi trạng thái kết nối
        if (myUid != null) {
            observeConnectionStatus();
        }
    }

    /**
     * Theo dõi thay đổi trên Firebase Realtime Database
     */
    private void observeConnectionStatus() {
        mDatabase.child("users").child(myUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // 1. Lấy và hiển thị tên của chính mình
                    String myName = snapshot.child("name").getValue(String.class);
                    if (myName != null && !myName.isEmpty()) {
                        binding.txtMyNameHeader.setText(myName);
                    } else {
                        binding.txtMyNameHeader.setText("Tôi"); // Backup nếu tên trống
                    }
                    // Cập nhật Avatar của bản thân lên header
                    String myAvatar = snapshot.child("avatar").getValue(String.class);
                    setBase64Image(myAvatar, binding.ivMyAvatar);

                    // Kiểm tra xem đã có PartnerId chưa
                    String partnerId = snapshot.child("partnerId").getValue(String.class);

                    if (partnerId != null && !partnerId.isEmpty()) {
                        // TRẠNG THÁI: ĐÃ KẾT NỐI
                        showConnectedUI(partnerId);
                    } else {
                        // TRẠNG THÁI: CHƯA KẾT NỐI
                        showNoConnectionUI();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Xử lý lỗi nếu cần
            }
        });
    }

    /**
     * Hiển thị giao diện khi chưa có kết nối
     */
    private void showNoConnectionUI() {
        binding.layoutConnected.getRoot().setVisibility(View.GONE);
        binding.layoutNoConnection.getRoot().setVisibility(View.VISIBLE);

        // Reset các thành phần trên Header về trạng thái chờ
        binding.ivHeart.setAlpha(0.3f); // Tim mờ đi
        binding.txtPartnerNameHeader.setText("Đang chờ...");
        binding.ivPartnerAvatar.setImageResource(R.drawable.default_avatar);
    }

    /**
     * Hiển thị giao diện thông tin khi đã kết nối thành công
     */
    private void showConnectedUI(String partnerId) {
        binding.layoutConnected.getRoot().setVisibility(View.VISIBLE);
        binding.layoutNoConnection.getRoot().setVisibility(View.GONE);
        binding.ivHeart.setAlpha(1.0f); // Tim sáng rực lên

        binding.layoutConnected.btnDisconnect.setOnClickListener(v -> {
            confirmDisconnect(partnerId);
        });
        // Truy vấn thông tin chi tiết của đối phương
        mDatabase.child("users").child(partnerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String avatar = snapshot.child("avatar").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String birthday = snapshot.child("birthday").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);
                    String bio = snapshot.child("bio").getValue(String.class);

                    // Đổ dữ liệu vào các TextView trong layoutConnected
                    binding.txtPartnerNameHeader.setText(name);
                    binding.layoutConnected.txtPartnerFullName.setText(name);
                    binding.layoutConnected.txtPartnerPhone.setText(phone != null ? phone : "Chưa cập nhật");
                    binding.layoutConnected.txtPartnerBirthday.setText(birthday != null ? birthday : "--/--/----");
                    binding.layoutConnected.txtPartnerGender.setText(gender != null ? gender : "Chưa rõ");
                    binding.layoutConnected.txtPartnerBio.setText(bio != null ? bio : "Chưa có giới thiệu");

                    // Hiển thị ảnh đại diện đối phương
                    setBase64Image(avatar, binding.ivPartnerAvatar);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Hàm hỗ trợ chuyển đổi chuỗi Base64 sang ImageView
     */
    private void setBase64Image(String base64String, android.widget.ImageView imageView) {
        if (base64String == null || base64String.isEmpty()) {
            imageView.setImageResource(R.drawable.default_avatar);
            return;
        }
        try {
            byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(decodedByte);
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.default_avatar);
        }
    }
    /**
     * Hiển thị hộp thoại xác nhận trước khi hủy kết nối
     */
    private void confirmDisconnect(String partnerId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Hủy kết nối")
                .setMessage("Bạn có chắc chắn muốn hủy kết nối với đối phương không?")
                .setPositiveButton("Hủy kết nối", (dialog, which) -> {
                    performDisconnect(partnerId);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    /**
     * Thực hiện xóa partnerId của cả 2 phía trên Firebase
     */
    private void performDisconnect(String partnerId) {
        if (myUid == null || partnerId == null) return;

        // 1. Tạo coupleKey để biết "ngăn kéo chung" nằm ở đâu
        String coupleKey;
        if (myUid.compareTo(partnerId) < 0) {
            coupleKey = myUid + "_" + partnerId;
        } else {
            coupleKey = partnerId + "_" + myUid;
        }

        // 2. Tạo Map để xóa partnerId và connectedAt của người dùng
        Map<String, Object> removeUpdates = new HashMap<>();
        removeUpdates.put("partnerId", null);
        removeUpdates.put("connectedAt", null);

        // Bắt đầu chuỗi dọn dẹp (Dùng Task để đảm bảo sạch sẽ)
        // Bước A: Xóa tin nhắn chung
        mDatabase.child("messages").child(coupleKey).removeValue().addOnSuccessListener(aVoid1 -> {

            // Bước B: Xóa ngày kỷ niệm chung
            mDatabase.child("special_days").child(coupleKey).removeValue().addOnSuccessListener(aVoid2 -> {

                // Bước C: Cập nhật lại profile của mình (Về trạng thái độc thân)
                mDatabase.child("users").child(myUid).updateChildren(removeUpdates).addOnSuccessListener(aVoid3 -> {

                    // Bước D: Cập nhật lại profile của đối phương
                    mDatabase.child("users").child(partnerId).updateChildren(removeUpdates).addOnSuccessListener(aVoid4 -> {

                        Toast.makeText(this, "Đã xóa sạch dữ liệu và hủy kết nối!", Toast.LENGTH_SHORT).show();
                        // Lưu ý: UI sẽ tự nhảy về màn hình chưa kết nối nhờ ValueEventListener

                    });
                });
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi khi dọn dẹp dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}