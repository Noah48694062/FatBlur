package com.example.fatblur.views;

import android.content.Context;
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
    private DatabaseReference chatRef; // messages/coupleKey
    private String myUid, coupleKey;
    private List<Message> messageList = new ArrayList<>();
    private ChatAdapter adapter;
    private View layoutNotConnected, layoutChatActive;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        layoutNotConnected = v.findViewById(R.id.layoutNotConnected);
        layoutChatActive = v.findViewById(R.id.layoutChatActive);
        rvChat = v.findViewById(R.id.rvChat);
        edtMessage = v.findViewById(R.id.edtMessage);
        ImageButton btnSend = v.findViewById(R.id.btnSend);

        myUid = FirebaseAuth.getInstance().getUid();

        // KHỞI TẠO ADAPTER
        // Chú ý: Chúng ta sẽ truyền 'chatRef' vào sau khi có coupleKey
        adapter = new ChatAdapter(messageList, myUid);
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChat.setAdapter(adapter);

        btnSend.setOnClickListener(view -> sendMessage());

        checkConnectionStatus();
        return v;
    }

    private void setupChatRoom(String partnerId) {
        if (myUid.compareTo(partnerId) < 0) {
            coupleKey = myUid + "_" + partnerId;
        } else {
            coupleKey = partnerId + "_" + myUid;
        }

        chatRef = FirebaseDatabase.getInstance().getReference("messages").child(coupleKey);

        // CỰC KỲ QUAN TRỌNG: Truyền tham chiếu database vào Adapter để Adapter có quyền XÓA/SỬA
        adapter.setChatDatabaseReference(chatRef.child("chats"));

        listenForMessages();
    }

    private void sendMessage() {
        String content = edtMessage.getText().toString().trim();
        if (!content.isEmpty() && chatRef != null) {
            long currentTime = System.currentTimeMillis();

            // 1. Tạo một tham chiếu mới với ID tự động (Key)
            DatabaseReference newMsgRef = chatRef.child("chats").push();
            String messageId = newMsgRef.getKey();

            // 2. Tạo đối tượng tin nhắn và gán ID cho nó
            Message msg = new Message(myUid, content, currentTime);
            msg.setMessageId(messageId);

            // 3. Đẩy lên Firebase
            newMsgRef.setValue(msg);

            edtMessage.setText("");
            updateStreakLogic(currentTime);
        }
    }

    private void checkConnectionStatus() {
        FirebaseDatabase.getInstance().getReference("users").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && user.partnerId != null && !user.partnerId.isEmpty()) {
                            layoutChatActive.setVisibility(View.VISIBLE);
                            layoutNotConnected.setVisibility(View.GONE);
                            setupChatRoom(user.partnerId);
                        } else {
                            layoutChatActive.setVisibility(View.GONE);
                            layoutNotConnected.setVisibility(View.VISIBLE);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }



//    private void listenForMessages() {
//        chatRef.child("chats").addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                messageList.clear();
//                for (DataSnapshot ds : snapshot.getChildren()) {
//                    Message msg = ds.getValue(Message.class);
//                    if (msg != null) messageList.add(msg);
//                }
//
//                // Cập nhật dữ liệu cho Adapter thay vì tạo mới
//                adapter.notifyDataSetChanged();
//
//                if (messageList.size() > 0) {
//                    rvChat.scrollToPosition(messageList.size() - 1);
//                }
//            }
//            @Override public void onCancelled(@NonNull DatabaseError error) {}
//        });
//    }
private void listenForMessages() {
    chatRef.child("chats").addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            Message lastIncomingMsg = null;

            messageList.clear();
            for (DataSnapshot ds : snapshot.getChildren()) {
                Message msg = ds.getValue(Message.class);
                if (msg != null) {
                    messageList.add(msg);
                    if (!msg.getSenderId().equals(myUid)) {
                        lastIncomingMsg = msg;
                    }
                }
            }

            adapter.notifyDataSetChanged();

            if (messageList.size() > 0) {
                rvChat.scrollToPosition(messageList.size() - 1);

                if (lastIncomingMsg != null) {
                    long timeDiff = System.currentTimeMillis() - lastIncomingMsg.getTimestamp();

                    if (timeDiff < 3000) {
                        // --- BẮT ĐẦU ĐỌC CẤU HÌNH CONFIG CHAT ---
                        if (getContext() != null) {
                            android.content.SharedPreferences prefs =
                                    getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                            boolean isMsgEnabled = prefs.getBoolean("message_notification", true);

                            // Chỉ nổ thông báo khi người dùng đang bật Switch tin nhắn
                            if (isMsgEnabled) {
                                showLocalChatNotification("Người yêu: ❤️", lastIncomingMsg.getContent());
                            }
                        }
                        // --- KẾT THÚC KIỂM TRA ---
                    }
                }
            }
        }
        @Override public void onCancelled(@NonNull DatabaseError error) {}
    });
}

    private void showLocalChatNotification(String title, String messageContent) {
        // Kiểm tra an toàn xem Fragment đã được gắn vào Activity chưa, tránh crash máy ảo
        if (!isAdded() || getContext() == null) return;

        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        // Sử dụng chung mã kênh "special_day_channel" ông đã khởi tạo thành công ở MainActivity
        androidx.core.app.NotificationCompat.Builder builder =
                new androidx.core.app.NotificationCompat.Builder(getContext(), "special_day_channel")
                        .setSmallIcon(R.drawable.ic_launcher_foreground) // Icon nhỏ hiển thị trên thanh trạng thái
                        .setContentTitle(title)
                        .setContentText(messageContent)
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH) // Đảm bảo hiện pop-up trên đầu màn hình
                        .setAutoCancel(true); // Nhấn vào tự động xóa thông báo

        if (notificationManager != null) {
            // Dùng thời gian hệ thống làm ID để các thông báo test không bị ghi đè lên nhau
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
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