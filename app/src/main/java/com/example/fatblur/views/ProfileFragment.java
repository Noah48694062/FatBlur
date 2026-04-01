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

        // 6. Xử lý sự kiện nhấn nút Đăng xuất
        binding.btnLogOutProfile.setOnClickListener(v -> {
            // Lệnh đăng xuất của Firebase
            mAuth.signOut();

            // Quay về màn hình Login
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            // Xóa sạch bộ nhớ các màn hình trước đó để không bị bấm "Back" quay lại
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Đóng Activity hiện tại (MainActivity)
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
        return view;
    }
    // Phương thức tải thông tin người dùng
    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();

            // Lấy tham chiếu đến đúng User đang đăng nhập trong Database
            mDatabase.child("users").child(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // 1. Lấy tên từ Database (trường "name" bạn đã lưu lúc Đăng ký)
                        String name = snapshot.child("name").getValue(String.class);

                        if (name != null && !name.isEmpty()) {
                            binding.textViewUserName.setText(name);
                        } else {
                            binding.textViewUserName.setText("Người dùng");
                        }

                        // 2. Xử lý ảnh đại diện (Nếu bạn có lưu trường avatarUrl trong DB)
                        // Nếu chưa có, tạm thời dùng mặc định hoặc lấy từ Firebase Auth như cũ
                        if (snapshot.hasChild("avatarUrl")) {
                            String avatarUrl = snapshot.child("avatarUrl").getValue(String.class);
                            Glide.with(ProfileFragment.this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .error(R.drawable.default_avatar)
                                    .into(binding.imageViewAvatar);
                        } else {
                            binding.imageViewAvatar.setImageResource(R.drawable.default_avatar);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ProfileFragment", "Lỗi tải dữ liệu: " + error.getMessage());
                }
            });
        } else {
            binding.textViewUserName.setText("Khách");
            binding.imageViewAvatar.setImageResource(R.drawable.default_avatar);
        }
    }
}