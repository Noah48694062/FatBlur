package com.example.fatblur.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fatblur.R;
import com.example.fatblur.models.Message;
import com.example.fatblur.models.User;
import com.example.fatblur.controllers.ChatAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {
    private RecyclerView rvChat;
    private EditText edtMessage;
    private DatabaseReference chatRef;
    private String myUid, coupleKey;
    private List<Message> messageList = new ArrayList<>();
    private ChatAdapter adapter;
    private View layoutNotConnected, layoutChatActive;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        // 1. Ánh xạ các View (QUAN TRỌNG: Bạn bị thiếu phần này dẫn đến lỗi 'never assigned')
        layoutNotConnected = v.findViewById(R.id.layoutNotConnected);
        layoutChatActive = v.findViewById(R.id.layoutChatActive);
        rvChat = v.findViewById(R.id.rvChat);
        edtMessage = v.findViewById(R.id.edtMessage);
        ImageButton btnSend = v.findViewById(R.id.btnSend);

        myUid = FirebaseAuth.getInstance().getUid();

        // 2. Thiết lập nút gửi (Sửa lỗi 'sendMessage is never used')
        btnSend.setOnClickListener(view -> sendMessage());

        // 3. Kiểm tra kết nối
        checkConnectionStatus();

        return v;
    }

    private void checkConnectionStatus() {
        FirebaseDatabase.getInstance().getReference("users").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
//                        layoutChatActive.setVisibility(View.VISIBLE);
//                        layoutNotConnected.setVisibility(View.GONE);

                        if (user != null && user.partnerId != null && !user.partnerId.isEmpty()) {
                            layoutChatActive.setVisibility(View.VISIBLE);
                            layoutNotConnected.setVisibility(View.GONE);


                            setupChatRoom(user.partnerId);
                        } else {
                            layoutChatActive.setVisibility(View.GONE);
                            layoutNotConnected.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }


    private void setupChatRoom(String partnerId) {
        // Tạo coupleKey đồng bộ
        if (myUid.compareTo(partnerId) < 0) {
            coupleKey = myUid + "_" + partnerId;
        } else {
            coupleKey = partnerId + "_" + myUid;
        }

        chatRef = FirebaseDatabase.getInstance().getReference("messages").child(coupleKey);
        listenForMessages();
    }

    private void listenForMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Message msg = ds.getValue(Message.class);
                    if (msg != null) messageList.add(msg);
                }

                // Khởi tạo Adapter và hiển thị
                adapter = new ChatAdapter(messageList, myUid);
                rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
                rvChat.setAdapter(adapter);

                if (messageList.size() > 0) {
                    rvChat.scrollToPosition(messageList.size() - 1);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendMessage() {
        String content = edtMessage.getText().toString().trim();
        if (!content.isEmpty() && chatRef != null) {
            long currentTime = System.currentTimeMillis();
            Message msg = new Message(myUid, content, System.currentTimeMillis());
            chatRef.push().setValue(msg);
            edtMessage.setText("");
            // 2. Logic tính Streak: Cả 2 cùng nhắn mới tăng
            updateStreakLogic(currentTime);
        }
    }
    private void updateStreakLogic(long currentTime) {
        if (coupleKey == null) return; // Bảo vệ nếu chưa có key
        DatabaseReference streakRef = FirebaseDatabase.getInstance()
                .getReference("messages")
                .child(coupleKey)
                .child("streakInfo");

        // Lấy ngày hiện tại định dạng số (ví dụ: 20260401)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault());
        String today = sdf.format(new java.util.Date(currentTime));

        streakRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String lastDate = snapshot.child("lastDate").getValue(String.class);
                Long count = snapshot.child("count").getValue(Long.class);
                if (count == null) count = 0L;

                boolean user1Sent = snapshot.child("user1Sent").getValue(Boolean.class) != null && snapshot.child("user1Sent").getValue(Boolean.class);
                boolean user2Sent = snapshot.child("user2Sent").getValue(Boolean.class) != null && snapshot.child("user2Sent").getValue(Boolean.class);

                // Kiểm tra xem mình là người thứ mấy trong coupleKey (Uid1_Uid2)
                String[] uids = coupleKey.split("_");
                boolean isUser1 = myUid.equals(uids[0]);

                if (today.equals(lastDate)) {
                    // NẾU LÀ CÙNG MỘT NGÀY
                    if (isUser1) user1Sent = true;
                    else user2Sent = true;

                    // Nếu sau khi mình nhắn, cả 2 đều đã nhắn trong hôm nay
                    if (user1Sent && user2Sent) {
                        // Kiểm tra xem đã tăng streak cho ngày hôm nay chưa (tránh tăng nhiều lần 1 ngày)
                        boolean alreadyIncremented = snapshot.child("incrementedToday").getValue(Boolean.class) != null && snapshot.child("incrementedToday").getValue(Boolean.class);
                        if (!alreadyIncremented) {
                            streakRef.child("count").setValue(count + 1);
                            streakRef.child("incrementedToday").setValue(true);
                        }
                    }
                } else {
                    // NẾU SANG NGÀY MỚI
                    long lastDateLong = (lastDate != null) ? Long.parseLong(lastDate) : 0;
                    long todayLong = Long.parseLong(today);

                    if (todayLong - lastDateLong > 1) {
                        // Nếu bỏ lỡ hơn 1 ngày -> Reset Streak
                        count = 0L;
                    }

                    // Reset trạng thái nhắn tin cho ngày mới
                    user1Sent = isUser1;
                    user2Sent = !isUser1;
                    streakRef.child("lastDate").setValue(today);
                    streakRef.child("count").setValue(count);
                    streakRef.child("incrementedToday").setValue(false);
                }

                // Cập nhật trạng thái người gửi
                streakRef.child(isUser1 ? "user1Sent" : "user2Sent").setValue(true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}