package com.example.fatblur.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fatblur.R;
import com.example.fatblur.models.SpecialDay;
import com.example.fatblur.models.User;
import com.example.fatblur.utils.ReminderReceiver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CalendarFragment extends Fragment {
    private TextView txtMonthYear;
    private RecyclerView recyclerView;
    private LocalDate selectedDate;
    private DatabaseReference mDatabase;
    private String userId;
    private Set<String> specialDaysList = new HashSet<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_calendar, container, false);

        // 1. Ánh xạ các View (Sử dụng findViewById truyền thống)
        txtMonthYear = v.findViewById(R.id.txtMonthYear);
        recyclerView = v.findViewById(R.id.calendarRecyclerView);
        ImageButton btnPrev = v.findViewById(R.id.btnPrevMonth);
        ImageButton btnNext = v.findViewById(R.id.btnNextMonth);
        ImageButton btnShowList = v.findViewById(R.id.btnShowList);

        // 2. Khởi tạo dữ liệu cơ bản
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        selectedDate = LocalDate.now();

        // 3. Logic xác định "Ngăn kéo chung" cho cặp đôi
        initSharedDatabase();

        // 4. Thiết lập các sự kiện Click
        btnPrev.setOnClickListener(view -> {
            selectedDate = selectedDate.minusMonths(1);
            fetchSpecialDaysAndSetCalendar();
        });

        btnNext.setOnClickListener(view -> {
            selectedDate = selectedDate.plusMonths(1);
            fetchSpecialDaysAndSetCalendar();
        });

        txtMonthYear.setOnClickListener(v1 -> showYearSelector());

        // CHỖ NÀY QUAN TRỌNG: Phải nằm TRƯỚC lệnh return v;
        btnShowList.setOnClickListener(v1 -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SpecialDaysListFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // 5. Trả về view để hiển thị
        return v;
    }

    private void initSharedDatabase() {
        // Lấy thông tin partnerId từ node users
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                String coupleKey = userId; // Mặc định nếu chưa kết nối là dùng ID cá nhân

                if (user != null && user.partnerId != null && !user.partnerId.isEmpty()) {
                    // Tạo khóa chung uid1_uid2 để cả 2 cùng truy cập vào 1 chỗ
                    String partnerId = user.partnerId;
                    if (userId.compareTo(partnerId) < 0) {
                        coupleKey = userId + "_" + partnerId;
                    } else {
                        coupleKey = partnerId + "_" + userId;
                    }
                }

                // Trỏ mDatabase vào đúng ngăn kéo chung
                mDatabase = FirebaseDatabase.getInstance().getReference("special_days").child(coupleKey);

                // Sau khi có mDatabase mới bắt đầu lấy dữ liệu kỷ niệm
                fetchSpecialDaysAndSetCalendar();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchSpecialDaysAndSetCalendar() {
        if (mDatabase == null) return;

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                specialDaysList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    SpecialDay day = data.getValue(SpecialDay.class);
                    if (day != null && day.isSpecial) {
                        specialDaysList.add(data.getKey());
                    }
                }
                setMonthView();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Các hàm setMonthView, daysInMonthArray, showYearSelector... giữ nguyên như cũ
    // Chỉ cần đảm bảo showEditDialog sử dụng mDatabase (đã được trỏ vào coupleKey) để lưu dữ liệu

    private void setMonthView() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'Tháng' MM 'năm' yyyy", new Locale("vi", "VN"));
        txtMonthYear.setText(selectedDate.format(formatter));

        ArrayList<LocalDate> daysInMonth = daysInMonthArray(selectedDate);
        CalendarAdapter adapter = new CalendarAdapter(daysInMonth, date -> {
            String dateKey = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            showEditDialog(dateKey, date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        });

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 7));
        recyclerView.setAdapter(adapter);
    }

    private ArrayList<LocalDate> daysInMonthArray(LocalDate date) {
        ArrayList<LocalDate> daysArray = new ArrayList<>();
        YearMonth yearMonth = YearMonth.from(date);
        int daysInMonth = yearMonth.lengthOfMonth();

        LocalDate firstOfMonth = selectedDate.withDayOfMonth(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // T2=1 ... CN=7

        // Tính toán để căn chỉnh ngày 1 vào đúng cột thứ
        int shift = dayOfWeek - 1;

        for (int i = 1; i <= 42; i++) {
            if (i <= shift || i > daysInMonth + shift) {
                daysArray.add(null);
            } else {
                daysArray.add(LocalDate.of(selectedDate.getYear(), selectedDate.getMonth(), i - shift));
            }
        }
        return daysArray;
    }

    private void showYearSelector() {
        final android.widget.NumberPicker picker = new android.widget.NumberPicker(getContext());
        picker.setMinValue(2020);
        picker.setMaxValue(2050);
        picker.setValue(selectedDate.getYear());

        new AlertDialog.Builder(getContext())
                .setTitle("Chọn năm")
                .setView(picker)
                .setPositiveButton("OK", (dialog, which) -> {
                    selectedDate = selectedDate.withYear(picker.getValue());
                    fetchSpecialDaysAndSetCalendar();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // --- CalendarAdapter ---
    private class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
        private final ArrayList<LocalDate> days;
        private final OnItemListener onItemListener;

        public CalendarAdapter(ArrayList<LocalDate> days, OnItemListener onItemListener) {
            this.days = days;
            this.onItemListener = onItemListener;
        }

        @NonNull
        @Override
        public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_cell, parent, false);
            return new CalendarViewHolder(view, onItemListener, days);
        }

        @Override
        public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
            LocalDate date = days.get(position);
            if (date != null) {
                holder.dayOfMonth.setText(String.valueOf(date.getDayOfMonth()));
                String dateKey = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

                if (specialDaysList.contains(dateKey)) {
                    holder.bgSpecial.setVisibility(View.VISIBLE);
                    holder.dayOfMonth.setTextColor(0xFFFF5864); // Chữ trắng trên nền hồng
                } else {
                    holder.bgSpecial.setVisibility(View.GONE);
                    holder.dayOfMonth.setTextColor(0xFF333333);
                }
            } else {
                holder.dayOfMonth.setText("");
                holder.bgSpecial.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return days.size(); }

        class CalendarViewHolder extends RecyclerView.ViewHolder {
            TextView dayOfMonth;
            View bgSpecial;

            public CalendarViewHolder(@NonNull View itemView, OnItemListener onItemListener, ArrayList<LocalDate> days) {
                super(itemView);
                dayOfMonth = itemView.findViewById(R.id.cellDayText);
                bgSpecial = itemView.findViewById(R.id.bgSpecial);
                itemView.setOnClickListener(v -> {
                    LocalDate d = days.get(getAdapterPosition());
                    if (d != null) onItemListener.onItemClick(d);
                });
            }
        }
    }

    public interface OnItemListener { void onItemClick(LocalDate date); }

    private void showEditDialog(String dateKey, String displayDate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_special_day, null);
        builder.setView(dialogView);

        TextView txtDate = dialogView.findViewById(R.id.txtSelectedDate);
        CheckBox cbSpecial = dialogView.findViewById(R.id.cbIsSpecial);
        CheckBox cbRepeat = dialogView.findViewById(R.id.cbIsRepeat);
        EditText edtNote = dialogView.findViewById(R.id.edtNote);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        txtDate.setText("Ngày " + displayDate);

        // --- BẮT ĐẦU PHẦN LOGIC MỚI ---

        // 1. Mặc định ban đầu: Vô hiệu hóa ghi chú và lặp lại
        cbRepeat.setEnabled(false);
        edtNote.setEnabled(false);

        // 2. Lắng nghe sự kiện khi Tick/Untick vào checkbox "Ngày đặc biệt"
        cbSpecial.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Nếu isChecked là true (đã chọn), thì cho phép sửa, ngược lại thì khóa
            cbRepeat.setEnabled(isChecked);
            edtNote.setEnabled(isChecked);

            if (!isChecked) {
                // Tùy chọn: Xóa trắng dữ liệu nếu người dùng bỏ chọn "Ngày đặc biệt"
                cbRepeat.setChecked(false);
                edtNote.setText("");
            }
        });

        // --- KẾT THÚC PHẦN LOGIC MỚI ---

        AlertDialog dialog = builder.create();

        // Tải dữ liệu cũ từ Firebase
        mDatabase.child(dateKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                SpecialDay day = snapshot.getValue(SpecialDay.class);
                if (day != null) {
                    cbSpecial.setChecked(day.isSpecial);
                    cbRepeat.setChecked(day.isRepeat);
                    edtNote.setText(day.note);

                    // Quan trọng: Cập nhật lại trạng thái Enable dựa trên dữ liệu cũ
                    cbRepeat.setEnabled(day.isSpecial);
                    edtNote.setEnabled(day.isSpecial);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        btnSave.setOnClickListener(v -> {
            SpecialDay dayInfo = new SpecialDay(
                    cbSpecial.isChecked(),
                    cbRepeat.isChecked(),
                    edtNote.getText().toString(),
                    System.currentTimeMillis(),
                    userId
            );

            mDatabase.child(dateKey).setValue(dayInfo).addOnSuccessListener(aVoid -> {
                // Nếu là ngày đặc biệt thì đặt lịch thông báo luôn
                if (cbSpecial.isChecked()) {
                    scheduleNotification(dateKey, edtNote.getText().toString());
                }

                Toast.makeText(getContext(), "Đã lưu kỷ niệm chung!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });


        });

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void scheduleNotification(String dateKey, String note) {
        // 1. Khởi tạo Calendar
        java.util.Calendar calendar = java.util.Calendar.getInstance();

        // --- DÒNG ĐỂ TEST: Thông báo sẽ nổ sau 10 giây kể từ khi nhấn Lưu ---
        calendar.setTimeInMillis(System.currentTimeMillis() + 10000);
        // ---------------------------------------------------------------

        // 2. Khởi tạo Intent và PendingIntent (Giữ nguyên)
        Intent intent = new Intent(getContext(), ReminderReceiver.class);
        intent.putExtra("title", "Test thông báo SecretLove ❤️");
        intent.putExtra("message", note.isEmpty() ? "Kỷ niệm đang đến kìa!" : note);

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                getContext(),
                (int) System.currentTimeMillis(), // Dùng thời gian hiện tại làm ID để không bị trùng khi test nhiều lần
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        // 3. Đặt báo thức với AlarmManager (Giữ nguyên logic kiểm tra quyền của bạn)
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.set(
                            android.app.AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        }
    }

//    private void scheduleNotification(String dateKey, String note) {
//        // --- PHẦN BỊ THIẾU DẪN ĐẾN LỖI ĐỎ ---
//        // 1. Khởi tạo Calendar từ dateKey (yyyyMMdd)
//        int year = Integer.parseInt(dateKey.substring(0, 4));
//        int month = Integer.parseInt(dateKey.substring(4, 6));
//        int day = Integer.parseInt(dateKey.substring(6, 8));
//
//        java.util.Calendar calendar = java.util.Calendar.getInstance();
//        calendar.set(year, month - 1, day, 8, 0, 0); // Đặt lịch lúc 8 giờ sáng
//
//        // Nếu ngày đó đã trôi qua thì không đặt nữa
//        if (calendar.getTimeInMillis() < System.currentTimeMillis()) return;
//
//        // 2. Khởi tạo Intent và PendingIntent
//        Intent intent = new Intent(getContext(), ReminderReceiver.class);
//        intent.putExtra("title", "Hôm nay là ngày đặc biệt!");
//        intent.putExtra("message", note.isEmpty() ? "Vào xem kỷ niệm của chúng mình nhé ❤️" : note);
//
//        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
//                getContext(),
//                dateKey.hashCode(), // ID duy nhất cho mỗi ngày
//                intent,
//                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
//        );
//        // --- HẾT PHẦN BỊ THIẾU ---
//
//        // 3. Đặt báo thức với AlarmManager (Đoạn này giữ nguyên logic kiểm tra quyền của bạn)
//        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
//        if (alarmManager != null) {
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
//                if (alarmManager.canScheduleExactAlarms()) {
//                    alarmManager.setExactAndAllowWhileIdle(
//                            android.app.AlarmManager.RTC_WAKEUP,
//                            calendar.getTimeInMillis(),
//                            pendingIntent
//                    );
//                } else {
//                    alarmManager.set(
//                            android.app.AlarmManager.RTC_WAKEUP,
//                            calendar.getTimeInMillis(),
//                            pendingIntent
//                    );
//                }
//            } else {
//                alarmManager.setExactAndAllowWhileIdle(
//                        android.app.AlarmManager.RTC_WAKEUP,
//                        calendar.getTimeInMillis(),
//                        pendingIntent
//                );
//            }
//        }
//    }

}