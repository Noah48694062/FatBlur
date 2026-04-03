package com.example.fatblur.controllers;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fatblur.R;
import com.example.fatblur.models.Message;
import com.google.firebase.database.DatabaseReference;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private List<Message> messageList;
    private String myUid;
    private DatabaseReference chatDatabase;

    // Cần Context để hiển thị Dialog và Toast
    public ChatAdapter(List<Message> messageList, String myUid) {
        this.messageList = messageList;
        this.myUid = myUid;
    }

    public void setChatDatabaseReference(DatabaseReference ref) {
        this.chatDatabase = ref;
    }

    @Override
    public int getItemViewType(int position) {
        if (messageList.get(position).getSenderId().equals(myUid)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messageList.get(position);
        TextView currentBubble;

        if (holder instanceof SentViewHolder) {
            SentViewHolder sentHolder = (SentViewHolder) holder;
            sentHolder.txtMessage.setText(msg.getContent());
            currentBubble = sentHolder.txtMessage; // Lấy đúng cái TextView màu hồng
        } else {
            ReceivedViewHolder recHolder = (ReceivedViewHolder) holder;
            recHolder.txtMessage.setText(msg.getContent());
            currentBubble = recHolder.txtMessage; // Lấy cái TextView màu nhạt
        }

        // Gán nhấn giữ vào CÁI BONG BÓNG (currentBubble), không phải holder.itemView
        currentBubble.setOnLongClickListener(v -> {
            if (msg.getSenderId().equals(myUid)) {
                showChatMenu(v, msg); // v ở đây chính là cái TextView
            }
            return true;
        });
    }

    private void showChatMenu(View v, Message msg) {
        PopupMenu popup = new PopupMenu(v.getContext(), v);
        popup.getMenu().add("Chỉnh sửa");
        popup.getMenu().add("Thu hồi");

        popup.setOnMenuItemClickListener(item -> {
            if (chatDatabase == null) {
                Toast.makeText(v.getContext(), "Lỗi kết nối dữ liệu!", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (item.getTitle().equals("Thu hồi")) {
                // Thu hồi tin nhắn
                chatDatabase.child(msg.getMessageId()).removeValue()
                        .addOnSuccessListener(aVoid -> Toast.makeText(v.getContext(), "Đã thu hồi", Toast.LENGTH_SHORT).show());
            } else {
                // Định nghĩa hàm sửa tin nhắn
                showEditDialog(v.getContext(), msg);
            }
            return true;
        });
        popup.show();
    }

    // --- HÀM CHỈNH SỬA TIN NHẮN ---
    private void showEditDialog(Context context, Message msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Chỉnh sửa tin nhắn");

        // Tạo EditText để nhập nội dung mới
        final EditText input = new EditText(context);
        input.setText(msg.getContent()); // Hiển thị nội dung cũ
        input.setPadding(50, 40, 50, 40);
        builder.setView(input);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newContent = input.getText().toString().trim();
            if (!newContent.isEmpty() && !newContent.equals(msg.getContent())) {
                // Cập nhật nội dung mới kèm ghi chú (đã chỉnh sửa)
                chatDatabase.child(msg.getMessageId()).child("content").setValue(newContent + " (đã chỉnh sửa)");
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public int getItemCount() { return messageList.size(); }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage;
        SentViewHolder(View v) { super(v); txtMessage = v.findViewById(R.id.txtMessage); }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage;
        ReceivedViewHolder(View v) { super(v); txtMessage = v.findViewById(R.id.txtMessage); }
    }
}