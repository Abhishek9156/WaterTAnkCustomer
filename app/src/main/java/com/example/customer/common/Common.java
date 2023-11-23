package com.example.customer.common;



import com.example.customer.model.DriverGeoModel;
import com.example.customer.model.RiderModel;
import com.google.android.gms.maps.model.Marker;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Common {
    public static final String RIDER_INFO_REFENCE = "Riders";
    public static final String DRIVERS_LOCATION_REFERENCE = "DriversLocation";
    public static final String DRIVER_INFO_REFERENCE = "DriverInfo";
    public static RiderModel currentRider;
    public static Set<DriverGeoModel> driverFound=new HashSet<DriverGeoModel>();
    public static HashMap<String, Marker> markerList=new HashMap<>();

    public static String buildName(String firstName, String lastName) {
        return new StringBuilder(firstName).append(" ").append(lastName).toString();
    }
}
