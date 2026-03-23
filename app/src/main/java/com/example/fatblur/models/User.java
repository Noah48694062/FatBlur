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
    }

    // 4. Các Getter và Setter (Tùy chọn, nhưng nên có để bảo mật dữ liệu)
    // Bạn có thể nhấn Alt + Insert trong Android Studio để tự động tạo nhanh
    //getter va setter

}