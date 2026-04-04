package com.example.fatblur.views;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.fatblur.R;
import com.example.fatblur.databinding.ActivityEditProfileBinding;
import com.example.fatblur.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private ActivityEditProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference mDatabase;
    private Uri selectedImageUri;

    // Trình chọn ảnh
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    binding.imageViewEditAvatar.setImageURI(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        loadUserProfileData();

        // Nút chọn ảnh
        binding.btnChangeAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Nút hủy
        binding.btnCancelEdit.setOnClickListener(v -> finish());

        // Nút lưu
        binding.btnSaveProfile.setOnClickListener(v -> saveUserProfile());

        // Chọn ngày sinh
        binding.edtBirthdayReg.setOnClickListener(v -> showDatePickerDialog());
    }

    private void loadUserProfileData() {
        mDatabase.child("users").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String birthday = snapshot.child("birthday").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);
                    String bio = snapshot.child("bio").getValue(String.class);
                    String avatarBase64 = snapshot.child("avatar").getValue(String.class);

                    binding.editTextDisplayName.setText(name);
                    binding.edtPhone.setText(phone);
                    binding.edtBirthdayReg.setText(birthday);
                    binding.edtBioReg.setText(bio);
                    binding.editTextEmail.setText(currentUser.getEmail());

                    if ("Nam".equals(gender)) binding.rbMale.setChecked(true);
                    else if ("Nữ".equals(gender)) binding.rbFemale.setChecked(true);

                    // Load ảnh từ Base64 bằng Glide
                    if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                        byte[] imageBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
                        Glide.with(EditProfileActivity.this).load(imageBytes).into(binding.imageViewEditAvatar);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveUserProfile() {
        String name = binding.editTextDisplayName.getText().toString().trim();
        String phone = binding.edtPhone.getText().toString().trim();
        String birthday = binding.edtBirthdayReg.getText().toString().trim();
        String bio = binding.edtBioReg.getText().toString().trim();
        String gender = binding.rbMale.isChecked() ? "Nam" : (binding.rbFemale.isChecked() ? "Nữ" : "");

        if (name.isEmpty()) {
            binding.editTextDisplayName.setError("Tên không được để trống");
            return;
        }

        Toast.makeText(this, "Đang xử lý dữ liệu...", Toast.LENGTH_SHORT).show();

        String base64Image = null;
        if (selectedImageUri != null) {
            base64Image = compressAndEncodeImage(selectedImageUri);
        }

        updateDatabaseAndAuth(name, phone, birthday, gender, bio, base64Image);
    }

    private String compressAndEncodeImage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(inputStream);

            // Nén kích thước ảnh xuống tối đa 200x200 pixel để tiết kiệm bộ nhớ DB
            Bitmap scaled = Bitmap.createScaledBitmap(original, 200, 200, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Nén chất lượng xuống 20% (vẫn đủ nhìn rõ avatar điện thoại)
            scaled.compress(Bitmap.CompressFormat.JPEG, 20, baos);
            byte[] bytes = baos.toByteArray();

            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi nén ảnh: " + e.getMessage());
            return null;
        }
    }

    private void updateDatabaseAndAuth(String name, String phone, String bday, String gen, String bio, String avatar) {
        // 1. Cập nhật Firebase Auth (Tên hiển thị)
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        currentUser.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            // 2. Cập nhật Realtime Database
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("phone", phone);
            updates.put("birthday", bday);
            updates.put("gender", gen);
            updates.put("bio", bio);
            if (avatar != null) updates.put("avatar", avatar);

            mDatabase.child("users").child(currentUser.getUid()).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, y, m, d) -> binding.edtBirthdayReg.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", d, m + 1, y)),
                year, month, day);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}