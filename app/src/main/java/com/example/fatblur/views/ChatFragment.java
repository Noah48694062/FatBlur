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
            Message msg = new Message(myUid, content, System.currentTimeMillis());
            chatRef.push().setValue(msg);
            edtMessage.setText("");
        }
    }
}