package com.example.fatblur.views;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.fatblur.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PrivacySettingsActivity extends AppCompatActivity {
    private SwitchCompat swLocation;
    private DatabaseReference mDatabase;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_settings);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        myUid = FirebaseAuth.getInstance().getUid();
        swLocation = findViewById(R.id.swShareLocationDetail);

        // 1. Lấy trạng thái hiện tại từ Firebase
        mDatabase.child("user_status").child(myUid).child("isSharingLocation")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isSharing = snapshot.exists() ? snapshot.getValue(Boolean.class) : true;
                        swLocation.setChecked(isSharing);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // 2. Xử lý khi gạt Switch
        swLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mDatabase.child("user_status").child(myUid).child("isSharingLocation").setValue(isChecked);
        });

        // 3. Nút quay lại
        findViewById(R.id.btnBackPrivacy).setOnClickListener(v -> finish());
    }
}
