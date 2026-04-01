package com.example.fatblur.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fatblur.R;
import com.example.fatblur.models.User;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StatusFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private String myUid, partnerId;
    private Marker partnerMarker;

    // Các view trong Bottom Sheet (Trang chi tiết)
    private TextView txtPartnerName, txtPartnerBattery, txtLastActive;
    private View viewOnlineStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // QUAN TRỌNG: Phải inflate fragment_status (cái có chứa Map và CoordinatorLayout)
        View v = inflater.inflate(R.layout.fragment_status, container, false);

        // 1. Ánh xạ View từ layout_status_detail (đã được include trong fragment_status)
        txtPartnerName = v.findViewById(R.id.txtPartnerName);
        txtPartnerBattery = v.findViewById(R.id.txtPartnerBattery);
        txtLastActive = v.findViewById(R.id.txtLastActive);
        viewOnlineStatus = v.findViewById(R.id.viewOnlineStatus);

        // 2. Thiết lập Bottom Sheet
        View bottomSheet = v.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN); // Mặc định ẩn đi

        // 3. Khởi tạo Bản đồ
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        myUid = FirebaseAuth.getInstance().getUid();
        getPartnerInfo();

        return v;
    }

    private void getPartnerInfo() {
        // Lấy partnerId để biết cần theo dõi ai
        FirebaseDatabase.getInstance().getReference("users").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User me = snapshot.getValue(User.class);
                        if (me != null && me.partnerId != null) {
                            partnerId = me.partnerId;
                            listenToPartnerStatus();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void listenToPartnerStatus() {
        // Lắng nghe dữ liệu realtime (vị trí, pin, online) của đối phương
        FirebaseDatabase.getInstance().getReference("user_status").child(partnerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Giả sử bạn lưu lat/lng trong node user_status
                        String name = snapshot.child("name").getValue(String.class); // Giả sử trong user_status có lưu tên
                        if (name != null) txtPartnerName.setText(name);
                        Double lat = snapshot.child("latitude").getValue(Double.class);
                        Double lng = snapshot.child("longitude").getValue(Double.class);
                        Integer battery = snapshot.child("batteryLevel").getValue(Integer.class);
                        Boolean isOnline = snapshot.child("isOnline").getValue(Boolean.class);

                        // Cập nhật giao diện Bottom Sheet
                        if (battery != null) txtPartnerBattery.setText(battery + "%");
                        if (isOnline != null) {
                            txtLastActive.setText(isOnline ? "Đang trực tuyến" : "Ngoại tuyến");
                            viewOnlineStatus.setBackgroundResource(isOnline ? R.drawable.bg_online_dot : R.drawable.bg_offline_dot);
                        }

                        // Cập nhật vị trí trên bản đồ
                        if (mMap != null && lat != null && lng != null) {
                            LatLng partnerPos = new LatLng(lat, lng);
                            if (partnerMarker == null) {
                                partnerMarker = mMap.addMarker(new MarkerOptions().position(partnerPos).title("Người yêu"));
                            } else {
                                partnerMarker.setPosition(partnerPos);
                            }
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(partnerPos, 15f));
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Khi bấm vào Marker (Avatar) thì hiện Bottom Sheet
        mMap.setOnMarkerClickListener(marker -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            return true;
        });

        // Bấm ra ngoài bản đồ thì ẩn Bottom Sheet
        mMap.setOnMapClickListener(latLng -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        });
    }
}