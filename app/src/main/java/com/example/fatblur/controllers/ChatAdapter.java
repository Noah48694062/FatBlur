package com.example.fatblur.controllers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fatblur.R;
import com.example.fatblur.models.Message;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<Message> messageList;
    private String myUid;

    public ChatAdapter(List<Message> messageList, String myUid) {
        this.messageList = messageList;
        this.myUid = myUid;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Tạm thời dùng 1 layout chung, sau này bạn nên tách ra Sent và Received
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Message msg = messageList.get(position);
        TextView txt = holder.itemView.findViewById(android.R.id.text1);
        // Hiển thị: "Người gửi: Nội dung"
        txt.setText((msg.senderId.equals(myUid) ? "Bạn: " : "Người ấy: ") + msg.content);
    }

    @Override
    public int getItemCount() { return messageList.size(); }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        public ChatViewHolder(@NonNull View itemView) { super(itemView); }
    }
}