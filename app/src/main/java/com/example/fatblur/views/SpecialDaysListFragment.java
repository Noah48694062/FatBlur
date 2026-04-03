package com.example.fatblur.views;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fatblur.R;
import com.example.fatblur.controllers.SpecialDaysAdapter;
import com.example.fatblur.models.SpecialDay;
import com.example.fatblur.models.User;
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
    private SpecialDaysAdapter adapter;
    private String myUid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_special_days_list, container, false);
        rv = v.findViewById(R.id.rvSpecialDays);

        // Khởi tạo Adapter và RecyclerView trước
        adapter = new SpecialDaysAdapter(specialDays);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        myUid = FirebaseAuth.getInstance().getUid();

        // Nút xóa tất cả
        v.findViewById(R.id.btnDeleteAll).setOnClickListener(view -> confirmDeleteAll());

        // QUAN TRỌNG: Phải tìm coupleKey trước rồi mới load được dữ liệu
        initSharedDatabase();

        return v;
    }

    private void initSharedDatabase() {
        // Tìm partnerId của mình trong node users
        FirebaseDatabase.getInstance().getReference("users").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        String coupleKey = myUid; // Mặc định dùng ID cá nhân nếu chưa kết nối

                        if (user != null && user.partnerId != null && !user.partnerId.isEmpty()) {
                            // Tạo coupleKey chung (Logic phải y hệt bên CalendarFragment)
                            String partnerId = user.partnerId;
                            if (myUid.compareTo(partnerId) < 0) {
                                coupleKey = myUid + "_" + partnerId;
                            } else {
                                coupleKey = partnerId + "_" + myUid;
                            }
                        }

                        // Sau khi đã có coupleKey, trỏ mDatabase vào đúng "ngăn kéo chung"
                        mDatabase = FirebaseDatabase.getInstance().getReference("special_days").child(coupleKey);

                        // Bắt đầu lắng nghe dữ liệu từ ngăn kéo chung này
                        loadSpecialDays();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadSpecialDays() {
        if (mDatabase == null) return;

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                specialDays.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    // Firebase trả về các node, chúng ta chỉ lấy những node có dữ liệu kỷ niệm
                    SpecialDay day = data.getValue(SpecialDay.class);
                    if (day != null && day.isSpecial) {
                        // Gán tạm ID hiển thị là ngày tháng để Adapter dễ dùng
                        day.specialDayId = formatKeyToDate(data.getKey());
                        specialDays.add(day);
                    }
                }
                adapter.notifyDataSetChanged(); // Cập nhật lại danh sách trên màn hình
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String formatKeyToDate(String key) {
        if (key == null || key.length() < 8) return "N/A";
        // Chuyển 20260320 thành 20/03/2026
        return key.substring(6, 8) + "/" + key.substring(4, 6) + "/" + key.substring(0, 4);
    }

    private void confirmDeleteAll() {
        if (mDatabase == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Xóa tất cả kỷ niệm?")
                .setMessage("Thao tác này sẽ xóa sạch kỷ niệm của cả hai người. Bạn chắc chắn chứ?")
                .setPositiveButton("Xóa sạch", (d, w) -> {
                    mDatabase.removeValue().addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Đã xóa sạch ngăn kéo chung!", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Hủy", null).show();
    }
}