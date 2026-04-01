package com.example.fatblur.controllers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fatblur.R;
import com.example.fatblur.models.SpecialDay;

import java.util.List;

public class SpecialDaysAdapter extends RecyclerView.Adapter<SpecialDaysAdapter.ViewHolder> {
    private List<SpecialDay> list;

    public SpecialDaysAdapter(List<SpecialDay> list) { this.list = list; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_special_day, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SpecialDay day = list.get(position);
        // Định dạng lại ngày từ key (yyyyMMdd -> dd/MM/yyyy) nếu cần
        holder.txtDate.setText(day.specialDayId);
        holder.txtNote.setText(day.note);
    }

    @Override
    public int getItemCount() { return list.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDate, txtNote;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDate = itemView.findViewById(R.id.txtItemDate);
            txtNote = itemView.findViewById(R.id.txtItemNote);
        }
    }
}
