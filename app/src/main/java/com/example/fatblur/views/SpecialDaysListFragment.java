package com.example.fatblur.views;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fatblur.R;
import com.example.fatblur.controllers.SpecialDaysAdapter;
import com.example.fatblur.models.SpecialDay;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SpecialDaysListFragment extends Fragment {
    private RecyclerView rv;
    private DatabaseReference mDatabase;
    private List<SpecialDay> specialDays = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_special_days_list, container, false);
        rv = v.findViewById(R.id.rvSpecialDays);
        String uid = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("special_days").child(uid);

        // Nút xóa tất cả
        v.findViewById(R.id.btnDeleteAll).setOnClickListener(view -> confirmDeleteAll());

        loadSpecialDays();
        return v;
    }

    private void loadSpecialDays() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                specialDays.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    SpecialDay day = data.getValue(SpecialDay.class);
                    if (day != null && day.isSpecial) {
                        day.specialDayId = formatKeyToDate(data.getKey());
                        specialDays.add(day);
                    }
                }
                rv.setLayoutManager(new LinearLayoutManager(getContext()));
                rv.setAdapter(new SpecialDaysAdapter(specialDays));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String formatKeyToDate(String key) {
        // Chuyển 20260320 thành 20/03/2026
        return key.substring(6, 8) + "/" + key.substring(4, 6) + "/" + key.substring(0, 4);
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(getContext())
                .setTitle("Xóa tất cả kỷ niệm?")
                .setMessage("Bạn có chắc muốn xóa sạch danh sách này?")
                .setPositiveButton("Xóa", (d, w) -> mDatabase.removeValue())
                .setNegativeButton("Hủy", null).show();
    }
}