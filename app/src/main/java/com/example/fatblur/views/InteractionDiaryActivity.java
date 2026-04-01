package com.example.fatblur.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fatblur.R;
import com.example.fatblur.databinding.ActivityInteractionDiaryBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.concurrent.TimeUnit;

public class InteractionDiaryActivity extends AppCompatActivity {

    private ActivityInteractionDiaryBinding binding;
    private DatabaseReference mDatabase;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInteractionDiaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mDatabase = FirebaseDatabase.getInstance().getReference();
        myUid = FirebaseAuth.getInstance().getUid();

        binding.btnBack.setOnClickListener(v -> finish());

        if (myUid != null) {
            loadInteractionData();
        }
    }

    private void loadInteractionData() {
        // 1. Lấy dữ liệu của bản thân
        mDatabase.child("users").child(myUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot mySnapshot) {
                if (mySnapshot.exists()) {
                    // Hiển thị tên và ảnh của mình
                    String myName = mySnapshot.child("name").getValue(String.class);
                    String myAvatar = mySnapshot.child("avatar").getValue(String.class);
                    binding.txtMyNameHeader.setText(myName != null ? myName : "Bạn");
                    setBase64Image(myAvatar, binding.ivMyAvatar);

                    // Lấy PartnerId để truy vấn tiếp
                    String partnerId = mySnapshot.child("partnerId").getValue(String.class);
                    Long connectedAt = mySnapshot.child("connectedAt").getValue(Long.class);

                    // Tính số ngày đã kết nối
                    if (connectedAt != null) {
                        calculateConnectedDays(connectedAt);
                    }

                    if (partnerId != null && !partnerId.isEmpty()) {
                        loadPartnerData(partnerId);
                        // Tạo coupleKey giống như bên ChatFragment
                        String coupleKey;
                        if (myUid.compareTo(partnerId) < 0) {
                            coupleKey = myUid + "_" + partnerId;
                        } else {
                            coupleKey = partnerId + "_" + myUid;
                        }

                        // Gọi hàm với tham số là String coupleKey
                        calculateStreak(coupleKey);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadPartnerData(String partnerId) {
        mDatabase.child("users").child(partnerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String pName = snapshot.child("name").getValue(String.class);
                    String pAvatar = snapshot.child("avatar").getValue(String.class);
                    binding.txtPartnerNameHeader.setText(pName);
                    setBase64Image(pAvatar, binding.ivPartnerAvatar);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Tính toán số ngày từ lúc kết nối đến hiện tại
     */
    private void calculateConnectedDays(long connectedAt) {
        long diffInMs = System.currentTimeMillis() - connectedAt;
        long days = TimeUnit.MILLISECONDS.toDays(diffInMs);
        // Nếu mới kết nối cùng ngày thì hiển thị 1 ngày (hoặc 0 tùy bạn)
        binding.txtConnectedDays.setText(String.valueOf(days + 1));
    }

    /**
     * Logic tính Streak đơn giản (Dựa trên dữ liệu bạn lưu)
     */
    private void calculateStreak(String coupleKey) {
        mDatabase.child("messages").child(coupleKey).child("streakInfo")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long streakCount = snapshot.child("count").getValue(Long.class);
                        binding.txtChatStreak.setText(streakCount != null ? String.valueOf(streakCount) : "0");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

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
}