package com.example.fatblur.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fatblur.R;

public class StatusFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Bạn có thể tạo 1 file xml đơn giản fragment_placeholder.xml có 1 TextView bên trong
        return inflater.inflate(R.layout.fragment_status, container, false);
    }
}
