package com.example.fatblur.views;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.fatblur.R;
import com.example.fatblur.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;         // Import Realtime Database
import com.google.firebase.database.DatabaseError;        // Import Realtime Database
import com.google.firebase.database.DatabaseReference;    // Import Realtime Database
import com.google.firebase.database.FirebaseDatabase;    // Import Realtime Database
import com.google.firebase.database.ValueEventListener;   // Import Realtime Database

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private Button btnLogout;
    private FragmentProfileBinding binding; // Khai báo View Binding
    private FirebaseAuth mAuth; // Khai báo FirebaseAuth
    private DatabaseReference mDatabase; // Khai báo DatabaseReference

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance(); // Khởi tạo FirebaseAuth
        mDatabase = FirebaseDatabase.getInstance().getReference(); // Khởi tạo Realtime Database
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Nạp giao diện XML vào Fragment
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // 1. Tải và hiển thị thông tin người dùng (Tên và Avatar)
        loadUserProfile();

        // 2. Xử lý sự kiện nhấn vào Avatar (chuyển đến trang chỉnh sửa hồ sơ)
        binding.cardViewAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        // 3. Xử lý sự kiện nhấn vào mục "Quản lý kết nối"
        binding.layoutConnectionManagement.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ConnectionManagementActivity.class);
            startActivity(intent);
        });

        // 4. Xử lý sự kiện nhấn vào mục "Cài đặt thông báo"
        binding.layoutNotificationSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NotificationSettingsActivity.class);
            startActivity(intent);
        });

        // 5. Xử lý sự kiện nhấn vào mục "Nhật ký tương tác"
        binding.layoutInteractionDiary.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), InteractionDiaryActivity.class);
            startActivity(intent);
        });

        binding.layoutPrivacySettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PrivacySettingsActivity.class);
            startActivity(intent);
        });

        // 6. Xử lý sự kiện nhấn nút Đăng xuất
        // 6. Xử lý sự kiện nhấn nút Đăng xuất
        binding.btnLogOutProfile.setOnClickListener(v -> {
            // 1. Dựng cờ: "Tôi đang chủ động thoát đây!"
            LoginActivity.isLoggingOut = true;

            String uid = mAuth.getUid();
            if (uid != null) {
                Map<String, Object> logoutUpdates = new HashMap<>();
                logoutUpdates.put("isOnline", false);
                logoutUpdates.put("currentSessionId", "");

                mDatabase.child("user_status").child(uid).updateChildren(logoutUpdates)
                        .addOnCompleteListener(task -> {
                            mAuth.signOut();

                            // Xóa trắng Session ID ở bộ nhớ máy luôn cho sạch
                            com.example.fatblur.models.SessionManager.saveSessionId(getActivity(), "");

                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            // Flag này giúp xóa sạch lịch sử các màn hình trước đó
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            if (getActivity() != null) getActivity().finish();
                        });
            }
        });
        return view;
    }
    // Phương thức tải thông tin người dùng
    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();

            mDatabase.child("users").child(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // 1. Hiển thị tên
                        String name = snapshot.child("name").getValue(String.class);
                        binding.textViewUserName.setText((name != null && !name.isEmpty()) ? name : "Người dùng");

                        // 2. Hiển thị Avatar (Xử lý chuỗi Base64)
                        // CHÚ Ý: Đổi từ 'avatarUrl' thành 'avatar' cho khớp với EditProfileActivity
                        if (snapshot.hasChild("avatar")) {
                            String avatarBase64 = snapshot.child("avatar").getValue(String.class);

                            if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                                try {
                                    // Giải mã chuỗi Base64 thành mảng byte
                                    byte[] imageBytes = android.util.Base64.decode(avatarBase64, android.util.Base64.DEFAULT);

                                    // Dùng Glide để load mảng byte này vào ImageView
                                    Glide.with(ProfileFragment.this)
                                            .load(imageBytes)
                                            .placeholder(R.drawable.default_avatar)
                                            .error(R.drawable.default_avatar)
                                            .circleCrop() // Bo tròn ảnh cho đẹp giống Messenger
                                            .into(binding.imageViewAvatar);
                                } catch (Exception e) {
                                    Log.e("ProfileFragment", "Lỗi giải mã ảnh: " + e.getMessage());
                                    binding.imageViewAvatar.setImageResource(R.drawable.default_avatar);
                                }
                            }
                        } else {
                            // Nếu chưa có ảnh trong DB thì dùng ảnh mặc định
                            binding.imageViewAvatar.setImageResource(R.drawable.default_avatar);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ProfileFragment", "Lỗi tải dữ liệu: " + error.getMessage());
                }
            });
        }
    }
}