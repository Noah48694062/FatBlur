package com.example.fatblur.models;

import java.io.Serializable;

public class User implements Serializable {
    // 1. Thuộc tính (Phải khớp hoàn toàn với tên trong CSDL.txt)
    public String userId;       // ID duy nhất của người dùng 
    public String email;        // Email đăng nhập 
    public String passwordHash; // Mật khẩu đã mã hóa 
    public String userCode;     // Mã chia sẻ để kết nối 
    public String name;         // Tên hiển thị 
    public String avatar;       // Ảnh đại diện (URL) 
    public String birthday;     // Ngày sinh 
    public String gender;       // Giới tính 
    public String bio;          // Mô tả cá nhân 
    public String phone;        // Số điện thoại 
    public String partnerId;    // ID người yêu 
    public boolean isDeleted;   // Đánh dấu đã xóa tài khoản 
    public long createdAt;      // Thời điểm tạo tài khoản (ms) 
    public long updatedAt;      // Lần cập nhật gần nhất (ms)
    public String currentSessionId;
    public boolean isSharingLocation;

    // 2. Constructor mặc định (Bắt buộc phải có để Firebase hoạt động) 
    public User() {
    }

    // 3. Constructor có tham số (Dùng khi đăng ký tài khoản mới)
    public User(String userId, String email, String name, String userCode, long createdAt) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.userCode = userCode;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.isDeleted = false; // Mặc định tài khoản mới chưa bị xóa
        this.isSharingLocation = true;
        this.currentSessionId = "";
    }

    // 4. Các Getter và Setter (Tùy chọn, nhưng nên có để bảo mật dữ liệu)
    // Bạn có thể nhấn Alt + Insert trong Android Studio để tự động tạo nhanh
    //getter va setter

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCurrentSessionId() {
        return currentSessionId;
    }

    public void setCurrentSessionId(String currentSessionId) {
        this.currentSessionId = currentSessionId;
    }

    public boolean isSharingLocation() {
        return isSharingLocation;
    }

    public void setSharingLocation(boolean sharingLocation) {
        isSharingLocation = sharingLocation;
    }
}