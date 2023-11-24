package com.example.customer.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.customer.R;
import com.example.customer.callback.IFirebaseDriverInfoListner;
import com.example.customer.callback.IFirebaseFailedListner;
import com.example.customer.common.Common;
import com.example.customer.databinding.FragmentHomeBinding;
import com.example.customer.model.DriverGeoModel;
import com.example.customer.model.DriverInfoModel;
import com.example.customer.model.GeoQueryModel;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.data.DataBufferObserver;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HomeFragment extends Fragment implements OnMapReadyCallback, IFirebaseFailedListner, IFirebaseDriverInfoListner {


    private GoogleMap mMap;

    HomeViewModel homeViewModel;
    SupportMapFragment mapFragment;
    //    //Location
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    //Load Driver
    private double distance = 1.0; //default in km
    private static final double LIMIT_RANGE = 10.0;
    private Location priviousLocation, currentLocation;//use to calculate distance
    private boolean firstTime = true;
    //Listner
    IFirebaseDriverInfoListner iFirebaseDriverInfoListner;
    IFirebaseFailedListner iFirebaseFailedListner;
    private String cityName;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        init();

        return root;
    }

    private void init() {
        iFirebaseDriverInfoListner = this;
        iFirebaseFailedListner = this;

        locationRequest = new LocationRequest();
        locationRequest.setSmallestDisplacement(10f);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                LatLng newPosition = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f));


                //if user has change location calculate and load driver again
                if (firstTime) {
                    priviousLocation = currentLocation = locationResult.getLastLocation();
                    firstTime = false;
                } else {
                    priviousLocation = currentLocation;
                    currentLocation = locationResult.getLastLocation();
                }
                if (priviousLocation.distanceTo(currentLocation) / 1000 <= LIMIT_RANGE) {
                    //not over range
                    loadAvailableDriver();
                } else {
//Do nothing
                }

            }
        };

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), getString(R.string.permission_required), Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        loadAvailableDriver();
    }

    private void loadAvailableDriver() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Check permission", Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }).addOnSuccessListener(location -> {
                    //Load all driver available in city
                    Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                    List<Address> addressesList;
                    try {
                        addressesList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        cityName = addressesList.get(0).getLocality();

//Query
                        DatabaseReference driver_location_ref = FirebaseDatabase.getInstance()
                                .getReference(Common.DRIVERS_LOCATION_REFERENCE)
                                .child(cityName);

                        GeoFire gf = new GeoFire(driver_location_ref);
                        GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(location.getLatitude(),
                                location.getLongitude()), distance);

                        geoQuery.removeAllListeners();
                        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                            @Override
                            public void onKeyEntered(String key, GeoLocation location) {
                                Common.driverFound.add(new DriverGeoModel(key, location));
                            }

                            @Override
                            public void onKeyExited(String key) {

                            }

                            @Override
                            public void onKeyMoved(String key, GeoLocation location) {

                            }

                            @Override
                            public void onGeoQueryReady() {
                                if (distance <= LIMIT_RANGE) {
                                    distance++;
                                    loadAvailableDriver();//for continious search
                                } else {
                                    distance = 1.0; //reset
                                    addDriverMarker();
                                }
                            }

                            @Override
                            public void onGeoQueryError(DatabaseError error) {
                                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                        //Listen to new driver in city and range
                        driver_location_ref.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                                GeoQueryModel geoQueryModel = snapshot.getValue(GeoQueryModel.class);
                                GeoLocation geoLocation = new GeoLocation(geoQueryModel.getL().get(0),
                                        geoQueryModel.getL().get(1));
                                DriverGeoModel driverGeoModel = new DriverGeoModel(snapshot.getKey(),
                                        geoLocation);
                                Location newDriverLocation = new Location("");
                                newDriverLocation.setLatitude(geoLocation.latitude);
                                newDriverLocation.setLongitude(geoLocation.longitude);
                                float newdistance = location.distanceTo(newDriverLocation) / 1000;//in KM
                                if (newdistance <= LIMIT_RANGE) {
                                    findDriverByKey(driverGeoModel);
                                }
                            }

                            @Override
                            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                            }

                            @Override
                            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                            }

                            @Override
                            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                    } catch (IOException e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addDriverMarker() {
        if (Common.driverFound.size() > 0) {
            Observable.fromIterable(Common.driverFound)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(driverGeoModel -> {
                        findDriverByKey(driverGeoModel);
                    }, throwable -> {
                        Toast.makeText(getContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }, () -> {
                    });

        } else {
            Toast.makeText(getContext(), getString(R.string.driver_not_found), Toast.LENGTH_SHORT).show();
        }
    }

    private void findDriverByKey(DriverGeoModel driverGeoModel) {
        FirebaseDatabase.getInstance()
                .getReference(Common.DRIVER_INFO_REFERENCE)
                .child(driverGeoModel.getKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChildren()) {
                            driverGeoModel.setDriverInfoModel(snapshot.getValue(DriverInfoModel.class));
                            iFirebaseDriverInfoListner.onDriverInfoLoadSuccess(driverGeoModel);
                        } else {
                            iFirebaseFailedListner.onFirebaseLoadFailed(getString(R.string.not_found_key) + driverGeoModel.getKey());

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        iFirebaseFailedListner.onFirebaseLoadFailed(error.getMessage());
                    }
                });
    }

    @Override
    public void onDestroyView() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onDestroyView();


    }


    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        //check permission
        Dexter.withContext(getContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {

                        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(getContext(),
                                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            Snackbar.make(getView(), getString(R.string.permission_required), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);

                        mMap.setOnMyLocationButtonClickListener(() -> {
                            fusedLocationProviderClient.getLastLocation()
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnSuccessListener(location -> {
                                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f));
                                    });
                            return true;
                        });

                        //set layout button
                        View locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1"))
                                .getParent())
                                .findViewById(Integer.parseInt("2"));
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        params.setMargins(0, 0, 0, 250);


                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(getContext(), "Permission " + permissionDeniedResponse.getPermissionName() + "" + "was denied!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();
        mMap.getUiSettings().setZoomControlsEnabled(true);
        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.uber_map_style));
            if (!success)
                Toast.makeText(getContext(), "Style Parsing error", Toast.LENGTH_SHORT).show();
        } catch (Resources.NotFoundException e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

        }


    }

    @Override
    public void onFirebaseLoadFailed(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDriverInfoLoadSuccess(DriverGeoModel driverGeoModel) {
        //If already has marker with this key doesnot set again
        if (!Common.markerList.containsKey(driverGeoModel.getKey()))
            Common.markerList.put(driverGeoModel.getKey(),
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(driverGeoModel.getGeoLocation().latitude,
                                    driverGeoModel.getGeoLocation().longitude))
                            .flat(true)
                            .title(Common.buildName(driverGeoModel.getDriverInfoModel().getFirstName(),
                                    driverGeoModel.getDriverInfoModel().getLastName()))
                            .snippet(driverGeoModel.getDriverInfoModel().getPhoneNumber())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))));

        if (!TextUtils.isEmpty(cityName)) {
            DatabaseReference driverLocation = FirebaseDatabase.getInstance()
                    .getReference(Common.DRIVERS_LOCATION_REFERENCE)
                    .child(cityName)
                    .child(driverGeoModel.getKey());
            driverLocation.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.hasChildren()) {
                        if (Common.markerList.get(driverGeoModel.getKey()) != null)
                            Common.markerList.get(driverGeoModel.getKey()).remove();//Remove marker

                        Common.markerList.remove(driverGeoModel.getKey());//Remove maeker info has map
                        driverLocation.removeEventListener(this);//remove event listner
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

    }
}