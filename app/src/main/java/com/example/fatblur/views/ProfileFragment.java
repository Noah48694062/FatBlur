package com.example.fatblur.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fatblur.R;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    private Button btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Nạp giao diện XML vào Fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        btnLogout = view.findViewById(R.id.btnLogOutProfile);

        btnLogout.setOnClickListener(v -> {
            // 1. Lệnh đăng xuất của Firebase
            FirebaseAuth.getInstance().signOut();

            // 2. Quay về màn hình Login
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
}