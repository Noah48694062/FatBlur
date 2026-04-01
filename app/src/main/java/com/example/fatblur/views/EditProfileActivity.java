package com.example.fatblur.views;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.fatblur.R; // Đảm bảo đúng package của bạn
import com.example.fatblur.databinding.ActivityEditProfileBinding; // Tên binding sẽ là ActivityEditProfileBinding
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private ActivityEditProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference mDatabase;
    private Uri selectedImageUri; // Để lưu trữ URI của ảnh mới chọn

    // Launcher để chọn ảnh từ thư viện
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    binding.imageViewEditAvatar.setImageURI(uri); // Hiển thị ảnh mới chọn
                }
            }
    );

    // Khai báo Launcher xin quyền ở cấp class
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    // Nếu người dùng đồng ý, mở trình chọn ảnh
                    pickImageLauncher.launch("image/*");
                } else {
                    // Nếu từ chối, thông báo cho họ biết
                    Toast.makeText(this, "Bạn cần cấp quyền truy cập bộ nhớ để chọn ảnh", Toast.LENGTH_SHORT).show();
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
            // Người dùng chưa đăng nhập, chuyển về màn hình đăng nhập
            startActivity(new Intent(EditProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        loadUserProfileData(); // Tải dữ liệu hồ sơ hiện tại

        // CẬP NHẬT LẠI SỰ KIỆN NHẤN NÚT ĐỔI AVATAR
        binding.btnChangeAvatar.setOnClickListener(v -> {
            String permission;
            // Kiểm tra phiên bản Android
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permission = android.Manifest.permission.READ_MEDIA_IMAGES;
            } else {
                permission = android.Manifest.permission.READ_EXTERNAL_STORAGE;
            }

            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*");
            } else {
                requestPermissionLauncher.launch(permission);
            }
        });

        // Xử lý sự kiện nhấn vào trường ngày sinh để mở DatePickerDialog
        binding.edtBirthdayReg.setOnClickListener(v -> showDatePickerDialog());

        // SỰ KIỆN NÚT HỦY (Thêm đoạn này)
        binding.btnCancelEdit.setOnClickListener(v -> {
            // Chỉ đơn giản là đóng Activity này để quay lại màn hình trước đó
            finish();
        });

        // Xử lý sự kiện nhấn nút "Lưu thay đổi"
        binding.btnSaveProfile.setOnClickListener(v -> {
            saveUserProfile(); // Lưu các thay đổi
        });
    }

    private void loadUserProfileData() {
        // 1. Load từ Firebase Authentication (Tên hiển thị, Email, Ảnh đại diện)
        binding.editTextEmail.setText(currentUser.getEmail()); // Email không cho sửa
        binding.editTextEmail.setEnabled(false); // Đảm bảo email không sửa được

        if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.default_avatar) // Ảnh mặc định trong khi tải
                    .error(R.drawable.default_avatar) // Ảnh hiển thị nếu có lỗi
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Ảnh profile không tồn tại trên Storage, dùng ảnh mặc định.");
                            return false; // Trả về false để Glide tiếp tục thực hiện lệnh .error()
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(binding.imageViewEditAvatar);
        } else {
            binding.imageViewEditAvatar.setImageResource(R.drawable.default_avatar);
        }

        // 2. Load từ Firebase Realtime Database (Số điện thoại, Ngày sinh, Giới tính, Giới thiệu bản thân)
        mDatabase.child("users").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "Realtime Database user data exists. Data: " + dataSnapshot.getValue());

                    String name = dataSnapshot.child("name").getValue(String.class);
                    String phone = dataSnapshot.child("phone").getValue(String.class);
                    String birthday = dataSnapshot.child("birthday").getValue(String.class);
                    String gender = dataSnapshot.child("gender").getValue(String.class);
                    String bio = dataSnapshot.child("bio").getValue(String.class);

                    if (name != null && !name.isEmpty()) {
                        binding.editTextDisplayName.setText(name);
                    } else {
                        // Nếu DB chưa có tên, mới dùng tạm tên từ Auth
                        binding.editTextDisplayName.setText(currentUser.getDisplayName());
                    }

                    Log.d(TAG, "RTDB Phone: " + phone);
                    Log.d(TAG, "RTDB Birthday: " + birthday);
                    Log.d(TAG, "RTDB Gender: " + gender);
                    Log.d(TAG, "RTDB Bio: " + bio);

                    binding.edtPhone.setText(phone != null ? phone : "");
                    binding.edtBirthdayReg.setText(birthday != null ? birthday : "");
                    binding.edtBioReg.setText(bio != null ? bio : "");

                    if ("Nam".equals(gender)) {
                        binding.rbMale.setChecked(true);
                    } else if ("Nữ".equals(gender)) {
                        binding.rbFemale.setChecked(true);
                    } else {
                        // Clear gender selection if unknown or null
                        binding.rgGender.clearCheck();
                    }
                } else {
                    Log.d(TAG, "Realtime Database user data does NOT exist for user: " + currentUser.getUid() + ". Extended profile data is empty.");
                    Toast.makeText(EditProfileActivity.this, "Không tìm thấy dữ liệu hồ sơ mở rộng.", Toast.LENGTH_SHORT).show();
                    // Đặt các trường về trống để người dùng có thể nhập
                    binding.edtPhone.setText("");
                    binding.edtBirthdayReg.setText("");
                    binding.edtBioReg.setText("");
                    binding.rgGender.clearCheck();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Error loading user data from Realtime Database", databaseError.toException());
                Toast.makeText(EditProfileActivity.this, "Lỗi khi tải dữ liệu hồ sơ: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserProfile() {
        String newDisplayName = binding.editTextDisplayName.getText().toString().trim();
        String newPhone = binding.edtPhone.getText().toString().trim();
        String newBirthday = binding.edtBirthdayReg.getText().toString().trim();
        String newBio = binding.edtBioReg.getText().toString().trim();
        String newGender = "";
        if (binding.rgGender.getCheckedRadioButtonId() == binding.rbMale.getId()) {
            newGender = "Nam";
        } else if (binding.rgGender.getCheckedRadioButtonId() == binding.rbFemale.getId()) {
            newGender = "Nữ";
        }

        if (newDisplayName.isEmpty()) {
            binding.editTextDisplayName.setError("Tên hiển thị không được để trống");
            binding.editTextDisplayName.requestFocus();
            return;
        }

        boolean profileAuthUpdated = false;
        boolean profileRTDBUpdated = false; // Flag mới cho RTDB

        // 1. Cập nhật tên hiển thị trong Firebase Authentication
        if (!newDisplayName.equals(currentUser.getDisplayName())) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newDisplayName)
                    .build();

            currentUser.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(EditProfileActivity.this, "Cập nhật tên hiển thị thành công", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "User display name updated.");
                        } else {
                            Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật tên hiển thị: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error updating display name", task.getException());
                        }
                    });
            profileAuthUpdated = true;
        }

        // 2. Tải ảnh avatar mới lên Firebase Storage nếu có
        if (selectedImageUri != null) {
            uploadNewAvatar(selectedImageUri);
            profileAuthUpdated = true; // PhotoUri cũng thuộc Auth Profile
        }

        // 3. Cập nhật các thông tin khác vào Firebase Realtime Database
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("name", newDisplayName);
        userUpdates.put("phone", newPhone);
        userUpdates.put("birthday", newBirthday);
        userUpdates.put("gender", newGender);
        userUpdates.put("bio", newBio);
        userUpdates.put("lastUpdated", System.currentTimeMillis()); // Thêm thời gian cập nhật

        // XỬ LÝ ẢNH: Nếu người dùng có chọn ảnh mới
        if (selectedImageUri != null) {
            String base64Image = encodeImageToBase64(selectedImageUri);
            if (base64Image != null) {
                // Lưu trực tiếp chuỗi ảnh vào trường "avatar"
                userUpdates.put("avatar", base64Image);
            }
        }

        mDatabase.child("users").child(currentUser.getUid())
                .updateChildren(userUpdates) // Sử dụng updateChildren để cập nhật các trường hiện có mà không ghi đè
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfileActivity.this, "Cập nhật thông tin bổ sung thành công", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "User additional data updated successfully in Realtime Database.");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật thông tin bổ sung: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating user additional data in Realtime Database", e);
                });
        profileRTDBUpdated = true;


        // Nếu không có gì được cập nhật, thông báo và kết thúc
        if (!profileAuthUpdated && !profileRTDBUpdated && selectedImageUri == null) {
            Toast.makeText(EditProfileActivity.this, "Không có thay đổi nào để lưu.", Toast.LENGTH_SHORT).show();
            finish();
        } else if (selectedImageUri == null) {
            // Nếu không có ảnh mới, có thể kết thúc activity ngay sau khi cập nhật Auth/RTDB
            // Hoặc chờ đến khi cả hai đều hoàn thành nếu muốn đảm bảo đồng bộ
            Toast.makeText(EditProfileActivity.this, "Lưu hồ sơ hoàn tất.", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Nếu có ảnh mới, việc finish() sẽ được gọi trong updateProfilePhotoUrl()
    }


    private void uploadNewAvatar(Uri imageUri) {
        // Tạo một tham chiếu duy nhất cho ảnh trong Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images/" + currentUser.getUid() + "/" + UUID.randomUUID().toString());

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Lấy URL của ảnh sau khi upload thành công
                    storageRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                Uri downloadUri = task.getResult();
                                updateProfilePhotoUrl(downloadUri); // Cập nhật URL ảnh vào Firebase Auth
                            } else {
                                Log.e(TAG, "Failed to get download URL", task.getException());
                                Toast.makeText(EditProfileActivity.this, "Lỗi khi lấy URL ảnh: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading image to storage", e);
                    Toast.makeText(EditProfileActivity.this, "Lỗi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfilePhotoUrl(Uri photoUri) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(photoUri)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(EditProfileActivity.this, "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "User profile photo URL updated.");
                    } else {
                        Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật ảnh đại diện: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error updating profile photo URL", task.getException());
                    }
                    finish(); // Kết thúc Activity sau khi lưu ảnh
                });
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        if (!binding.edtBirthdayReg.getText().toString().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                c.setTime(sdf.parse(binding.edtBirthdayReg.getText().toString()));
                year = c.get(Calendar.YEAR);
                month = c.get(Calendar.MONTH);
                day = c.get(Calendar.DAY_OF_MONTH);
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing birthday date", e);
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Lưu ngày đã chọn vào EditText
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                    binding.edtBirthdayReg.setText(date);
                }, year, month, day);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Giải phóng binding để tránh memory leak
    }
    private String encodeImageToBase64(Uri imageUri) {
        try {
            // Mở luồng dữ liệu từ Uri ảnh
            java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);

            // Nén ảnh lại một chút để chuỗi Base64 không quá dài (tránh làm chậm Database)
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] bytes = baos.toByteArray();

            // Chuyển mảng byte thành chuỗi Base64
            return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi chuyển đổi ảnh sang Base64: " + e.getMessage());
            return null;
        }
    }
}
