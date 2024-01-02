package com.example.customer.common;

import android.animation.ValueAnimator;
import android.widget.TextView;

import com.example.customer.model.AnimationModel;
import com.example.customer.model.DriverGeoModel;
import com.example.customer.model.RiderModel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Common {
    public static final String RIDER_INFO_REFENCE = "Riders";
    public static final String DRIVERS_LOCATION_REFERENCE = "DriversLocation";
    public static final String DRIVER_INFO_REFERENCE = "DriverInfo";
    public static RiderModel currentRider;
//    public static Set<DriverGeoModel> driverFound=new HashSet<DriverGeoModel>();
    public static HashMap<String, Marker> markerList=new HashMap<>();
    public static HashMap<String, AnimationModel> driverLocationSubscribe=new HashMap<String,AnimationModel>();
    public static Map<String,DriverGeoModel> driverFound=new HashMap<String,DriverGeoModel>();


    public static String buildName(String firstName, String lastName) {
        return new StringBuilder(firstName).append(" ").append(lastName).toString();
    }

    //DECODE POLY
    public static List<LatLng> decodePoly(String encoded) {
        List poly = new ArrayList();
        int index=0,len=encoded.length();
        int lat=0,lng=0;
        while(index < len)
        {
            int b,shift=0,result=0;
            do{
                b=encoded.charAt(index++)-63;
                result |= (b & 0x1f) << shift;
                shift+=5;

            }while(b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1):(result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do{
                b = encoded.charAt(index++)-63;
                result |= (b & 0x1f) << shift;
                shift +=5;
            }while(b >= 0x20);
            int dlng = ((result & 1)!=0 ? ~(result >> 1): (result >> 1));
            lng +=dlng;

            LatLng p = new LatLng((((double)lat / 1E5)),
                    (((double)lng/1E5)));
            poly.add(p);
        }
        return poly;
    }
    public static float getBearing(LatLng begin, LatLng end) {
        //You can copy this function by link at description
        double lat = Math.abs(begin.latitude - end.latitude);
        double lng = Math.abs(begin.longitude - end.longitude);

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }

    public static void setWelcomemessage(TextView txtWelcome) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if(hour >=1 && hour <=12)
            txtWelcome.setText(new StringBuilder("Good morning"));
        else if(hour >= 13 && hour <=17)
            txtWelcome.setText(new StringBuilder("Good afternoon"));
        else
            txtWelcome.setText(new StringBuilder("Good evening"));



    }

    public static String formatDuration(String duration) {
        if(duration.contains("mins"))
            return duration.substring(0,duration.length()-1);
        else
            return duration;
    }

    public static String formatAddress(String startAddres) {
        int firstIndexOfComma =startAddres.indexOf(",");
        return startAddres.substring(0,firstIndexOfComma);
    }

    public static ValueAnimator valueAnimate(long duration, ValueAnimator.AnimatorUpdateListener listener) {
    ValueAnimator va=ValueAnimator.ofFloat(0,100);
    va.setDuration(duration);
    va.addUpdateListener(listener);
    va.setRepeatCount(ValueAnimator.INFINITE);
    va.setRepeatMode(ValueAnimator.RESTART);

    va.start();
    return va;
    }
}
