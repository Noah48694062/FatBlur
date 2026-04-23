package com.example.fatblur.views;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.Base64;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

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
import com.google.firebase.database.DatabaseReference;
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

    private TextView txtPartnerName, txtPartnerBattery, txtLastActive, txtDistance, txtMyPrivacyStatus;
    private View viewOnlineStatus;
    private de.hdodenhof.circleimageview.CircleImageView imgPartnerAvatar;
    private Boolean lastPartnerSharingStatus = null;
    private View layoutPartnerPrivacyBanner;
    private boolean isBannerDismissedManually = false;

    private boolean isFirstMyLocation = true;
    private boolean isFirstPartnerLocation = true;
    private double myLat = 0, myLng = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_status, container, false);

        txtPartnerName = v.findViewById(R.id.txtPartnerName);
        txtPartnerBattery = v.findViewById(R.id.txtPartnerBattery);
        txtLastActive = v.findViewById(R.id.txtLastActive);
        viewOnlineStatus = v.findViewById(R.id.viewOnlineStatus);
        imgPartnerAvatar = v.findViewById(R.id.imgPartnerAvatar);
        txtDistance = v.findViewById(R.id.txtDistance);
        txtMyPrivacyStatus = v.findViewById(R.id.txtMyPrivacyStatus);
        layoutPartnerPrivacyBanner = v.findViewById(R.id.layoutPartnerPrivacyBanner);
        View btnCloseBanner = v.findViewById(R.id.btnCloseBanner);

        btnCloseBanner.setOnClickListener(view -> {
            layoutPartnerPrivacyBanner.setVisibility(View.GONE);
            isBannerDismissedManually = true;
        });

        View bottomSheet = v.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        myUid = FirebaseAuth.getInstance().getUid();
        listenToMyPrivacyStatus();
        getPartnerInfo();

        return v;
    }

    private void listenToMyPrivacyStatus() {
        if (myUid == null) return;
        FirebaseDatabase.getInstance().getReference("user_status").child(myUid).child("isSharingLocation")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        boolean isSharing = snapshot.exists() ? snapshot.getValue(Boolean.class) : true;
                        txtMyPrivacyStatus.setText(isSharing ?
                                "Đối tác có thể nhìn thấy vị trí của bạn" :
                                "Bạn hiện đang chặn đối tác truy cập vị trí");
                        txtMyPrivacyStatus.setTextColor(isSharing ? 0xFF8BC34A : 0xFFFF5864);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void listenToPartnerStatus() {
        if (partnerId == null) return;
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        db.child("users").child(partnerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String avatar = snapshot.child("avatar").getValue(String.class);
                    txtPartnerName.setText(name != null ? name : "Đối tác");
                    setPartnerAvatar(avatar);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        db.child("user_status").child(partnerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists()) return;

                Double lat = snapshot.child("latitude").getValue(Double.class);
                Double lng = snapshot.child("longitude").getValue(Double.class);
                Integer battery = snapshot.child("batteryLevel").getValue(Integer.class);
                Boolean isOnline = snapshot.child("isOnline").getValue(Boolean.class);
                Long lastActive = snapshot.child("lastActiveAt").getValue(Long.class);
                Boolean isSharing = snapshot.child("isSharingLocation").getValue(Boolean.class);

                if (isSharing == null) isSharing = true;

                if (lastPartnerSharingStatus != null && lastPartnerSharingStatus != isSharing) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), isSharing ? "Đối tác đã bật lại vị trí" : "Đối tác vừa tắt vị trí", Toast.LENGTH_SHORT).show();
                    }
                }
                lastPartnerSharingStatus = isSharing;
                txtPartnerBattery.setText((battery != null ? battery : 0) + "%");

                if (!isSharing) {
                    if (partnerMarker != null) { partnerMarker.remove(); partnerMarker = null; }
                    if (!isBannerDismissedManually) layoutPartnerPrivacyBanner.setVisibility(View.VISIBLE);
                    txtDistance.setText("Vị trí: Đã ẩn");
                    txtLastActive.setText("Đối tác hiện đang ẩn danh");
                    txtLastActive.setTextColor(0xFFFF5864);
                    viewOnlineStatus.setBackgroundResource(R.drawable.bg_offline_dot);
                } else {
                    layoutPartnerPrivacyBanner.setVisibility(View.GONE);
                    isBannerDismissedManually = false;
                    txtLastActive.setTextColor(0xFF888888);
                    updateOnlineStatusUI(isOnline, lastActive);

                    if (mMap != null && lat != null && lng != null) {
                        LatLng pos = new LatLng(lat, lng);
                        if (partnerMarker == null) {
                            partnerMarker = mMap.addMarker(new MarkerOptions().position(pos).title(txtPartnerName.getText().toString()));
                        } else {
                            partnerMarker.setPosition(pos);
                        }

                        if (myLat != 0 && myLng != 0) {
                            float[] results = new float[1];
                            Location.distanceBetween(myLat, myLng, lat, lng, results);
                            float d = results[0];
                            txtDistance.setText(d < 1000 ? String.format("Cách bạn: %.0f m", d) : String.format("Cách bạn: %.1f km", d / 1000));
                        }

                        if (isFirstPartnerLocation) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
                            isFirstPartnerLocation = false;
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateOnlineStatusUI(Boolean isOnline, Long lastActive) {
        if (isOnline != null && isOnline) {
            txtLastActive.setText("Đang trực tuyến");
            viewOnlineStatus.setBackgroundResource(R.drawable.bg_online_dot);
        } else if (lastActive != null) {
            String timeAgo = (String) DateUtils.getRelativeTimeSpanString(lastActive, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            txtLastActive.setText("Hoạt động " + timeAgo);
            viewOnlineStatus.setBackgroundResource(R.drawable.bg_offline_dot);
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000).build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        myLat = location.getLatitude();
                        myLng = location.getLongitude();
                        updateMyLocationToFirebase(myLat, myLng);
                        if (mMap != null && isFirstMyLocation) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(myLat, myLng), 15f));
                            isFirstMyLocation = false;
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
        updates.put("batteryLevel", getBatteryPercentage());
        FirebaseDatabase.getInstance().getReference("user_status").child(myUid).updateChildren(updates);
    }

    @Override
    public void onStart() { super.onStart(); startLocationUpdates(); }

    @Override
    public void onStop() {
        super.onStop();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        mMap.setOnMarkerClickListener(marker -> {
            if (partnerMarker != null && marker.equals(partnerMarker)) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
            return true;
        });
        mMap.setOnMapClickListener(latLng -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN));
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

    private int getBatteryPercentage() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = requireContext().registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            return (int) ((level / (float) scale) * 100);
        }
        return 0;
    }

    private void setPartnerAvatar(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            imgPartnerAvatar.setImageResource(R.drawable.default_avatar);
            return;
        }
        try {
            byte[] bytes = Base64.decode(base64String, Base64.DEFAULT);
            imgPartnerAvatar.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
        } catch (Exception e) {
            imgPartnerAvatar.setImageResource(R.drawable.default_avatar);
        }
    }
}