package com.example.fatblur.views;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.fatblur.R;
import com.example.fatblur.models.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
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

import java.util.HashMap;
import java.util.Map;

public class StatusFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private String myUid, partnerId;
    private Marker partnerMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private TextView txtPartnerName, txtPartnerBattery, txtLastActive;
    private View viewOnlineStatus;
    private boolean isFirstLocationUpdate = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_status, container, false);

        txtPartnerName = v.findViewById(R.id.txtPartnerName);
        txtPartnerBattery = v.findViewById(R.id.txtPartnerBattery);
        txtLastActive = v.findViewById(R.id.txtLastActive);
        viewOnlineStatus = v.findViewById(R.id.viewOnlineStatus);

        View bottomSheet = v.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Khởi tạo Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        myUid = FirebaseAuth.getInstance().getUid();
        getPartnerInfo();

        return v;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Cách viết LocationRequest mới (để không bị báo deprecated)
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (android.location.Location location : locationResult.getLocations()) {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();

                        // 1. Gửi vị trí lên Firebase (code cũ của bạn)
                        updateMyLocationToFirebase(lat, lng);

                        // 2. Nếu là lần đầu lấy được vị trí, hãy zoom vào
                        if (mMap != null && isFirstLocationUpdate) {
                            LatLng myLatLng = new LatLng(lat, lng);

                            // Zoom mức 15f là mức nhìn rõ đường phố và nhà cửa
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15f));

                            // Đánh dấu là đã zoom xong, lần sau di chuyển sẽ không tự zoom nữa
                            isFirstLocationUpdate = false;
                        }
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void updateMyLocationToFirebase(double lat, double lng) {
        if (myUid == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("latitude", lat);
        updates.put("longitude", lng);
        updates.put("lastActiveAt", System.currentTimeMillis());
        updates.put("isOnline", true);

        FirebaseDatabase.getInstance().getReference("user_status").child(myUid).updateChildren(updates);
    }

    private void getPartnerInfo() {
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
        FirebaseDatabase.getInstance().getReference("user_status").child(partnerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("name").getValue(String.class);
                        if (name != null) txtPartnerName.setText(name);

                        Double lat = snapshot.child("latitude").getValue(Double.class);
                        Double lng = snapshot.child("longitude").getValue(Double.class);
                        Integer battery = snapshot.child("batteryLevel").getValue(Integer.class);
                        Boolean isOnline = snapshot.child("isOnline").getValue(Boolean.class);

                        if (battery != null) txtPartnerBattery.setText(battery + "%");
                        if (isOnline != null) {
                            txtLastActive.setText(isOnline ? "Đang trực tuyến" : "Ngoại tuyến");
                            viewOnlineStatus.setBackgroundResource(isOnline ? R.drawable.bg_online_dot : R.drawable.bg_offline_dot);
                        }

                        if (mMap != null && lat != null && lng != null) {
                            LatLng partnerPos = new LatLng(lat, lng);
                            if (partnerMarker == null) {
                                partnerMarker = mMap.addMarker(new MarkerOptions().position(partnerPos).title("Người yêu"));
                            } else {
                                partnerMarker.setPosition(partnerPos);
                            }
                            // Tự động di chuyển camera đến người yêu lần đầu tiên
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(partnerPos, 15f));
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            startLocationUpdates();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }

        mMap.setOnMarkerClickListener(marker -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            return true;
        });
        mMap.setOnMapClickListener(latLng -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}