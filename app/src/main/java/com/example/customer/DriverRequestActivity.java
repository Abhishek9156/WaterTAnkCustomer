package com.example.customer;

import static io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.customer.common.Common;
import com.example.customer.model.DriverGeoModel;
import com.example.customer.model.event.SelectePlaceEvent;
import com.example.customer.remote.IGoogleApi;
import com.example.customer.remote.RetrofitClient;
import com.example.customer.utills.UserUtills;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.ButtCap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.customer.databinding.ActivityDriverRequestBinding;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.material.snackbar.Snackbar;
import com.google.maps.android.ui.IconGenerator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DriverRequestActivity extends FragmentActivity implements OnMapReadyCallback {
    //slowly camera spinning
    private ValueAnimator animator;
    private static final int DESIRED_NUM_OF_SPINS = 5;
    private static final int DESIRED_SECOND_PER_ONC_FULL_360_SPIN = 40;


    //Effect
    private Circle lastUserCircle;
    private long duration = 1000;
    private ValueAnimator lastPulseAnimator;
    CardView confirm_uber_layout;
    CardView finding_your_ride_layout;
    CardView confirm_pickup_layout;
    Button btn_confirm_pickup;
    Button btn_confirm_truck;
    TextView txt_address_pickup;
    TextView txt_origin;
    View fill_maps;
    private GoogleMap mMap;
    private ActivityDriverRequestBinding binding;
    private SelectePlaceEvent selectePlaceEvent;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IGoogleApi iGoogleApi;
    Polyline blackPolyline, greyPolyline;
    PolylineOptions polylineOptions, blackPolylineOptions;
    List<LatLng> polyLineList;
    RelativeLayout main_layout;
    Marker originMarker, destinationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDriverRequestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        inits();
        confirm_uber_layout = findViewById(R.id.confirm_uber_layout);
        confirm_pickup_layout = findViewById(R.id.confirm_pickup_layout);
        btn_confirm_pickup = findViewById(R.id.btn_confirm_pickuo);
        btn_confirm_truck = findViewById(R.id.btn_confirm_truck);
        txt_address_pickup = findViewById(R.id.txt_address_pickup);
        finding_your_ride_layout = findViewById(R.id.finding_your_ride_layout);
        fill_maps = findViewById(R.id.fill_maps);
        main_layout = findViewById(R.id.main_layout);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        btn_confirm_truck.setOnClickListener(view -> {
            confirm_pickup_layout.setVisibility(View.VISIBLE);
            confirm_uber_layout.setVisibility(View.GONE);
            setDataPicker();
        });

        btn_confirm_pickup.setOnClickListener(view -> {
            if (mMap == null) return;
            if (selectePlaceEvent == null) return;

            mMap.clear();
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(selectePlaceEvent.getOrigin())
                    .tilt(45f)
                    .zoom(16f)
                    .build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            //startAnimation
            addmarkwithPulshAnimate();
        });
    }

    private void addmarkwithPulshAnimate() {
        confirm_pickup_layout.setVisibility(View.GONE);
        fill_maps.setVisibility(View.VISIBLE);
        finding_your_ride_layout.setVisibility(View.VISIBLE);

        originMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker())
                .position(selectePlaceEvent.getOrigin()));

        addPulsetingEffect(selectePlaceEvent.getOrigin());
    }

    private void addPulsetingEffect(LatLng origin) {
        if (lastPulseAnimator != null) lastPulseAnimator.cancel();
        if (lastUserCircle != null) lastUserCircle.setCenter(origin);

        lastPulseAnimator = Common.valueAnimate(duration, animation -> {
            if (lastUserCircle != null)
                lastUserCircle.setRadius((Float) animation.getAnimatedValue());
            else {
                lastUserCircle = mMap.addCircle(new CircleOptions()
                        .center(origin)
                        .radius((Float) animation.getAnimatedValue())
                        .strokeColor(Color.WHITE)
                        .fillColor(Color.parseColor("#33333333")));
            }
        });

        startMapCameraSpinningAnimation(origin);

    }

    private void startMapCameraSpinningAnimation(LatLng target) {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0, DESIRED_NUM_OF_SPINS * 360);
        animator.setDuration(DESIRED_SECOND_PER_ONC_FULL_360_SPIN * DESIRED_NUM_OF_SPINS * 1000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setStartDelay(100);
        animator.addUpdateListener(valueAnimator -> {
            Float newBearingValue = (Float) valueAnimator.getAnimatedValue();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(target)
                    .zoom(16f)
                    .tilt(45f)
                    .bearing(newBearingValue)
                    .build()));
        });
        animator.start();

        //after start animation find driver
        findNearbyDriver(target);


    }

    private void findNearbyDriver(LatLng target) {
        if (Common.driverFound.size() > 0) {
            float min_distance = 0;
            DriverGeoModel foundDriver = Common.driverFound.get(Common.driverFound.keySet().iterator().next());
            Location currentRiderLocation = new Location("");
            currentRiderLocation.setLongitude(target.longitude);
            currentRiderLocation.setLatitude(target.latitude);
            for (String key : Common.driverFound.keySet()) {
                Location driverLocation = new Location("");
                driverLocation.setLatitude(Common.driverFound.get(key).getGeoLocation().latitude);
                driverLocation.setLongitude(Common.driverFound.get(key).getGeoLocation().longitude);

                //compare 2 locaiton
                if (min_distance == 0) {
                    min_distance = driverLocation.distanceTo(currentRiderLocation); //first default locatoin
                    foundDriver = Common.driverFound.get(key);
                } else if (driverLocation.distanceTo(currentRiderLocation) < min_distance) {
                    min_distance = driverLocation.distanceTo(currentRiderLocation);
                    foundDriver = Common.driverFound.get(key);

                }
                //Snackbar.make(main_layout,new StringBuilder("Found Driver").append(foundDriver.getDriverInfoModel().getPhoneNumber()),Snackbar.LENGTH_LONG).show();

                UserUtills.sendRequestToDriver(this,main_layout,foundDriver,target);
            }
        } else {
            //Not found
            Snackbar.make(main_layout, getString(R.string.driver_not_found), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (animator != null) animator.end();
        super.onDestroy();
    }

    private void setDataPicker() {
        txt_address_pickup.setText(txt_origin != null ? txt_origin.getText() : "None");
        mMap.clear();
        addPickupMarker();
    }

    private void addPickupMarker() {
        View view = getLayoutInflater().inflate(R.layout.pickup_info_window, null);

        //create icon of marker
        IconGenerator generator = new IconGenerator(this);
        generator.setContentView(view);
        generator.setBackground(new ColorDrawable(Color.TRANSPARENT));
        Bitmap icon = generator.makeIcon();
        destinationMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(selectePlaceEvent.getDestination()));
    }

    private void inits() {
        iGoogleApi = RetrofitClient.getInstance().create(IGoogleApi.class);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        compositeDisposable.clear();
        super.onStop();
        if (EventBus.getDefault().hasSubscriberForEvent(SelectePlaceEvent.class))
            EventBus.getDefault().removeStickyEvent(SelectePlaceEvent.class);
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onSelectePlaceEvent(SelectePlaceEvent event) {
        selectePlaceEvent = event;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

//        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                && ActivityCompat.checkSelfPermission(getApplicationContext(),
//                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_SHORT).show();
//            return;
//        }
//        mMap.setMyLocationEnabled(true);
//        mMap.getUiSettings().setMyLocationButtonEnabled(true);
//
//        mMap.setOnMyLocationButtonClickListener(() -> {
//            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectePlaceEvent.getOrigin(), 18f));
//            return true;
//        });

        drawPath(selectePlaceEvent);

//        //set layout button
//        View locationButton = ((View) findViewById(Integer.parseInt("1"))
//                .getParent())
//                .findViewById(Integer.parseInt("2"));
//        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
//        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
//        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
//        params.setMargins(0, 0, 0, 250);
//
//        mMap.getUiSettings().setZoomControlsEnabled(true);
        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.uber_map_style));
            if (!success)
                Toast.makeText(getApplicationContext(), "Style Parsing error", Toast.LENGTH_SHORT).show();
        } catch (Resources.NotFoundException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

        }

    }

    private void drawPath(SelectePlaceEvent selectePlaceEvent) {
        compositeDisposable.add(iGoogleApi.getDirections("driving",
                        "less_driving",
                        selectePlaceEvent.getOriginString(), selectePlaceEvent.getDestinationString(),
                        getString(R.string.google_api_key))
                .subscribeOn(Schedulers.io())
                .observeOn(mainThread())
                .subscribe(result -> {
                    Log.d("Api Return", result);

                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        JSONArray jsonArray = jsonObject.getJSONArray("routes");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject route = jsonArray.getJSONObject(i);
                            JSONObject poly = route.getJSONObject("overview_polyline");
                            String polyline = poly.getString("points");
                            polyLineList = Common.decodePoly(polyline);
                        }
                        polylineOptions = new PolylineOptions();
                        polylineOptions.color(Color.GRAY);
                        polylineOptions.width(12);
                        polylineOptions.startCap(new SquareCap());
                        polylineOptions.jointType(JointType.ROUND);
                        polylineOptions.addAll(polyLineList);
                        greyPolyline = mMap.addPolyline(polylineOptions);


                        blackPolylineOptions = new PolylineOptions();
                        blackPolylineOptions.color(Color.BLACK);
                        blackPolylineOptions.width(5);
                        blackPolylineOptions.startCap(new SquareCap());
                        blackPolylineOptions.jointType(JointType.ROUND);
                        blackPolylineOptions.addAll(polyLineList);
                        blackPolyline = mMap.addPolyline(blackPolylineOptions);

                        //Animation
                        ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 100);
                        valueAnimator.setDuration(1100);
                        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
                        valueAnimator.setInterpolator(new LinearInterpolator());
                        valueAnimator.addUpdateListener(value -> {
                            List<LatLng> points = greyPolyline.getPoints();
                            int percentValue = (int) value.getAnimatedValue();
                            int size = points.size();
                            int newPoints = (int) (size * (percentValue / 100.0f));
                            List<LatLng> p = points.subList(0, newPoints);
                            blackPolyline.setPoints(p);

                        });
                        valueAnimator.start();

                        LatLngBounds latLngBounds = new LatLngBounds.Builder()
                                .include(selectePlaceEvent.getOrigin())
                                .include(selectePlaceEvent.getDestination())
                                .build();

                        //add car icon
                        JSONObject object = jsonArray.getJSONObject(0);
                        JSONArray legs = object.getJSONArray("legs");
                        JSONObject legObject = legs.getJSONObject(0);
                        JSONObject time = legObject.getJSONObject("duration");
                        String duration = time.getString("text");

                        String start_addres = legObject.getString("start_address");
                        String end_address = legObject.getString("end_address");

                        addOriginMarker(duration, start_addres);
                        addDestinaitonMarker(end_address);

                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 160));
                        mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom - 1));


                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })

        );
    }

    private void addDestinaitonMarker(String endAddress) {
        View view = getLayoutInflater().inflate(R.layout.destination_info_windows, null);
        TextView txt_origin = view.findViewById(R.id.txt_destination);

        txt_origin.setText(Common.formatAddress(endAddress));

        //create icon of marker
        IconGenerator generator = new IconGenerator(this);
        generator.setContentView(view);
        generator.setBackground(new ColorDrawable(Color.TRANSPARENT));
        Bitmap icon = generator.makeIcon();
        destinationMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(selectePlaceEvent.getDestination()));

    }

    private void addOriginMarker(String duration, String startAddres) {
        View view = getLayoutInflater().inflate(R.layout.origin_info_windows, null);
        TextView txt_time = view.findViewById(R.id.txt_time);
        txt_origin = view.findViewById(R.id.txt_origin);

        txt_time.setText(Common.formatDuration(duration));
        txt_origin.setText(Common.formatAddress(startAddres));

        //create icon of marker
        IconGenerator generator = new IconGenerator(this);
        generator.setContentView(view);
        generator.setBackground(new ColorDrawable(Color.TRANSPARENT));
        Bitmap icon = generator.makeIcon();
        originMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(selectePlaceEvent.getOrigin()));
    }
}